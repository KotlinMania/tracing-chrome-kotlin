package io.github.kotlinmania.tracingchrome

import io.github.kotlinmania.serdejson.JsonMap
import io.github.kotlinmania.tracingsubscriber.core.Event
import io.github.kotlinmania.tracingsubscriber.registry.LookupSpan
import io.github.kotlinmania.tracingsubscriber.registry.SpanRef

typealias NameFn = (EventOrSpan) -> String
typealias Object = JsonMap

/**
 * Represents either an Event or SpanRef.
 */
sealed class EventOrSpan {
    class Event(
        val event: io.github.kotlinmania.tracingsubscriber.core.Event,
    ) : EventOrSpan()

    class Span(
        val span: SpanRef<LookupSpan>,
    ) : EventOrSpan()
}
