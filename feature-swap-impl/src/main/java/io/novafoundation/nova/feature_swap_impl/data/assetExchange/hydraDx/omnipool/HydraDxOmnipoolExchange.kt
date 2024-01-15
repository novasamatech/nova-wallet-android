package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrNull
import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.common.utils.filterToSet
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.mapNotNullToSet
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.common.utils.padEnd
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.withFlowScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchangeFee
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchangeQuote
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.OmniPool
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.OmniPoolToken
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.OmniPoolTokenId
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model.OmnipoolAssetState
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ext.decodeOrNull
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.ormlOrNull
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.metadata
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.runningFold
import java.math.BigInteger

private val SYSTEM_ON_CHAIN_ASSET_ID = BigInteger.ZERO

class HydraDxOmnipoolExchangeFactory(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
) : AssetExchange.Factory {

    override suspend fun create(chain: Chain, coroutineScope: CoroutineScope): AssetExchange {
        return HydraDxOmnipoolExchange(remoteStorageSource, chainRegistry, chain)
    }
}

private class HydraDxOmnipoolExchange(
    private val remoteStorageSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
    private val chain: Chain,
    private val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    private val assetSourceRegistry: AssetSourceRegistry,
) : AssetExchange {

    private val pooledOnChainAssetIdsState: MutableSharedFlow<List<RemoteAndLocalId>> = singleReplaySharedFlow()

    private val omniPool: MutableSharedFlow<OmniPool> = singleReplaySharedFlow()

    override suspend fun canPayFeeInNonUtilityToken(asset: Chain.Asset): Boolean {
        // TODO HydraDx supports custom token fee payment
        return false
    }

    override suspend fun availableSwapDirections(): MultiMap<FullChainAssetId, FullChainAssetId> {
        val pooledOnChainAssetIds = getPooledOnChainAssetIds()

        val pooledChainAssetsIds = matchKnownChainAssetIds(pooledOnChainAssetIds)
        pooledOnChainAssetIdsState.emit(pooledChainAssetsIds)

        return pooledChainAssetsIds.associateBy(
            keySelector = { it.second },
            valueTransform = { (_, currentId) ->
                // In OmniPool, each asset is tradable with any other except itself
                pooledChainAssetsIds.mapNotNullToSet { (_, otherId) -> otherId.takeIf { currentId != otherId } }
            }
        )
    }

    override suspend fun quote(args: SwapQuoteArgs): AssetExchangeQuote {
        TODO("Not yet implemented")
    }

    override suspend fun estimateFee(args: SwapExecuteArgs): AssetExchangeFee {
        TODO("Not yet implemented")
    }

    override suspend fun swap(args: SwapExecuteArgs): Result<ExtrinsicSubmission> {
        TODO("Not yet implemented")
    }

    override suspend fun slippageConfig(): SlippageConfig {
        return SlippageConfig.default()
    }

    override fun runSubscriptions(chain: Chain): Flow<ReQuoteTrigger> {
        return withFlowScope { scope ->
            val pooledAssets = pooledOnChainAssetIdsState.first()
            val subscriptionBuilder = storageSharedRequestsBuilderFactory.create(chain.id)

            val omniPoolState = pooledAssets.map { (onChainId, chainAssetId) ->
                remoteStorageSource.subscribe(chain.id, subscriptionBuilder) {
                    metadata.omnipool.assets.observeNonNull(onChainId)
                }
            }
                .mergeIfMultiple()
                .runningFold(emptyMap<OmniPoolTokenId, OmnipoolAssetState>()) { accumulator, tokenState ->
                    val newRecord = tokenState.tokenId to tokenState
                    accumulator + newRecord
                }
                .filter { it.size == pooledAssets.size }

            val poolAccountId = poolAccountId()

            val omniPoolBalances = pooledAssets.map { (_, chainAssetId) ->
                val chainAsset = chain.assetsById.getValue(chainAssetId.assetId)
                val assetSource = assetSourceRegistry.sourceFor(chainAsset)
                assetSource.balance.subscribeAccountBalance(chain, chainAsset, poolAccountId, subscriptionBuilder)
            }

            subscriptionBuilder.subscribe(scope)
        }
    }

    private fun poolAccountId(): AccountId {
        return "modlomnipool".encodeToByteArray().padEnd(expectedSize = 32, padding = 0)
    }

    private suspend fun getPooledOnChainAssetIds(): List<BigInteger> {
        return remoteStorageSource.query(chain.id) {
            runtime.metadata.omnipoolOrNull?.assets?.keys().orEmpty()
        }
    }

    private suspend fun matchKnownChainAssetIds(onChainIds: List<OmniPoolTokenId>): List<RemoteAndLocalId> {
        val runtime = chainRegistry.getRuntime(chain.id)
        val ormlIds = chain.assets.mapNotNull {

            val onChainId = bindNumberOrNull(it.ormlOrNull()?.decodeOrNull(runtime)) ?: return@mapNotNull null

            onChainId to it
        }.toMap()

        return onChainIds.mapNotNull { onChainId ->
            val id = if (onChainId == SYSTEM_ON_CHAIN_ASSET_ID) {
                chain.utilityAsset.fullId
            } else {
                ormlIds[onChainId]?.fullId
            } ?: return@mapNotNull null

            onChainId to id
        }
    }
}

private typealias RemoteAndLocalId = Pair<OmniPoolTokenId, FullChainAssetId>
