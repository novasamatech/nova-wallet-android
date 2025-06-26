package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.validations

import android.util.Log
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.senderAccountId
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun.XcmTransferDryRunOrigin
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun.XcmTransferDryRunner
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

@FeatureScope
class DryRunSucceedsValidationFactory @Inject constructor(
    private val assetTransferDryRunner: XcmTransferDryRunner,
    private val crossChainTransfersUseCase: dagger.Lazy<CrossChainTransfersUseCase>,
) {

    context(AssetTransfersValidationSystemBuilder)
    fun dryRunSucceeds() {
        validate(DryRunSucceedsValidation(assetTransferDryRunner, crossChainTransfersUseCase.get()))
    }
}

private class DryRunSucceedsValidation(
    private val assetTransferDryRunner: XcmTransferDryRunner,
    private val crossChainTransfersUseCase: CrossChainTransfersUseCase,
) : AssetTransfersValidation {

    override suspend fun validate(value: AssetTransferPayload): ValidationStatus<AssetTransferValidationFailure> {
        val config = crossChainTransfersUseCase.transferConfigurationFor(value.transfer, cachingScope = CoroutineScope(coroutineContext))

        // Skip validation if it is not a cross chain transfer
        val crossChainFee = value.crossChainFee ?: return valid()

        if (config !is CrossChainTransferConfiguration.Dynamic) {
            Log.d(LOG_TAG, "Dry run validation is not available - skipping")
            return valid()
        }

        val dryRunResult = assetTransferDryRunner.dryRunXcmTransfer(
            config = config.config,
            transfer = value.transfer,
            origin = XcmTransferDryRunOrigin.Signed(value.transfer.senderAccountId, crossChainFee.amount)
        )

        return dryRunResult.isSuccess isTrueOrError {
            Log.e(LOG_TAG, "Dry run failed", dryRunResult.exceptionOrNull())

            AssetTransferValidationFailure.DryRunFailed
        }
    }
}
