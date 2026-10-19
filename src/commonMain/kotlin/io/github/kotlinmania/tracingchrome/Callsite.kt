package io.github.kotlinmania.tracingchrome

import io.github.kotlinmania.serdejson.JsonMap

internal class Callsite(
    val tid: Long,
    val name: String,
    val target: String,
    val file: String?,
    val line: Int?,
    val args: JsonMap?,
)

internal class ArgsWrapper(
    val args: JsonMap,
)
