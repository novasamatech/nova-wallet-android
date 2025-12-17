package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class XcmTransferDryRunResult(
    val origin: IntermediateSegment,
    val remoteReserve: IntermediateSegment?,
    val destination: FinalSegment,
) {

    class IntermediateSegment(
        val deliveryFee: Balance,
        val trapped: Balance,
    )

    class FinalSegment(
        val depositedAmount: Balance
    )
}
