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

    override fun observeAsset(): Flow<Asset> = observeWallet().map { it.asset }

    override fun observeWallet(): Flow<BittensorWalletContext> = flowOfAll {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        accountRepository.selectedMetaAccountFlow().flatMapLatest { account ->
            walletRepository.assetFlow(account.id, chain.utilityAsset).map { asset -> BittensorWalletContext(account.id, chain, asset) }
        }
    }

    override fun observeRootPortfolio(): Flow<BittensorPortfolio> = observePortfolio(rootOnly = true)

    override fun observePortfolio(): Flow<BittensorPortfolio> = observePortfolio(rootOnly = false)

    private fun observePortfolio(rootOnly: Boolean): Flow<BittensorPortfolio> = flowOfAll {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        accountRepository.selectedMetaAccountFlow().flatMapLatest { account ->
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

        chainStateRepository.currentRemoteBlockNumberFlow(chain.id).conflate().collect {
            val status = try {
                cachedPositions = portfolioLoader.load(chain.id, coldKey, rootOnly)
                BittensorDataStatus.Fresh
            } catch (e: TimeoutCancellationException) {
                logRefreshFailure(e)
                BittensorDataStatus.Stale(e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logRefreshFailure(e)
                BittensorDataStatus.Stale(e)
            }
            emit(PositionState(cachedPositions, status))
        }
    }

    private data class PositionState(val positions: BittensorPortfolioPositions?, val status: BittensorDataStatus)

    private fun logRefreshFailure(cause: Throwable) {
        logger.log("[BittensorPortfolio] Failed to refresh positions")
        logger.log(cause)
    }
}
