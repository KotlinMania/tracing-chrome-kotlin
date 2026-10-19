package io.github.kotlinmania.tracingchrome

import io.github.kotlinmania.io.RawSink
import io.github.kotlinmania.io.Sink
import io.github.kotlinmania.io.buffered
import io.github.kotlinmania.io.files.Path
import io.github.kotlinmania.io.files.SystemFileSystem
import io.github.kotlinmania.io.writeString
import io.github.kotlinmania.serdejson.JsonMap
import io.github.kotlinmania.serdejson.Value
import io.github.kotlinmania.serdejson.json
import io.github.kotlinmania.serdejson.obj
import io.github.kotlinmania.serdejson.valueToStr
import io.github.kotlinmania.threadlocal.ThreadLocal
import io.github.kotlinmania.tracingsubscriber.core.Attributes
import io.github.kotlinmania.tracingsubscriber.core.Event
import io.github.kotlinmania.tracingsubscriber.core.Record
import io.github.kotlinmania.tracingsubscriber.core.SpanId
import io.github.kotlinmania.tracingsubscriber.core.Subscriber
import io.github.kotlinmania.tracingsubscriber.layer.Context
import io.github.kotlinmania.tracingsubscriber.layer.Layer
import io.github.kotlinmania.tracingsubscriber.registry.LookupSpan
import io.github.kotlinmania.tracingsubscriber.registry.SpanRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.time.Clock
import kotlin.time.TimeSource

@OptIn(kotlin.time.ExperimentalTime::class)
internal fun createDefaultWriter(): RawSink {
    val nowMicros = Clock.System.now().toEpochMilliseconds() * 1000L
    val path = Path("./trace-$nowMicros.json")
    return SystemFileSystem.sink(path)
}

internal class JsonVisitor(
    val objectMap: JsonMap,
) {
    fun recordDebug(field: String, value: Any?) {
        objectMap.insert(field, Value.Str(value?.toString() ?: "null"))
    }
}

internal class TraceWriter(
    initialWriter: RawSink?,
) : SynchronizedObject() {
    private var sink: Sink = (initialWriter ?: createDefaultWriter()).buffered()
    private var hasStarted: Boolean = false
    private val threadNames = mutableListOf<Pair<Long, String>>()
    private var isClosed: Boolean = false

    init {
        sink.writeString("[\n")
    }

    fun send(msg: Message) =
        synchronized(this) {
            if (isClosed) return@synchronized
            when (msg) {
                is Message.Flush -> {
                    sink.flush()
                }

                is Message.Drop -> {
                    closeInternal()
                }

                is Message.StartNew -> {
                    sink.writeString("\n]")
                    sink.flush()
                    sink.close()

                    val nextWriter = msg.writer ?: createDefaultWriter()
                    sink = nextWriter.buffered()
                    sink.writeString("[\n")
                    hasStarted = false

                    for ((tid, name) in threadNames) {
                        val entry =
                            json {
                                "ph" to "M"
                                "pid" to 1
                                "name" to "thread_name"
                                "tid" to tid
                                "args" to
                                    obj {
                                        "name" to name
                                    }
                            }
                        if (hasStarted) {
                            sink.writeString(",\n")
                        }
                        sink.writeString(valueToStr(entry))
                        hasStarted = true
                    }
                }

                is Message.Enter -> {
                    val ph = if (msg.rootId != null) "b" else "B"
                    writeTraceEvent(ph, msg.ts, msg.callsite, msg.rootId)
                }

                is Message.Event -> {
                    writeTraceEvent("i", msg.ts, msg.callsite, null)
                }

                is Message.Exit -> {
                    val ph = if (msg.rootId != null) "e" else "E"
                    writeTraceEvent(ph, msg.ts, msg.callsite, msg.rootId)
                }

                is Message.NewThread -> {
                    threadNames.add(Pair(msg.tid, msg.name))
                    val entry =
                        json {
                            "ph" to "M"
                            "pid" to 1
                            "name" to "thread_name"
                            "tid" to msg.tid
                            "args" to
                                obj {
                                    "name" to msg.name
                                }
                        }
                    if (hasStarted) {
                        sink.writeString(",\n")
                    }
                    sink.writeString(valueToStr(entry))
                    hasStarted = true
                }
            }
        }

    private fun writeTraceEvent(ph: String, ts: Double, callsite: Callsite, id: Long?) {
        val entry =
            json {
                "ph" to ph
                "pid" to 1
                "ts" to ts
                "name" to callsite.name
                "cat" to callsite.target
                "tid" to callsite.tid
                if (id != null) {
                    "id" to id
                }
                if (ph == "i") {
                    "s" to "t"
                }
                if (callsite.file != null && callsite.line != null) {
                    ".file" to callsite.file
                    ".line" to callsite.line
                }
                if (callsite.args != null && !callsite.args.isEmpty()) {
                    "args" to Value.Object(callsite.args)
                }
            }
        if (hasStarted) {
            sink.writeString(",\n")
        }
        sink.writeString(valueToStr(entry))
        hasStarted = true
    }

    fun flush() =
        synchronized(this) {
            if (!isClosed) {
                sink.flush()
            }
        }

    fun startNew(newWriter: RawSink?) {
        send(Message.StartNew(newWriter))
    }

    private fun closeInternal() {
        if (!isClosed) {
            sink.writeString("\n]")
            sink.flush()
            sink.close()
            isClosed = true
        }
    }

    fun close() =
        synchronized(this) {
            closeInternal()
        }
}

/**
 * A Layer that writes a Chrome trace file.
 */
