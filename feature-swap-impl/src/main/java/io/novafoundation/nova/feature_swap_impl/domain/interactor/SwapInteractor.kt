package io.novafoundation.nova.feature_swap_impl.domain.interactor

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.quotedBalance
import io.novafoundation.nova.feature_swap_api.domain.model.toExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.AssetConversionExchangeFactory
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationPayload
import io.novafoundation.nova.feature_swap_impl.domain.validation.utils.SharedQuoteValidationRetriever
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationSystem
import io.novafoundation.nova.feature_swap_impl.domain.validation.positiveAmount
import io.novafoundation.nova.feature_swap_impl.domain.validation.availableSlippage
import io.novafoundation.nova.feature_swap_impl.domain.validation.checkForFeeChanges
import io.novafoundation.nova.feature_swap_impl.domain.validation.enoughLiquidity
import io.novafoundation.nova.feature_swap_impl.domain.validation.rateNotExceedSlippage
import io.novafoundation.nova.feature_swap_impl.domain.validation.sufficientBalanceInUsedAsset
import io.novafoundation.nova.feature_swap_impl.domain.validation.sufficientBalanceInFeeAsset
import io.novafoundation.nova.feature_swap_impl.domain.validation.sufficientRecipientBalanceToStayAboveED
import io.novafoundation.nova.feature_swap_impl.domain.validation.swapFeeSufficientBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.swapSmallRemainingBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SwapInteractor(
    private val swapService: SwapService,
    private val assetExchangeFactory: AssetConversionExchangeFactory,
    private val enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val chainRegistry: ChainRegistry
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

            swapSmallRemainingBalance(assetSourceRegistry, chainRegistry)

            sufficientBalanceInFeeAsset()

            sufficientBalanceInUsedAsset()

            sufficientRecipientBalanceToStayAboveED(enoughTotalToStayAboveEDValidationFactory)

            checkForFeeChanges(swapService)
        }
    }

    suspend fun getValidationPayload(
        swapSettings: SwapSettings,
        quoteArgs: SwapQuoteArgs,
        swapQuote: SwapQuote,
        swapFee: SwapFee
    ): SwapValidationPayload? {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val assetIn = swapSettings.assetIn ?: return null
        val assetOut = swapSettings.assetOut ?: return null
        val feeChainAsset = swapSettings.feeAsset ?: return null
        val chainIn = chainRegistry.getChain(swapQuote.assetIn.chainId)
        val chainOut = chainRegistry.getChain(swapQuote.assetOut.chainId)
        val nativeChainAssetIn = chainIn.commissionAsset

        val executeArgs = quoteArgs.toExecuteArgs(
            quotedBalance = swapQuote.quotedBalance,
            customFeeAsset = feeChainAsset,
            nativeAsset = walletRepository.getAsset(metaAccount.id, nativeChainAssetIn) ?: return null
        )
        return SwapValidationPayload(
            detailedAssetIn = SwapValidationPayload.SwapAssetData(
                chain = chainIn,
                asset = walletRepository.getAsset(metaAccount.id, assetIn) ?: return null,
                amount = assetIn.amountFromPlanks(swapQuote.planksIn)
            ),
            detailedAssetOut = SwapValidationPayload.SwapAssetData(
                chain = chainOut,
                asset = walletRepository.getAsset(metaAccount.id, assetOut) ?: return null,
                amount = assetOut.amountFromPlanks(swapQuote.planksOut)
            ),
            slippage = swapSettings.slippage,
            feeAsset = walletRepository.getAsset(metaAccount.id, feeChainAsset) ?: return null,
            swapFee = swapFee,
            swapQuote = swapQuote,
            swapQuoteArgs = quoteArgs,
            swapExecuteArgs = executeArgs
        )
    }
}
