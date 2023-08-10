package io.novafoundation.nova.runtime.ethereum.gas

import io.novafoundation.nova.runtime.ethereum.sendSuspend
import org.web3j.protocol.Web3j
import java.math.BigInteger

class LegacyGasPriceProvider(private val api: Web3j) : GasPriceProvider {

    override suspend fun getGasPrice(): BigInteger {
        return api.ethGasPrice().sendSuspend().gasPrice
    }
}
