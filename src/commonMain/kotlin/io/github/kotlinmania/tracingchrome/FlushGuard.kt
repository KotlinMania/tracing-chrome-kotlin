package io.github.kotlinmania.tracingchrome

import io.github.kotlinmania.io.RawSink

/**
 * This guard will signal the thread writing the trace file to stop and join it when dropped.
 */
class FlushGuard internal constructor(
    private val writer: TraceWriter,
) : AutoCloseable {
    /**
     * Signals the trace writing thread to flush to disk.
     */
    fun flush() {
        writer.flush()
    }

    /**
     * Finishes the current trace and starts a new one.
     *
     * If a writer implementation is supplied, the new trace is written to it.
     * Otherwise, the new trace goes to `./trace-{unix epoch in micros}.json`.
     */
    fun startNew(writer: RawSink? = null) {
        this.writer.startNew(writer)
    }

    internal fun drop() {
        writer.close()
    }

    override fun close() {
        drop()
    }
}
