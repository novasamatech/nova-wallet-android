package jp.co.soramitsu.common.utils

fun Iterable<String>.asQueryParam() = joinToString(separator = ",")
