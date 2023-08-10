package io.novafoundation.nova.runtime.ethereum.gas

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.tryFindNonNull
import java.math.BigInteger

class CompoundGasPriceProvider(vararg val delegates: GasPriceProvider): GasPriceProvider {

    override suspend fun getGasPrice(): BigInteger {
       return delegates.tryFindNonNull { delegate ->
           runCatching { delegate.getGasPrice() }.getOrNull()
       }.orZero()
    }
}
