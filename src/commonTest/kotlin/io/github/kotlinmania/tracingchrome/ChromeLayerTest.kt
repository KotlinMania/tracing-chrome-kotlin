package io.github.kotlinmania.tracingchrome

import io.github.kotlinmania.io.Buffer
import io.github.kotlinmania.io.readString
import io.github.kotlinmania.serdejson.fromStr
import io.github.kotlinmania.tracingsubscriber.core.Attributes
import io.github.kotlinmania.tracingsubscriber.core.Event
import io.github.kotlinmania.tracingsubscriber.core.Level
import io.github.kotlinmania.tracingsubscriber.core.Metadata
import io.github.kotlinmania.tracingsubscriber.core.Record
import io.github.kotlinmania.tracingsubscriber.layer.Context
import io.github.kotlinmania.tracingsubscriber.registry.Registry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChromeLayerTest {
    @Test
    fun testThreadedTraceEmission() {
        val buffer = Buffer()
        val (layer, guard) =
            ChromeLayerBuilder
                .new<Registry>()
                .writer(buffer)
                .includeArgs(true)
                .includeLocations(true)
                .traceStyle(TraceStyle.Threaded)
                .build()

        val registry = Registry()
        val context = Context(registry)

        val meta =
            Metadata(
                name = "mySpan",
                target = "myTarget",
                level = Level.INFO,
                isSpan = true,
                file = "Test.kt",
                line = 42,
            )
        val spanId = registry.newSpan(Attributes(metadata = meta, values = mapOf("initial" to "val")))
        layer.onNewSpan(Attributes(metadata = meta, values = mapOf("initial" to "val")), spanId, context)

        layer.onEnter(spanId, context)
        layer.onRecord(spanId, Record(mapOf("updated" to 123)), context)

        val eventMeta =
            Metadata(
                name = "myEvent",
                target = "myEventTarget",
                level = Level.DEBUG,
                isEvent = true,
                file = "Test.kt",
                line = 50,
            )
        layer.onEvent(Event(metadata = eventMeta, fields = mapOf("message" to "event happened")), context)

        layer.onExit(spanId, context)
        layer.onClose(spanId, context)

        guard.close()

        val output = buffer.readString()
        assertTrue(output.startsWith("[\n"), "Output should start with array open: $output")
        assertTrue(output.endsWith("\n]"), "Output should end with array close: $output")

        val jsonValue = fromStr(output).getOrThrow()
        assertTrue(jsonValue.isArray(), "Output should parse as JSON array")
        val array = jsonValue.asArray()!!
        assertTrue(array.size >= 3, "Array should contain at least NewThread, Begin, Event, End entries")

        val phases = array.mapNotNull { it.get("ph")?.asStr() }
        assertTrue(phases.contains("M"), "Should have thread metadata phase")
        assertTrue(phases.contains("B"), "Should have begin phase in threaded mode")
        assertTrue(phases.contains("i"), "Should have instant event phase")
        assertTrue(phases.contains("E"), "Should have end phase in threaded mode")
    }

    @Test
    fun testAsyncTraceEmission() {
        val buffer = Buffer()
        val (layer, guard) =
            ChromeLayerBuilder
                .new<Registry>()
                .writer(buffer)
                .includeArgs(true)
                .includeLocations(false)
                .traceStyle(TraceStyle.Async)
                .build()

        val registry = Registry()
        val context = Context(registry)

        val meta =
            Metadata(
                name = "asyncSpan",
                target = "asyncTarget",
                level = Level.INFO,
                isSpan = true,
            )
        val spanId = registry.newSpan(Attributes(metadata = meta))
        layer.onNewSpan(Attributes(metadata = meta), spanId, context)
        layer.onClose(spanId, context)

        guard.close()

        val output = buffer.readString()
        val jsonValue = fromStr(output).getOrThrow()
        val array = jsonValue.asArray()!!
        val phases = array.mapNotNull { it.get("ph")?.asStr() }
        assertTrue(phases.contains("b"), "Should have async begin phase")
        assertTrue(phases.contains("e"), "Should have async end phase")
    }

    @Test
    fun testCustomNameAndCategoryFn() {
        val buffer = Buffer()
        val (layer, guard) =
            ChromeLayerBuilder
                .new<Registry>()
                .writer(buffer)
                .nameFn { "custom_name" }
                .categoryFn { "custom_cat" }
                .build()

        val registry = Registry()
        val context = Context(registry)

        val meta =
            Metadata(
                name = "defaultName",
                target = "defaultTarget",
                level = Level.INFO,
                isSpan = true,
            )
        val spanId = registry.newSpan(Attributes(metadata = meta))
        layer.onNewSpan(Attributes(metadata = meta), spanId, context)
        layer.onEnter(spanId, context)
        layer.onExit(spanId, context)

        guard.close()

        val output = buffer.readString()
        val jsonValue = fromStr(output).getOrThrow()
        val array = jsonValue.asArray()!!
        val beginEntry = array.firstOrNull { it.get("ph")?.asStr() == "B" }
        assertNotNull(beginEntry)
        assertEquals("custom_name", beginEntry.get("name")?.asStr())
        assertEquals("custom_cat", beginEntry.get("cat")?.asStr())
    }

    @Test
    fun testStartNewAndFlush() {
        val firstBuffer = Buffer()
        val secondBuffer = Buffer()
        val (layer, guard) =
            ChromeLayerBuilder
                .new<Registry>()
                .writer(firstBuffer)
                .build()

        val registry = Registry()
        val context = Context(registry)

        val meta =
            Metadata(
                name = "spanOne",
                target = "targetOne",
                level = Level.INFO,
                isSpan = true,
            )
        val spanId = registry.newSpan(Attributes(metadata = meta))
        layer.onNewSpan(Attributes(metadata = meta), spanId, context)
        layer.onEnter(spanId, context)
        layer.onExit(spanId, context)

        guard.flush()
        guard.startNew(secondBuffer)

        val metaTwo =
            Metadata(
                name = "spanTwo",
                target = "targetTwo",
                level = Level.INFO,
                isSpan = true,
            )
        val spanIdTwo = registry.newSpan(Attributes(metadata = metaTwo))
        layer.onNewSpan(Attributes(metadata = metaTwo), spanIdTwo, context)
        layer.onEnter(spanIdTwo, context)
        layer.onExit(spanIdTwo, context)

        guard.close()

        val firstOutput = firstBuffer.readString()
        val secondOutput = secondBuffer.readString()

        val firstJson = fromStr(firstOutput).getOrThrow()
        val secondJson = fromStr(secondOutput).getOrThrow()

        assertTrue(firstJson.isArray())
        assertTrue(secondJson.isArray())
    }
}
