package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolio
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorWalletContext
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

interface BittensorPortfolioRepository {

    fun observeWallet(): Flow<BittensorWalletContext>

    fun observeAsset(): Flow<Asset>

    fun observeRootPortfolio(): Flow<BittensorPortfolio>

    fun observePortfolio(): Flow<BittensorPortfolio>
}

class RealBittensorPortfolioRepository(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val portfolioLoader: BittensorPortfolioLoader,
    private val chainStateRepository: ChainStateRepository,
    private val logger: Logger
) : BittensorPortfolioRepository {

    override fun observeAsset(): Flow<Asset> = observeWallet()
        .map { it.asset }

    override fun observeWallet(): Flow<BittensorWalletContext> = flowOfAll {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        accountRepository.selectedMetaAccountFlow()
            .flatMapLatest { account ->
                walletRepository.assetFlow(account.id, chain.utilityAsset)
                    .map { asset -> BittensorWalletContext(account.id, chain, asset) }
            }
    }

    override fun observeRootPortfolio(): Flow<BittensorPortfolio> = observePortfolio(rootOnly = true)

    override fun observePortfolio(): Flow<BittensorPortfolio> = observePortfolio(rootOnly = false)

    private fun observePortfolio(rootOnly: Boolean): Flow<BittensorPortfolio> = flowOfAll {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        accountRepository.selectedMetaAccountFlow()
            .flatMapLatest { account ->
                combine(
                    walletRepository.assetFlow(account.id, chain.utilityAsset),
                    observePositions(chain, account.requireAccountIdIn(chain), rootOnly)
                ) { asset, positionState ->
                    BittensorPortfolio(account.id, chain, asset, positionState.positions, positionState.status)
                }
            }
    }

    private fun observePositions(chain: Chain, coldKey: AccountId, rootOnly: Boolean): Flow<PositionState> = flow {
        // Keep cached positions within this collector and wallet.
        var cachedPositions: BittensorPortfolioPositions? = null
        emit(PositionState(cachedPositions, BittensorDataStatus.Loading))

        chainStateRepository.currentRemoteBlockNumberFlow(chain.id)
            .conflate()
            .collect {
                val positionState = loadPositions(chain, coldKey, rootOnly, cachedPositions)
                cachedPositions = positionState.positions
                emit(positionState)
            }
    }

    private suspend fun loadPositions(
        chain: Chain,
        coldKey: AccountId,
        rootOnly: Boolean,
        cachedPositions: BittensorPortfolioPositions?
    ): PositionState = try {
        val positions = portfolioLoader.load(chain.id, coldKey, rootOnly)
        PositionState(positions, BittensorDataStatus.Fresh)
    } catch (e: TimeoutCancellationException) {
        stalePositions(cachedPositions, e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        stalePositions(cachedPositions, e)
    }

    private fun stalePositions(positions: BittensorPortfolioPositions?, cause: Throwable): PositionState {
        logger.log("[BittensorPortfolio] Failed to refresh positions")
        logger.log(cause)
        return PositionState(positions, BittensorDataStatus.Stale(cause))
    }

    private data class PositionState(val positions: BittensorPortfolioPositions?, val status: BittensorDataStatus)
}
