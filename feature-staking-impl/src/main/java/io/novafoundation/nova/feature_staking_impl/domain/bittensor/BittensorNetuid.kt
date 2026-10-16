package io.novafoundation.nova.feature_staking_impl.domain.bittensor

sealed class BittensorNetuid {

    abstract val value: Int

    data object Root : BittensorNetuid() {

        override val value: Int = 0
    }

    data class Subnet(override val value: Int) : BittensorNetuid() {

        init {
            require(value in 1..UShort.MAX_VALUE.toInt()) { "Dynamic subnet id must fit into a non-zero u16" }
        }
    }

    companion object {

        fun from(value: Int): BittensorNetuid = if (value == Root.value) Root else Subnet(value)
    }
}

fun BittensorNetuid.isRoot(): Boolean = this == BittensorNetuid.Root
