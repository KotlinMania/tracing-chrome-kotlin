package io.github.kotlinmania.tracingchrome

import io.github.kotlinmania.io.RawSink
import io.github.kotlinmania.io.files.Path
import io.github.kotlinmania.io.files.SystemFileSystem
import io.github.kotlinmania.tracingsubscriber.core.Subscriber
import io.github.kotlinmania.tracingsubscriber.registry.LookupSpan

/**
 * A builder for ChromeLayer.
 */
class ChromeLayerBuilder<S> where S : Subscriber, S : LookupSpan {
    internal var outWriter: RawSink? = null
    internal var nameFn: NameFn? = null
    internal var catFn: NameFn? = null
    internal var includeArgs: Boolean = false
    internal var includeLocations: Boolean = true
    internal var traceStyle: TraceStyle = TraceStyle.Threaded

    companion object {
        fun <S> new(): ChromeLayerBuilder<S> where S : Subscriber, S : LookupSpan = ChromeLayerBuilder()
    }

    /**
     * Set the file to which to output the trace.
     *
     * Defaults to `./trace-{unix epoch in micros}.json`.
     */
    fun file(file: Path): ChromeLayerBuilder<S> = writer(SystemFileSystem.sink(file))

    /**
     * Set the file to which to output the trace by string path.
     */
    fun file(filePath: String): ChromeLayerBuilder<S> = file(Path(filePath))

    /**
     * Supply an arbitrary writer to which to write trace contents.
     */
    fun writer(writer: RawSink): ChromeLayerBuilder<S> {
        this.outWriter = writer
        return this
    }

    /**
     * Include arguments in each trace entry.
     *
     * Defaults to `false`.
     */
    fun includeArgs(include: Boolean): ChromeLayerBuilder<S> {
        this.includeArgs = include
        return this
    }

    /**
     * Include file+line with each trace entry.
     *
     * Defaults to `true`.
     */
    fun includeLocations(include: Boolean): ChromeLayerBuilder<S> {
        this.includeLocations = include
        return this
    }

    /**
     * Sets the style used when recording trace events.
     */
    fun traceStyle(style: TraceStyle): ChromeLayerBuilder<S> {
        this.traceStyle = style
        return this
    }

    /**
     * Allows supplying a function that derives a name from an Event or Span.
     */
    fun nameFn(nameFn: NameFn): ChromeLayerBuilder<S> {
        this.nameFn = nameFn
        return this
    }

    /**
     * Allows supplying a function that derives a category from an Event or Span.
     */
    fun categoryFn(catFn: NameFn): ChromeLayerBuilder<S> {
        this.catFn = catFn
        return this
    }

    /**
     * Creates a ChromeLayer and associated FlushGuard.
     */
    fun build(): Pair<ChromeLayer<S>, FlushGuard> = ChromeLayer.new(this)
}
