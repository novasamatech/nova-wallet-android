package io.novafoundation.nova.core.ethereum.log

sealed class Topic {

    object Any : Topic()

    data class Single(val value: String) : Topic()

    data class AnyOf(val values: List<String>) : Topic() {

        constructor(vararg values: String) : this(values.toList())
    }
}
