package io.novafoundation.nova.feature_swap_core_api.data.paths

import java.math.BigInteger
import io.novafoundation.nova.common.utils.graph.Path
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.PathRoughFeeEstimation
import io.novafoundation.nova.feature_swap_core_api.data.paths.model.QuotedEdge

interface PathFeeEstimator<E> {

    /** The operation that charges a service fee, indexed by its last edge in this path. */
    suspend fun outputCommission(path: Path<E>): IndexedValue<OutputCommission>? = null

    suspend fun roughlyEstimateFee(path: Path<QuotedEdge<E>>): PathRoughFeeEstimation
}

interface OutputCommission {

    fun serviceCommissionToAddOnTop(net: BigInteger): BigInteger

    fun serviceCommissionIncludedIn(gross: BigInteger): BigInteger
}
