package io.novafoundation.nova.common.utils

fun Iterable<String>.asQueryParam() = joinToString(separator = ",")
