package io.novafoundation.nova.runtime.ethereum.gas

import io.novafoundation.nova.runtime.ethereum.sendSuspend
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import java.math.BigInteger

class MaxPriorityFeeGasProvider(private val api: Web3j) : GasPriceProvider {

    override suspend fun getGasPrice(): BigInteger {
        val baseFeePerGas = api.getLatestBaseFeePerGas()
        val maxPriorityFee = api.ethMaxPriorityFeePerGas().sendSuspend().maxPriorityFeePerGas

        return baseFeePerGas + maxPriorityFee
    }

    private suspend fun Web3j.getLatestBaseFeePerGas(): BigInteger {
        val block = ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).sendSuspend()

        return block.block.baseFeePerGas
    }
}
