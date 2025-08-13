package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.aave

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.Weights
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.aave.model.AavePool
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.aave.model.AavePools
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.RemoteAndLocalId
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.matchId
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.primitive.SwapQuoting
import io.novafoundation.nova.feature_swap_core_api.data.primitive.errors.SwapQuoteException
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuotingSource
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigInteger
import javax.inject.Inject

@FeatureScope
class AaveSwapQuotingSourceFactory @Inject constructor(
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
) : HydraDxQuotingSource.Factory<AavePoolQuotingSource> {

    companion object {

        const val ID = "Aave"
    }

    override fun create(chain: Chain, host: SwapQuoting.QuotingHost): AavePoolQuotingSource {
        return RealAaveSwapQuotingSource(
            hydraDxAssetIdConverter = hydraDxAssetIdConverter,
            multiChainRuntimeCallsApi = multiChainRuntimeCallsApi,
            chain = chain,
            host = host
        )
    }
}

private class RealAaveSwapQuotingSource(
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    private val chain: Chain,
    private val host: SwapQuoting.QuotingHost,
) : AavePoolQuotingSource {

    override val identifier: String = AaveSwapQuotingSourceFactory.ID

    private val initialPoolsInfo: MutableSharedFlow<Collection<AavePoolInitialInfo>> = singleReplaySharedFlow()

    private val aavePools: MutableSharedFlow<AavePools> = singleReplaySharedFlow()

    override suspend fun sync() {
        val pairs = getPairs()

        val poolInitialInfo = pairs.matchIdsWithLocal()
        initialPoolsInfo.emit(poolInitialInfo)
    }

    override suspend fun availableSwapDirections(): Collection<AavePoolQuotingSource.Edge> {
        val poolInitialInfo = initialPoolsInfo.first()

        return poolInitialInfo.allPossibleDirections()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun runSubscriptions(
        userAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Unit> = coroutineScope {
        aavePools.resetReplayCache()

        host.sharedSubscriptions.blockNumber(chain.id).map {
            val pools = getPools()
            aavePools.emit(pools)
        }
    }

    private suspend fun getPairs(): List<AavePoolPair> {
        return runCatching {
            multiChainRuntimeCallsApi.forChain(chain.id).call(
                section = "AaveTradeExecutor",
                method = "pairs",
                arguments = emptyMap(),
                returnBinding = ::bindPairs
            )
        }.onFailure { Log.d(LOG_TAG, "Failed to get aave pairs", it) }
            .getOrDefault(emptyList())
    }

    private suspend fun getPools(): AavePools {
        return multiChainRuntimeCallsApi.forChain(chain.id).call(
            section = "AaveTradeExecutor",
            method = "pools",
            arguments = emptyMap(),
            returnBinding = ::bindPools
        )
    }

    private suspend fun List<AavePoolPair>.matchIdsWithLocal(): List<AavePoolInitialInfo> {
        val allOnChainIds = hydraDxAssetIdConverter.allOnChainIds(chain)

        return mapNotNull { poolInfo ->
            AavePoolInitialInfo(
                firstAsset = allOnChainIds.matchId(poolInfo.firstAsset) ?: return@mapNotNull null,
                secondAsset = allOnChainIds.matchId(poolInfo.secondAsset) ?: return@mapNotNull null,
            )
        }
    }

    private fun Collection<AavePoolInitialInfo>.allPossibleDirections(): Collection<RealXYKSwapQuotingEdge> {
        return buildList {
            this@allPossibleDirections.forEach { poolInfo ->
                add(RealXYKSwapQuotingEdge(fromAsset = poolInfo.firstAsset, toAsset = poolInfo.secondAsset))
                add(RealXYKSwapQuotingEdge(fromAsset = poolInfo.secondAsset, toAsset = poolInfo.firstAsset))
            }
        }
    }

    inner class RealXYKSwapQuotingEdge(
        override val fromAsset: RemoteAndLocalId,
        override val toAsset: RemoteAndLocalId,
    ) : AavePoolQuotingSource.Edge {

        override val from: FullChainAssetId = fromAsset.second

        override val to: FullChainAssetId = toAsset.second

        override val weight: Int
            get() = Weights.Hydra.AAVE

        override suspend fun quote(amount: BigInteger, direction: SwapDirection): BigInteger {
            val allPools = aavePools.first()

            return allPools.quote(fromAsset.first, toAsset.first, amount, direction)
                ?: throw SwapQuoteException.NotEnoughLiquidity
        }
    }

    private fun bindPairs(decoded: Any?): List<AavePoolPair> {
        return bindList(decoded) { item ->
            val (first, second) = item.castToList()
            AavePoolPair(bindNumber(first), bindNumber(second))
        }
    }

    private fun bindPools(decoded: Any?): AavePools {
        val pools = bindList(decoded, ::bindPool)
        return AavePools(pools)
    }

    private fun bindPool(decoded: Any?): AavePool {
        val asStruct = decoded.castToStruct()

        return AavePool(
            reserve = bindNumber(asStruct["reserve"]),
            atoken = bindNumber(asStruct["atoken"]),
            liqudityIn = bindNumber(asStruct["liqudityIn"]),
            liquidityOut = bindNumber(asStruct["liqudityOut"])
        )
    }
}

private class AavePoolPair(val firstAsset: HydraDxAssetId, val secondAsset: HydraDxAssetId)

private class AavePoolInitialInfo(
    val firstAsset: RemoteAndLocalId,
    val secondAsset: RemoteAndLocalId
)
