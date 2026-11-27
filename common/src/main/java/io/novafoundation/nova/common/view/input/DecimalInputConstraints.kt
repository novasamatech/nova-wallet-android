package io.novafoundation.nova.common.view.input

sealed class DecimalInputConstraints {

    object Unlimited : DecimalInputConstraints()

    data class FractionOnly(val maxFractionDigits: Int) : DecimalInputConstraints() {

        init {
            require(maxFractionDigits >= 0)
        }
    }

    data class Bounded(val maxFractionDigits: Int, val maxIntegerDigits: Int) : DecimalInputConstraints() {

        init {
            require(maxFractionDigits >= 0)
            require(maxIntegerDigits > 0)
        }
    }
}
