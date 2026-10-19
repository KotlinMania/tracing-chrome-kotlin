package io.github.kotlinmania.tracingchrome

import io.github.kotlinmania.io.RawSink

internal sealed class Message {
    class Enter(
        val ts: Double,
        val callsite: Callsite,
        val rootId: Long?,
    ) : Message()

    class Event(
        val ts: Double,
        val callsite: Callsite,
    ) : Message()

    class Exit(
        val ts: Double,
        val callsite: Callsite,
        val rootId: Long?,
    ) : Message()

    class NewThread(
        val tid: Long,
        val name: String,
    ) : Message()

    object Flush : Message()

    object Drop : Message()

    class StartNew(
        val writer: RawSink?,
    ) : Message()
}
