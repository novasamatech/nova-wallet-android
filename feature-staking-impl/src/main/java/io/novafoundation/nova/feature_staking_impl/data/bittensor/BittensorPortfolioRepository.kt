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
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withTimeout

interface BittensorPortfolioRepository {

    fun observeAsset(): Flow<Asset>

    fun observePortfolio(): Flow<BittensorPortfolio>

    fun refresh()
}

class RealBittensorPortfolioRepository(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val positionSource: BittensorPositionSource,
    private val swapSource: BittensorSwapSource,
    private val positionUpdates: BittensorPositionUpdates,
    private val logger: Logger
) : BittensorPortfolioRepository {

    override fun observeAsset(): Flow<Asset> = flowOfAll {
        val chain = withTimeout(20_000L) { chainRegistry.getChain(ChainGeneses.BITTENSOR) }
        accountRepository.selectedMetaAccountFlow().flatMapLatest { account ->
            walletRepository.assetFlow(account.id, chain.utilityAsset)
        }
    }

    override fun observePortfolio(): Flow<BittensorPortfolio> = flowOfAll {
        val chain = withTimeout(20_000L) { chainRegistry.getChain(ChainGeneses.BITTENSOR) }
        accountRepository.selectedMetaAccountFlow().flatMapLatest { account ->
            // The cache belongs to this wallet's collector, never to the selected wallet globally.
            combine(
                walletRepository.assetFlow(account.id, chain.utilityAsset),
                flow {
                    var cached: BittensorPortfolioPositions? = null
                    emit(cached to BittensorDataStatus.Loading)
                    val periodic = flow {
                        while (true) {
                            delay(30_000L)
                            emit(Unit)
                        }
                    }
                    merge(positionUpdates.updates.map { Unit }, periodic).conflate().collect {
                        val state = try {
                            cached = withTimeout(20_000L) {
                                fetchPositions(chain.id, account.requireAccountIdIn(chain))
                            }
                            BittensorDataStatus.Fresh
                        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                            logRefreshFailure(e)
                            BittensorDataStatus.Stale(e)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logRefreshFailure(e)
                            BittensorDataStatus.Stale(e)
                        }
                        emit(cached to state)
                    }
                }
            ) { asset, (positions, status) ->
                BittensorPortfolio(account.id, chain, asset, positions, status)
            }
        }
    }

    override fun refresh() = positionUpdates.refresh()

    private suspend fun fetchPositions(chainId: String, coldkey: ByteArray): BittensorPortfolioPositions = coroutineScope {
        val root = async { positionSource.rootPositions(chainId, coldkey) }
        val subnets = positionSource.subnetPositions(chainId, coldkey)
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
