package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolio
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout

interface BittensorPortfolioRepository {

    fun observeAsset(): Flow<Asset>

    fun observePortfolio(): Flow<BittensorPortfolio>
}

class RealBittensorPortfolioRepository(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val positionSource: BittensorPositionSource,
    private val swapSource: BittensorSwapSource,
    private val chainStateRepository: ChainStateRepository,
    private val logger: Logger
) : BittensorPortfolioRepository {

    override fun observeAsset(): Flow<Asset> = flowOfAll {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        accountRepository.selectedMetaAccountFlow().flatMapLatest { account ->
            walletRepository.assetFlow(account.id, chain.utilityAsset)
        }
    }

    override fun observePortfolio(): Flow<BittensorPortfolio> = flowOfAll {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        accountRepository.selectedMetaAccountFlow().flatMapLatest { account ->
            combine(
                walletRepository.assetFlow(account.id, chain.utilityAsset),
                observePositions(chain, account.requireAccountIdIn(chain))
            ) { asset, (positions, status) ->
                BittensorPortfolio(account.id, chain, asset, positions, status)
            }
        }
    }

    private fun observePositions(chain: Chain, coldKey: AccountId): Flow<Pair<BittensorPortfolioPositions?, BittensorDataStatus>> = flow {
        // Keep cached positions within this collector and wallet.
        var cachedPositions: BittensorPortfolioPositions? = null
        emit(cachedPositions to BittensorDataStatus.Loading)

        chainStateRepository.currentRemoteBlockNumberFlow(chain.id).conflate().collect {
            val status = try {
                cachedPositions = withTimeout(20_000L) { fetchPositions(chain.id, coldKey) }
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
            emit(cachedPositions to status)
        }
    }

    private suspend fun fetchPositions(chainId: String, coldKey: AccountId): BittensorPortfolioPositions = coroutineScope {
        val root = async { positionSource.rootPositions(chainId, coldKey) }
        val subnets = positionSource.subnetPositions(chainId, coldKey)
        val prices = subnets.map { it.subnetId }.distinct().mapNotNull { subnetId ->
            try {
                subnetId to swapSource.currentAlphaPrice(chainId, subnetId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.log("[BittensorPortfolio] Failed to load alpha price for subnet ${subnetId.value}")
                logger.log(e)
                null
            }
        }.toMap()
        BittensorPortfolioPositions(root.await(), subnets, prices)
    }

    private fun logRefreshFailure(cause: Throwable) {
        logger.log("[BittensorPortfolio] Failed to refresh positions")
        logger.log(cause)
    }
}
