package io.novafoundation.nova.runtime.ethereum.log

sealed class Topic {

    object Any : Topic()

    class Single(val value: String) : Topic()

    class AnyOf(val values: List<String>) : Topic() {

        constructor(vararg values: String) : this(values.toList())
    }
}
