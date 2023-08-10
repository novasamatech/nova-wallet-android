package io.novafoundation.nova.runtime.ethereum.gas

import java.math.BigInteger

interface GasPriceProvider {

    suspend fun getGasPrice(): BigInteger
}

