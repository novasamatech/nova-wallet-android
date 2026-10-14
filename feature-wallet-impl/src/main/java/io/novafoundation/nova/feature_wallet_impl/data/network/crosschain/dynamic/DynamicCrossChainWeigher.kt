package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic

import android.util.Log
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.replaceAmount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.XcmTransferDryRunOrigin
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun.XcmTransferDryRunResult
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun.XcmTransferDryRunner
import javax.inject.Inject

private const val MINIMUM_SEND_AMOUNT = 100

// Some chains cap a single outgoing transfer of an asset (e.g. Hydration's per-asset xcm rate limit),
// so a dry run with MINIMUM_SEND_AMOUNT can fail even though the direction works. The fee is then re-estimated with this amount.
private const val FALLBACK_SEND_AMOUNT = 1

@FeatureScope
class DynamicCrossChainWeigher @Inject constructor(
    private val xcmTransferDryRunner: XcmTransferDryRunner,
) {

    suspend fun estimateFee(
        config: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase
    ): CrossChainFeeModel {
        return estimateFee(config, transfer, minimumSendAmount = MINIMUM_SEND_AMOUNT)
            .recoverCatching { error ->
                Log.w(LOG_TAG, "Fee dry run with $MINIMUM_SEND_AMOUNT tokens failed, retrying with $FALLBACK_SEND_AMOUNT", error)

                estimateFee(config, transfer, minimumSendAmount = FALLBACK_SEND_AMOUNT).getOrThrow()
            }
            .getOrThrow()
    }

    private suspend fun estimateFee(
        config: DynamicCrossChainTransferConfiguration,
        transfer: AssetTransferBase,
        minimumSendAmount: Int
    ): Result<CrossChainFeeModel> {
        val safeTransfer = transfer.ensureSafeAmount(minimumSendAmount)

        return xcmTransferDryRunner.dryRunXcmTransfer(config, safeTransfer, XcmTransferDryRunOrigin.Fake)
            .map { result ->
                CrossChainFeeModel.fromDryRunResult(
                    initialAmount = safeTransfer.amountPlanks,
                    transferDryRunResult = result
                )
            }
    }

    // Ensure we can calculate fee regardless of what user entered
    private fun AssetTransferBase.ensureSafeAmount(minimumSendAmount: Int): AssetTransferBase {
        val minimumSendPlanks = destinationChainAsset.planksFromAmount(minimumSendAmount.toBigDecimal())
        val safeAmount = amountPlanks.coerceAtLeast(minimumSendPlanks)
        return replaceAmount(newAmount = safeAmount)
    }

    private fun CrossChainFeeModel.Companion.fromDryRunResult(
        initialAmount: Balance,
        transferDryRunResult: XcmTransferDryRunResult
    ): CrossChainFeeModel {
        return with(transferDryRunResult) {
            // We do not add `remoteReserve.deliveryFee` since it is paid from holding and not by account
            val paidByAccount = origin.deliveryFee

            val trapped = origin.trapped + remoteReserve?.trapped.orZero()
            val totalFee = initialAmount - destination.depositedAmount - trapped

            // We do not subtract `origin.deliveryFee` since it is paid directly from the origin account and thus do not contribute towards execution fee
            // We do not subtract `remoteReserve.deliveryFee` since it is paid from holding and thus is already accounted in totalFee
            val executionFee = totalFee

            CrossChainFeeModel(paidByAccount = paidByAccount, paidFromHolding = executionFee)
        }
    }
}
