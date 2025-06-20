package io.novafoundation.nova.runtime.ethereum.gas

import android.util.Log
import io.novafoundation.nova.common.utils.orZero
import io.novasama.substrate_sdk_android.extensions.tryFindNonNull
import java.math.BigInteger

class CompoundGasPriceProvider(private val delegates: List<GasPriceProvider>) : GasPriceProvider {

    override suspend fun getGasPrice(): BigInteger {
        return delegates.tryFindNonNull { delegate ->
            runCatching { delegate.getGasPrice() }
                .onFailure { Log.w("GasPriceProvider","Failed to fetch price using ${delegate::class.simpleName}", it) }
                .getOrNull()
        }.orZero()
    }
}
