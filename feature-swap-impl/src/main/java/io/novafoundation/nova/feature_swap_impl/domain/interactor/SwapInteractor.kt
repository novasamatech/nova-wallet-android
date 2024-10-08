package io.novafoundation.nova.feature_swap_impl.domain.interactor

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_buy_api.domain.BuyTokenRegistry
import io.novafoundation.nova.feature_buy_api.domain.hasProvidersFor
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionCorrection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_impl.data.network.blockhain.updaters.SwapUpdateSystemFactory
import io.novafoundation.nova.feature_swap_impl.data.repository.SwapTransactionHistoryRepository
import io.novafoundation.nova.feature_swap_impl.domain.model.GetAssetInOption
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationSystem
import io.novafoundation.nova.feature_swap_impl.domain.validation.availableSlippage
import io.novafoundation.nova.feature_swap_impl.domain.validation.checkForFeeChanges
import io.novafoundation.nova.feature_swap_impl.domain.validation.enoughLiquidity
import io.novafoundation.nova.feature_swap_impl.domain.validation.positiveAmountIn
import io.novafoundation.nova.feature_swap_impl.domain.validation.positiveAmountOut
import io.novafoundation.nova.feature_swap_impl.domain.validation.rateNotExceedSlippage
import io.novafoundation.nova.feature_swap_impl.domain.validation.sufficientAssetOutBalanceToStayAboveED
import io.novafoundation.nova.feature_swap_impl.domain.validation.sufficientBalanceConsideringConsumersValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.sufficientBalanceConsideringNonSufficientAssetsValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.sufficientBalanceInFeeAsset
import io.novafoundation.nova.feature_swap_impl.domain.validation.sufficientBalanceInUsedAsset
import io.novafoundation.nova.feature_swap_impl.domain.validation.swapFeeSufficientBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.swapSmallRemainingBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.utils.SharedQuoteValidationRetriever
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.sufficientNativeBalanceToPayFeeConsideringED
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.incomingCrossChainDirectionsAvailable
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SwapInteractor(
    private val swapService: SwapService,
    private val buyTokenRegistry: BuyTokenRegistry,
    private val crossChainTransfersUseCase: CrossChainTransfersUseCase,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val walletRepository: WalletRepository,
    private val swapUpdateSystemFactory: SwapUpdateSystemFactory,
    private val swapTransactionHistoryRepository: SwapTransactionHistoryRepository
) {

    suspend fun sync(coroutineScope: CoroutineScope) {
        swapService.sync(coroutineScope)
    }

    suspend fun getUpdateSystem(chainFlow: Flow<Chain>, coroutineScope: CoroutineScope): UpdateSystem {
        return swapUpdateSystemFactory.create(chainFlow, coroutineScope)
    }

    fun availableGetAssetInOptionsFlow(chainAssetFlow: Flow<Chain.Asset?>): Flow<Set<GetAssetInOption>> {
        return combine(
            crossChainTransfersUseCase.incomingCrossChainDirectionsAvailable(chainAssetFlow),
            buyAvailable(chainAssetFlow),
            receiveAvailable(chainAssetFlow),
        ) { crossChainTransfersAvailable, buyAvailable, receiveAvailable ->
            setOfNotNull(
                GetAssetInOption.CROSS_CHAIN.takeIf { crossChainTransfersAvailable },
                GetAssetInOption.RECEIVE.takeIf { receiveAvailable },
                GetAssetInOption.BUY.takeIf { buyAvailable }
            )
        }
    }

    suspend fun quote(quoteArgs: SwapQuoteArgs, computationalScope: CoroutineScope): Result<SwapQuote> {
        return swapService.quote(quoteArgs, computationalScope)
    }

    suspend fun executeSwap(swapExecuteArgs: SwapExecuteArgs): Result<SwapExecutionCorrection> = withContext(Dispatchers.IO) {
        swapService.swap(swapExecuteArgs)
//            .onSuccess { submission ->
//                swapTransactionHistoryRepository.insertPendingSwap(
//                    chainAsset = swapExecuteArgs.assetIn,
//                    swapArgs = swapExecuteArgs,
//                    fee = decimalFee.genericFee,
//                    txSubmission = submission
//                )
//
//                swapTransactionHistoryRepository.insertPendingSwap(
//                    chainAsset = swapExecuteArgs.assetOut,
//                    swapArgs = swapExecuteArgs,
//                    fee = decimalFee.genericFee,
//                    txSubmission = submission
//                )
//            }
    }

    suspend fun canPayFeeInCustomAsset(asset: Chain.Asset): Boolean {
        return swapService.canPayFeeInNonUtilityAsset(asset)
    }

    suspend fun estimateFee(executeArgs: SwapExecuteArgs): SwapFee {
        return swapService.estimateFee(executeArgs)
    }

    suspend fun slippageConfig(chainId: ChainId): SlippageConfig? {
        return swapService.defaultSlippageConfig(chainId)
    }

    fun runSubscriptions(metaAccount: MetaAccount): Flow<ReQuoteTrigger> {
        return swapService.runSubscriptions(metaAccount)
    }

    private fun buyAvailable(chainAssetFlow: Flow<Chain.Asset?>): Flow<Boolean> {
        return chainAssetFlow.map { it != null && buyTokenRegistry.hasProvidersFor(it) }
    }

    private fun receiveAvailable(chainAssetFlow: Flow<Chain.Asset?>): Flow<Boolean> {
        return combine(accountRepository.selectedMetaAccountFlow(), chainAssetFlow) { metaAccout, asset ->
            metaAccout.type != Type.WATCH_ONLY && asset != null
        }
    }

    fun validationSystem(): SwapValidationSystem {
        val sharedQuoteValidationRetriever = SharedQuoteValidationRetriever(swapService)

        return ValidationSystem {
            positiveAmountIn()

            positiveAmountOut()

            sufficientBalanceInFeeAsset()

            sufficientNativeBalanceToPayFeeConsideringED(assetSourceRegistry, chainRegistry)

            availableSlippage(swapService)

            enoughLiquidity(sharedQuoteValidationRetriever)

            rateNotExceedSlippage(sharedQuoteValidationRetriever)

            sufficientBalanceInUsedAsset()

            swapFeeSufficientBalance()

            sufficientBalanceConsideringNonSufficientAssetsValidation(assetSourceRegistry)

            sufficientBalanceConsideringConsumersValidation(assetSourceRegistry)

            swapSmallRemainingBalance(assetSourceRegistry)

            sufficientAssetOutBalanceToStayAboveED(assetSourceRegistry)

            checkForFeeChanges(swapService)
        }
    }

//    suspend fun getValidationPayload(
//        assetIn: Chain.Asset,
//        assetOut: Chain.Asset,
//        feeAsset: Chain.Asset,
//        quoteArgs: SwapQuoteArgs,
//        swapQuote: SwapQuote,
//        swapFee: GenericDecimalFee<SwapFee>
//    ): SwapValidationPayload? {
//        val metaAccount = accountRepository.getSelectedMetaAccount()
//        val chainIn = chainRegistry.getChain(swapQuote.assetIn.chainId)
//        val chainOut = chainRegistry.getChain(swapQuote.assetOut.chainId)
//        val nativeChainAssetIn = chainIn.commissionAsset
//
//        val executeArgs = quoteArgs.toExecuteArgs(
//            quote = swapQuote,
//            customFeeAsset = feeAsset,
//            nativeAsset = walletRepository.getAsset(metaAccount.id, nativeChainAssetIn) ?: return null
//        )
//        return SwapValidationPayload(
//            detailedAssetIn = SwapValidationPayload.SwapAssetData(
//                chain = chainIn,
//                asset = walletRepository.getAsset(metaAccount.id, assetIn) ?: return null,
//                amountInPlanks = swapQuote.planksIn
//            ),
//            detailedAssetOut = SwapValidationPayload.SwapAssetData(
//                chain = chainOut,
//                asset = walletRepository.getAsset(metaAccount.id, assetOut) ?: return null,
//                amountInPlanks = swapQuote.planksOut
//            ),
//            slippage = quoteArgs.slippage,
//            feeAsset = walletRepository.getAsset(metaAccount.id, feeAsset) ?: return null,
//            decimalFee = swapFee,
//            swapQuote = swapQuote,
//            swapQuoteArgs = quoteArgs,
//            swapExecuteArgs = executeArgs
//        )
//    }
}
