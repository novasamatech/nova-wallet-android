package io.novafoundation.nova.feature_swap_impl.domain.interactor

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_buy_api.domain.TradeTokenRegistry
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFeeArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapProgress
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.allBasicFees
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_impl.data.network.blockhain.updaters.SwapUpdateSystemFactory
import io.novafoundation.nova.feature_swap_impl.data.repository.SwapTransactionHistoryRepository
import io.novafoundation.nova.feature_swap_impl.domain.model.GetAssetInOption
import io.novafoundation.nova.feature_swap_impl.domain.swap.PriceImpactThresholds
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapValidationSystem
import io.novafoundation.nova.feature_swap_impl.domain.validation.availableSlippage
import io.novafoundation.nova.feature_swap_impl.domain.validation.canPayAllFees
import io.novafoundation.nova.feature_swap_impl.domain.validation.enoughAssetInToPayForSwap
import io.novafoundation.nova.feature_swap_impl.domain.validation.enoughAssetInToPayForSwapAndFee
import io.novafoundation.nova.feature_swap_impl.domain.validation.enoughLiquidity
import io.novafoundation.nova.feature_swap_impl.domain.validation.positiveAmountIn
import io.novafoundation.nova.feature_swap_impl.domain.validation.positiveAmountOut
import io.novafoundation.nova.feature_swap_impl.domain.validation.priceImpactValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.rateNotExceedSlippage
import io.novafoundation.nova.feature_swap_impl.domain.validation.sufficientAmountOutToStayAboveED
import io.novafoundation.nova.feature_swap_impl.domain.validation.sufficientBalanceConsideringConsumersValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.swapFeeSufficientBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.swapSmallRemainingBalance
import io.novafoundation.nova.feature_swap_impl.domain.validation.utils.SharedQuoteValidationRetriever
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.intermediateReceivesMeetEDValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.sufficientBalanceConsideringNonSufficientAssetsValidation
import io.novafoundation.nova.feature_swap_impl.domain.validation.validations.sufficientNativeBalanceToPayFeeConsideringED
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.incomingCrossChainDirectionsAvailable
import io.novafoundation.nova.feature_wallet_api.domain.model.FiatAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.validation.context.AssetsValidationContext
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class SwapInteractor(
    private val priceImpactThresholds: PriceImpactThresholds,
    private val swapService: SwapService,
    private val buyTokenRegistry: TradeTokenRegistry,
    private val crossChainTransfersUseCase: CrossChainTransfersUseCase,
    private val accountRepository: AccountRepository,
    private val tokenRepository: TokenRepository,
    private val swapUpdateSystemFactory: SwapUpdateSystemFactory,
    private val assetsValidationContextFactory: AssetsValidationContext.Factory,
    private val swapTransactionHistoryRepository: SwapTransactionHistoryRepository
) {

    suspend fun getAllFeeTokens(swapFee: SwapFee): Map<FullChainAssetId, Token> {
        val basicFees = swapFee.allBasicFees()
        val chainAssets = basicFees.map { it.asset }

        return tokenRepository.getTokens(chainAssets)
    }

    suspend fun calculateSegmentFiatPrices(swapFee: SwapFee): List<FiatAmount> {
        return withContext(Dispatchers.Default) {
            val basicFeesBySegment = swapFee.segments.map { it.fee.allBasicFees() }
            val chainAssets = basicFeesBySegment.flatMap { segmentFees -> segmentFees.map { it.asset } }

            val tokens = tokenRepository.getTokens(chainAssets)
            val currency = tokens.values.first().currency

            basicFeesBySegment.map { segmentBasicFees ->
                val totalSegmentFees = segmentBasicFees.sumOf { basicFee ->
                    val token = tokens[basicFee.asset.fullId]
                    token?.planksToFiat(basicFee.amount) ?: BigDecimal.ZERO
                }

                FiatAmount(currency, totalSegmentFees)
            }
        }
    }

    suspend fun calculateTotalFiatPrice(swapFee: SwapFee): FiatAmount {
        return withContext(Dispatchers.Default) {
            val basicFees = swapFee.allBasicFees()
            val chainAssets = basicFees.map { it.asset }
            val tokens = tokenRepository.getTokens(chainAssets)

            val totalFiat = basicFees.sumOf { basicFee ->
                val token = tokens[basicFee.asset.fullId] ?: return@sumOf BigDecimal.ZERO
                token.planksToFiat(basicFee.amount)
            }

            FiatAmount(
                currency = tokens.values.first().currency,
                price = totalFiat
            )
        }
    }

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

    suspend fun executeSwap(calculatedFee: SwapFee): Flow<SwapProgress> = swapService.swap(calculatedFee)

    suspend fun warmUpSwapCommonlyUsedChains(computationalScope: CoroutineScope) {
        swapService.warmUpCommonChains(computationalScope)
    }

    suspend fun estimateFee(executeArgs: SwapFeeArgs): SwapFee {
        return swapService.estimateFee(executeArgs)
    }

    suspend fun slippageConfig(chainId: ChainId): SlippageConfig {
        return swapService.defaultSlippageConfig(chainId)
    }

    fun runSubscriptions(metaAccount: MetaAccount): Flow<ReQuoteTrigger> {
        return swapService.runSubscriptions(metaAccount)
    }

    private fun buyAvailable(chainAssetFlow: Flow<Chain.Asset?>): Flow<Boolean> {
        return chainAssetFlow.map { it != null && buyTokenRegistry.hasProvider(it, TradeTokenRegistry.TradeType.BUY) }
    }

    private fun receiveAvailable(chainAssetFlow: Flow<Chain.Asset?>): Flow<Boolean> {
        return combine(accountRepository.selectedMetaAccountFlow(), chainAssetFlow) { metaAccout, asset ->
            metaAccout.type != Type.WATCH_ONLY && asset != null
        }
    }

    fun validationSystem(): SwapValidationSystem {
        val assetsValidationContext = assetsValidationContextFactory.create()
        val sharedQuoteValidationRetriever = SharedQuoteValidationRetriever(swapService, assetsValidationContext)

        return ValidationSystem {
            positiveAmountIn()

            positiveAmountOut()

            canPayAllFees(assetsValidationContext)

            enoughAssetInToPayForSwap(assetsValidationContext)

            enoughAssetInToPayForSwapAndFee(assetsValidationContext)

            sufficientNativeBalanceToPayFeeConsideringED(assetsValidationContext)

            availableSlippage(swapService)

            enoughLiquidity(sharedQuoteValidationRetriever)

            rateNotExceedSlippage(sharedQuoteValidationRetriever)

            intermediateReceivesMeetEDValidation(assetsValidationContext)

            swapFeeSufficientBalance(assetsValidationContext)

            sufficientBalanceConsideringNonSufficientAssetsValidation(assetsValidationContext)

            sufficientBalanceConsideringConsumersValidation(assetsValidationContext)

            swapSmallRemainingBalance(assetsValidationContext)

            sufficientAmountOutToStayAboveED(assetsValidationContext)

            priceImpactValidation(priceImpactThresholds)
        }
    }
}
