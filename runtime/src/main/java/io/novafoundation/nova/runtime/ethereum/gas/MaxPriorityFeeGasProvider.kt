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

class MedianPriorityFeeFeeGasProvider(private val api: Web3j) : GasPriceProvider {

    companion object {
        private const val REWARD_PERCENTILE = 75.0
        private const val NUMBER_OF_BLOCKS = 5
    }

    override suspend fun getGasPrice(): BigInteger {
        val history = api.ethFeeHistory(NUMBER_OF_BLOCKS, DefaultBlockParameterName.LATEST, listOf(REWARD_PERCENTILE)).sendSuspend()

        val priorityFee = history.feeHistory.reward.maxOf { it.first() }
        val baseFee = history.feeHistory.baseFeePerGas.max()

        return baseFee + priorityFee
    }
}
