package io.novafoundation.nova.feature_swap_core_api.data.paths.model

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import java.math.BigInteger

class PathRoughFeeEstimation(val inAssetOut: BalanceOf, val inAssetIn: BalanceOf) {

    companion object {

        fun zero(): PathRoughFeeEstimation {
            return PathRoughFeeEstimation(BigInteger.ZERO, BigInteger.ZERO)
        }
    }
}
