package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import java.math.BigInteger

@JvmInline
value class Tradeability(val value: BigInteger) {

    companion object {
        // / Asset is allowed to be sold into omnipool
        val SELL = 0b0000_0001.toBigInteger()

        // / Asset is allowed to be bought into omnipool
        val BUY = 0b0000_0010.toBigInteger()
    }

    fun canBuy(): Boolean = flagEnabled(BUY)

    fun canSell(): Boolean = flagEnabled(SELL)

    private fun flagEnabled(flag: BigInteger) = value and flag == flag
}

fun bindTradeability(value: Any?): Tradeability {
    val asStruct = value.castToStruct()

    return Tradeability(bindNumber(asStruct["bits"]))
}
