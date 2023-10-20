package io.novafoundation.nova.feature_swap_impl.domain.slippage

import io.novafoundation.nova.common.utils.Percent

interface SlippageRepository {

    fun getDefaultSlippage(): Percent

    fun getSlippageTips(): List<Percent>

    fun minSlippage(): Percent

    fun maxSlippage(): Percent

    fun smallSlippage(): Percent

    abstract fun bigSlippage(): Percent
}

class RealSlippageRepository : SlippageRepository {

    override fun getDefaultSlippage(): Percent {
        return Percent(0.5)
    }

    override fun getSlippageTips(): List<Percent> {
        return listOf(Percent(0.1), Percent(1.0), Percent(3.0))
    }

    override fun minSlippage(): Percent {
        return Percent(0.1)
    }

    override fun maxSlippage(): Percent {
        return Percent(50.0)
    }

    override fun smallSlippage(): Percent {
        return Percent(0.5)
    }

    override fun bigSlippage(): Percent {
        return Percent(2.5)
    }
}
