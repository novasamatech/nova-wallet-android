package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.validations

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferPayload
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransferValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.AssetTransfersValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers.senderAccountId
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.XcmTransferDryRunOrigin
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

@FeatureScope
class DryRunSucceedsValidationFactory @Inject constructor(
    private val crossChainTransfersUseCase: CrossChainTransfersUseCase,
) {

    context(AssetTransfersValidationSystemBuilder)
    fun dryRunSucceeds() {
        validate(DryRunSucceedsValidation(crossChainTransfersUseCase))
    }
}

private class DryRunSucceedsValidation(
    private val crossChainTransfersUseCase: CrossChainTransfersUseCase,
) : AssetTransfersValidation {

    override suspend fun validate(value: AssetTransferPayload): ValidationStatus<AssetTransferValidationFailure> {
        // Skip validation if it is not a cross chain transfer
        val crossChainFee = value.crossChainFee ?: return valid()

        val dryRunResult = crossChainTransfersUseCase.dryRunTransferIfPossible(
            transfer = value.transfer,
            origin = XcmTransferDryRunOrigin.Signed(value.transfer.senderAccountId, crossChainFee.amount),
            computationalScope = CoroutineScope(coroutineContext)
        )

        return dryRunResult.isSuccess isTrueOrError {
            AssetTransferValidationFailure.DryRunFailed
        }
    }
}
