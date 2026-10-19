package io.github.kotlinmania.tracingchrome

/**
 * Decides how traces will be recorded.
 */
enum class TraceStyle {
    /**
     * Traces will be recorded as a group of threads.
     * In this style, spans should be entered and exited on the same thread.
     */
    Threaded,

    /**
     * Traces will recorded as a group of asynchronous operations.
     */
    Async,
}
