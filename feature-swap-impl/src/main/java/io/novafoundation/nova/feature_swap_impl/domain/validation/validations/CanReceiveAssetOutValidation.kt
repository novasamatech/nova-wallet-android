package io.novafoundation.nova.feature_swap_impl.domain.validation.validations

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationFailure
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationSystemBuilder
import io.novafoundation.nova.feature_wallet_api.data.repository.AccountInfoRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.getExistentialDeposit
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import javax.inject.Inject

@FeatureScope
class CanReceiveAssetOutValidationFactory @Inject constructor(
    private val accountInfoRepository: AccountInfoRepository,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
) {

    context(SwapValidationSystemBuilder)
    fun canReceiveAssetOut(validationContext: AssetsValidationContext) {
        validate(CanReceiveAssetOutValidation(accountInfoRepository, chainRegistry, accountRepository, validationContext))
    }
}

/**
 * 1. asset out is sufficient OR
 *
 * 2. remaining providers (minus 1 if asset in is on the same chain, sufficient and dusted) is positive
 *
 * Otherwise it is not possible to receive insufficient assets on destination
 */
class CanReceiveAssetOutValidation(
    private val accountInfoRepository: AccountInfoRepository,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val assetsValidationContext: AssetsValidationContext,
) : SwapValidation {

    override suspend fun validate(value: SwapValidationPayload): ValidationStatus<SwapValidationFailure> {
        val isAssetOutSufficient = assetsValidationContext.isAssetSufficient(value.amountOut.chainAsset)
        if (isAssetOutSufficient) return valid()

        val chainAssetOut = value.amountOut.chainAsset
        val chainOut = chainRegistry.getChain(chainAssetOut.chainId)

        val metaAccount = accountRepository.getSelectedMetaAccount()
        val recipientAccountId = metaAccount.accountIdIn(chainOut) ?: return valid()

        val destinationAccountInfo = accountInfoRepository.getAccountInfo(chainOut.id, recipientAccountId)

        val providersDecrease = if (swapDecreasesProviders(value)) 1 else 0
        val remainingProviders = destinationAccountInfo.providers.toInt() - providersDecrease

        return (remainingProviders > 0) isTrueOrError {
            val destinationChainNativeAsset = chainOut.utilityAsset
            val destinationChainNativeAssetEd = assetsValidationContext.getExistentialDeposit(destinationChainNativeAsset)

            SwapValidationFailure.CannotReceiveAssetOut(
                destination = ChainWithAsset(chainOut, chainAssetOut),
                requiredNativeAssetOnChainOut = destinationChainNativeAsset.withAmount(destinationChainNativeAssetEd)
            )
        }
    }

    private suspend fun swapDecreasesProviders(
        value: SwapValidationPayload
    ): Boolean {
        val assetIn = value.amountIn.chainAsset
        val assetOut = value.amountOut.chainAsset

        // Asset in does not affect providers on destination chain when its on different chain
        if (assetIn.chainId != assetOut.chainId) return false

        // If asset in is not sufficient, it cannot influence number of providers even if dusted
        if (assetsValidationContext.isAssetSufficient(assetIn)) return false

        val assetInBalance = assetsValidationContext.getAsset(assetIn)
        val assetInEd = assetsValidationContext.getExistentialDeposit(assetIn)

        val swapDustsAssetIn = assetInBalance.balanceCountedTowardsEDInPlanks - value.amountIn.amount < assetInEd

        return swapDustsAssetIn
    }
}
