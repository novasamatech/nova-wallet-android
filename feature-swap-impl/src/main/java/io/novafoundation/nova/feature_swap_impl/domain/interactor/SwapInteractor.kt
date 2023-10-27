package io.novafoundation.nova.feature_swap_impl.domain.interactor

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.AssetConversionExchangeFactory
import io.novafoundation.nova.feature_swap_impl.domain.validation.utils.SharedQuoteValidationRetriever
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationSystem
import io.novafoundation.nova.feature_swap_impl.domain.validation.positiveAmount
import io.novafoundation.nova.feature_swap_impl.domain.validation.availableSlippage
import io.novafoundation.nova.feature_swap_impl.domain.validation.checkForFeeChanges
import io.novafoundation.nova.feature_swap_impl.domain.validation.enoughLiquidity
import io.novafoundation.nova.feature_swap_impl.domain.validation.rateNotExceedSlippage
import io.novafoundation.nova.feature_swap_impl.domain.validation.sufficientBalanceInUsedAsset
import io.novafoundation.nova.feature_swap_impl.domain.validation.sufficientBalanceToPayFeeInUsedAsset
import io.novafoundation.nova.feature_swap_impl.domain.validation.sufficientCommissionBalanceToStayAboveED
import io.novafoundation.nova.feature_swap_impl.domain.validation.swapFeeSufficientBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.swapSmallRemainingBalance
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SwapInteractor(
    private val swapService: SwapService,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val assetExchangeFactory: AssetConversionExchangeFactory,
    private val enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
) {

    suspend fun quote(quoteArgs: SwapQuoteArgs): Result<SwapQuote> {
        return swapService.quote(quoteArgs)
    }

    suspend fun canPayFeeInCustomAsset(asset: Chain.Asset): Boolean {
        return swapService.canPayFeeInNonUtilityAsset(asset)
    }

    suspend fun estimateFee(executeArgs: SwapExecuteArgs): SwapFee {
        return swapService.estimateFee(executeArgs)
    }

    suspend fun validationSystem(chainId: ChainId): SwapValidationSystem? {
        val assetExchange = assetExchangeFactory.create(chainId) ?: return null
        val sharedQuoteValidationRetriever = SharedQuoteValidationRetriever(swapService)

        return ValidationSystem {
            positiveAmount()

            availableSlippage(assetExchange)

            enoughLiquidity(sharedQuoteValidationRetriever)

            rateNotExceedSlippage(sharedQuoteValidationRetriever)

            swapFeeSufficientBalance()

            swapSmallRemainingBalance()

            sufficientBalanceToPayFeeInUsedAsset()

            sufficientBalanceInUsedAsset()

            sufficientCommissionBalanceToStayAboveED(enoughTotalToStayAboveEDValidationFactory)

            checkForFeeChanges(swapService)
        }
    }
}
