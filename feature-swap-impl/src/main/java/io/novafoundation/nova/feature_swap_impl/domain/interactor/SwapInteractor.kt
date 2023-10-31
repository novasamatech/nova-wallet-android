package io.novafoundation.nova.feature_swap_impl.domain.interactor

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.common.utils.withFlowScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_buy_api.domain.BuyTokenRegistry
import io.novafoundation.nova.feature_buy_api.domain.hasProvidersFor
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_impl.domain.model.GetAssetInOption
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.domain.implementations.availableInDestinations
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.assets
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SwapInteractor(
    private val swapService: SwapService,
    private val chainStateRepository: ChainStateRepository,
    private val crossChainTransfersRepository: CrossChainTransfersRepository,
    private val buyTokenRegistry: BuyTokenRegistry,
    private val walletRepository: WalletRepository,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository
) {

    fun availableGetAssetInOptionsFlow(chainAssetFlow: Flow<Chain.Asset?>): Flow<Set<GetAssetInOption>> {
        return combine(
            crossChainTransfersAvailable(chainAssetFlow),
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

    suspend fun quote(quoteArgs: SwapQuoteArgs): Result<SwapQuote> {
        return swapService.quote(quoteArgs)
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

    private fun buyAvailable(chainAssetFlow: Flow<Chain.Asset?>): Flow<Boolean> {
        return chainAssetFlow.map { it != null && buyTokenRegistry.hasProvidersFor(it) }
    }

    private fun receiveAvailable(chainAssetFlow: Flow<Chain.Asset?>): Flow<Boolean> {
        return chainAssetFlow.map { it != null }
    }

    private fun crossChainTransfersAvailable(chainAssetFlow: Flow<Chain.Asset?>): Flow<Boolean> {
        val crossChainConfigFlow = withFlowScope { scope ->
            scope.launch { crossChainTransfersRepository.syncConfiguration() }

            crossChainTransfersRepository.configurationFlow()
        }

        return combineToPair(chainAssetFlow, crossChainConfigFlow).flatMapLatest { (chainAsset, crossChainConfig) ->
            if (chainAsset == null) return@flatMapLatest flowOf(false)

            val selectedMetaAccountId = accountRepository.getSelectedMetaAccount().id
            val availableDirections = crossChainConfig.availableInDestinations(chainAsset)
            val availableDirectionChainAssets = chainRegistry.assets(availableDirections)

            walletRepository.assetsFlow(selectedMetaAccountId, availableDirectionChainAssets).map { balances ->
                balances.any { it.transferable.isPositive }
            }
        }
    }
}