class ChromeLayer<S> internal constructor(
    private val writer: TraceWriter,
    private val includeArgs: Boolean,
    private val includeLocations: Boolean,
    private val traceStyle: TraceStyle,
    private val nameFn: NameFn?,
    private val catFn: NameFn?,
) : Layer<S> where S : Subscriber, S : LookupSpan {
    private val timeMark = TimeSource.Monotonic.markNow()
    private val maxTid = atomic(0L)
    private val threadTid = ThreadLocal<Long>()

    companion object {
        internal fun <S> new(builder: ChromeLayerBuilder<S>): Pair<ChromeLayer<S>, FlushGuard>
            where S : Subscriber, S : LookupSpan {
            val writer = TraceWriter(builder.outWriter)
            val guard = FlushGuard(writer)
            val layer =
                ChromeLayer<S>(
                    writer = writer,
                    includeArgs = builder.includeArgs,
                    includeLocations = builder.includeLocations,
                    traceStyle = builder.traceStyle,
                    nameFn = builder.nameFn,
                    catFn = builder.catFn,
                )
            return Pair(layer, guard)
        }

        private fun getRootId(span: SpanRef<LookupSpan>): Long =
            span
                .scope()
                .fromRoot()
                .firstOrNull()
                ?.id
                ?.value ?: span.id.value
    }

    private fun getTid(): Pair<Long, Boolean> {
        val cached = threadTid.get()
        return if (cached != null) {
            Pair(cached, false)
        } else {
            val allocated = maxTid.getAndIncrement()
            val tid = threadTid.getOr { allocated }
            Pair(tid, true)
        }
    }

    private fun getCallsite(data: EventOrSpan): Callsite {
        val (tid, newThread) = getTid()
        val customName = nameFn?.invoke(data)
        val customTarget = catFn?.invoke(data)
        val meta =
            when (data) {
                is EventOrSpan.Event -> data.event.metadata
                is EventOrSpan.Span -> data.span.metadata
            }
        val args: JsonMap? =
            when (data) {
                is EventOrSpan.Event -> {
                    if (includeArgs && data.event.fields.isNotEmpty()) {
                        val map = JsonMap()
                        val visitor = JsonVisitor(map)
                        for ((k, v) in data.event.fields) {
                            visitor.recordDebug(k, v)
                        }
                        map
                    } else {
                        null
                    }
                }

                is EventOrSpan.Span -> {
                    data.span.extensions
                        .get<ArgsWrapper>()
                        ?.args
                }
            }
        val name = customName ?: meta.name
        val target = customTarget ?: meta.target
        val (file, line) =
            if (includeLocations) {
                Pair(meta.file, meta.line)
            } else {
                Pair(null, null)
            }

        if (newThread) {
            val threadName = tid.toString()
            sendMessage(Message.NewThread(tid, threadName))
        }

        return Callsite(
            tid = tid,
            name = name,
            target = target,
            file = file,
            line = line,
            args = args,
        )
    }

    private fun enterSpan(span: SpanRef<LookupSpan>, ts: Double) {
        val callsite = getCallsite(EventOrSpan.Span(span))
        val rootId =
            when (traceStyle) {
                TraceStyle.Async -> getRootId(span)
                else -> null
            }
        sendMessage(Message.Enter(ts, callsite, rootId))
    }

    private fun exitSpan(span: SpanRef<LookupSpan>, ts: Double) {
        val callsite = getCallsite(EventOrSpan.Span(span))
        val rootId =
            when (traceStyle) {
                TraceStyle.Async -> getRootId(span)
                else -> null
            }
        sendMessage(Message.Exit(ts, callsite, rootId))
    }

    private fun getTs(): Double =
        timeMark.elapsedNow().inWholeNanoseconds.toDouble() / 1000.0

    private fun sendMessage(message: Message) {
        writer.send(message)
    }

    override fun onEnter(id: SpanId, context: Context<S>) {
        if (traceStyle == TraceStyle.Async) {
            return
        }
        val ts = getTs()
        val span = context.lookupSpan(id) ?: return
        enterSpan(span, ts)
    }

    override fun onRecord(id: SpanId, values: Record, context: Context<S>) {
        if (includeArgs) {
            val span = context.lookupSpan(id) ?: return
            val wrapper = span.extensions.get<ArgsWrapper>()
            if (wrapper != null) {
                val visitor = JsonVisitor(wrapper.args)
                for ((k, v) in values.values) {
                    visitor.recordDebug(k, v)
                }
            }
        }
    }

    override fun onEvent(event: Event, context: Context<S>) {
        if (!context.enabled(event.metadata)) return
        val ts = getTs()
        val callsite = getCallsite(EventOrSpan.Event(event))
        sendMessage(Message.Event(ts, callsite))
    }

    override fun onExit(id: SpanId, context: Context<S>) {
        if (traceStyle == TraceStyle.Async) {
            return
        }
        val ts = getTs()
        val span = context.lookupSpan(id) ?: return
        exitSpan(span, ts)
    }

    override fun onNewSpan(attributes: Attributes, id: SpanId, context: Context<S>) {
        if (includeArgs) {
            val map = JsonMap()
            val visitor = JsonVisitor(map)
            for ((k, v) in attributes.values) {
                visitor.recordDebug(k, v)
            }
            context.lookupSpan(id)?.extensions?.insert(ArgsWrapper(map))
        }
        if (traceStyle == TraceStyle.Threaded) {
            return
        }
        val ts = getTs()
        val span = context.lookupSpan(id) ?: return
        enterSpan(span, ts)
    }

    override fun onClose(id: SpanId, context: Context<S>) {
        if (traceStyle == TraceStyle.Threaded) {
            return
        }
        val ts = getTs()
        val span = context.lookupSpan(id) ?: return
        exitSpan(span, ts)
    }
}
