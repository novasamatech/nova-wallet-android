package io.novafoundation.nova.feature_wallet_impl.domain

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.isPositive
import io.novafoundation.nova.common.utils.withFlowScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainTransfersRepository
import io.novafoundation.nova.feature_wallet_api.domain.implementations.availableInDestinations
import io.novafoundation.nova.feature_wallet_api.domain.implementations.availableOutDestinations
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.CrossChainTransfersUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.IncomingDirection
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.OutcomingDirection
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.assets
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val INCOMING_DIRECTIONS = "RealCrossChainTransfersUseCase.INCOMING_DIRECTIONS"

internal class RealCrossChainTransfersUseCase(
    private val crossChainTransfersRepository: CrossChainTransfersRepository,
    private val walletRepository: WalletRepository,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val computationalCache: ComputationalCache,
) : CrossChainTransfersUseCase {

    override fun incomingCrossChainDirections(destination: Flow<Chain.Asset?>): Flow<List<IncomingDirection>> {
        return withFlowScope { scope ->
            computationalCache.useSharedFlow(INCOMING_DIRECTIONS, scope) {
                scope.launch { crossChainTransfersRepository.syncConfiguration() }

                combineToPair(destination, crossChainTransfersRepository.configurationFlow()).flatMapLatest { (destinationAsset, crossChainConfig) ->
                    if (destinationAsset == null) return@flatMapLatest flowOf(emptyList())

                    val selectedMetaAccountId = accountRepository.getSelectedMetaAccount().id
                    val availableDirections = crossChainConfig.availableInDestinations(destinationAsset)

                    val chains = chainRegistry.chainsById()
                    val availableDirectionChainAssets = chains.assets(availableDirections)

                    walletRepository.assetsFlow(selectedMetaAccountId, availableDirectionChainAssets).map { balances ->
                        balances
                            .filter { it.transferable.isPositive }
                            .map { IncomingDirection(it, chains.getValue(it.token.configuration.chainId)) }
                    }
                }
            }
        }.catch { emit(emptyList()) }
    }

    override fun outcomingCrossChainDirections(origin: Chain.Asset): Flow<List<OutcomingDirection>> {
        return withFlowScope { scope ->
            scope.launch { crossChainTransfersRepository.syncConfiguration() }

            crossChainTransfersRepository.configurationFlow().map { configuration ->
                val chainsById = chainRegistry.chainsById.first()

                configuration.availableOutDestinations(origin).mapNotNull { (chainId, assetId) ->
                    val chain = chainsById[chainId] ?: return@mapNotNull null
                    val asset = chain.assetsById[assetId] ?: return@mapNotNull null

                    ChainWithAsset(chain, asset)
                }
            }
        }.catch { emit(emptyList()) }
    }
}
