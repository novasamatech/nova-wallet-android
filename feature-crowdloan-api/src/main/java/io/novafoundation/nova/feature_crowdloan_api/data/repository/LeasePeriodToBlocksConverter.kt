package io.novafoundation.nova.feature_crowdloan_api.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import java.math.BigInteger

class LeasePeriodToBlocksConverter(
    private val blocksPerLease: BigInteger,
    private val blocksOffset: BigInteger
) {

    fun startBlockFor(leasePeriod: BigInteger): BlockNumber {
        return blocksPerLease * leasePeriod + blocksOffset
    }

    fun leaseIndexFromBlock(blockNumber: BlockNumber): BigInteger {
        return (blockNumber - blocksOffset) / blocksPerLease
    }
}
