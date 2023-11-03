package io.novafoundation.nova.feature_swap_impl.domain.interactor

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicHash
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.quotedBalance
import io.novafoundation.nova.feature_swap_api.domain.model.toExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
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
import io.novafoundation.nova.feature_swap_impl.domain.validation.sufficientAssetOutBalanceToStayAboveED
import io.novafoundation.nova.feature_swap_impl.domain.validation.swapFeeSufficientBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.swapSmallRemainingBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig

class SwapInteractor(
    private val swapService: SwapService,
    private val chainStateRepository: ChainStateRepository,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val chainRegistry: ChainRegistry
) {

    suspend fun quote(quoteArgs: SwapQuoteArgs): Result<SwapQuote> {
        return swapService.quote(quoteArgs)
    }

    suspend fun executeSwap(swapExecuteArgs: SwapExecuteArgs): Result<ExtrinsicHash> {
        return swapService.swap(swapExecuteArgs)
    }

    suspend fun canPayFeeInCustomAsset(asset: Chain.Asset): Boolean {
        return swapService.canPayFeeInNonUtilityAsset(asset)
    }

    suspend fun estimateFee(executeArgs: SwapExecuteArgs): SwapFee {
        return swapService.estimateFee(executeArgs)
    }

    suspend fun slippageConfig(chainId: ChainId): SlippageConfig? {
        return swapService.slippageConfig(chainId)
    }

    fun blockNumberUpdates(chainId: ChainId): Flow<BlockNumber> {
        return chainStateRepository.currentBlockNumberFlow(chainId)
            .drop(1) // skip immediate value from the cache to not perform double-quote on chain change
    }

    fun validationSystem(): SwapValidationSystem {
        val sharedQuoteValidationRetriever = SharedQuoteValidationRetriever(swapService)

        return ValidationSystem {
            positiveAmount()

            availableSlippage(swapService)

            enoughLiquidity(sharedQuoteValidationRetriever)

            rateNotExceedSlippage(sharedQuoteValidationRetriever)

            sufficientBalanceInUsedAsset()

            swapFeeSufficientBalance()

            sufficientBalanceInFeeAsset()

            swapSmallRemainingBalance(assetSourceRegistry)

            sufficientAssetOutBalanceToStayAboveED(assetSourceRegistry)

            checkForFeeChanges(swapService)
        }
    }

    suspend fun getValidationPayload(
        assetIn: Chain.Asset,
        assetOut: Chain.Asset,
        feeAsset: Chain.Asset,
        quoteArgs: SwapQuoteArgs,
        swapQuote: SwapQuote,
        swapFee: SwapFee
    ): SwapValidationPayload? {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chainIn = chainRegistry.getChain(swapQuote.assetIn.chainId)
        val chainOut = chainRegistry.getChain(swapQuote.assetOut.chainId)
        val nativeChainAssetIn = chainIn.commissionAsset

        val executeArgs = quoteArgs.toExecuteArgs(
            quotedBalance = swapQuote.quotedBalance,
            customFeeAsset = feeAsset,
            nativeAsset = walletRepository.getAsset(metaAccount.id, nativeChainAssetIn) ?: return null
        )
        return SwapValidationPayload(
            detailedAssetIn = SwapValidationPayload.SwapAssetData(
                chain = chainIn,
                asset = walletRepository.getAsset(metaAccount.id, assetIn) ?: return null,
                amountInPlanks = swapQuote.planksIn
            ),
            detailedAssetOut = SwapValidationPayload.SwapAssetData(
                chain = chainOut,
                asset = walletRepository.getAsset(metaAccount.id, assetOut) ?: return null,
                amountInPlanks = swapQuote.planksOut
            ),
            slippage = quoteArgs.slippage,
            feeAsset = walletRepository.getAsset(metaAccount.id, feeAsset) ?: return null,
            swapFee = swapFee,
            swapQuote = swapQuote,
            swapQuoteArgs = quoteArgs,
            swapExecuteArgs = executeArgs
        )
    }
}
