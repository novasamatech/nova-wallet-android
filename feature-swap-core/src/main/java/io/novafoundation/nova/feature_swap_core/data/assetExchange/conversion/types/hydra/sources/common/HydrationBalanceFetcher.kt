package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.common

import io.novafoundation.nova.common.data.network.ext.transferableBalance
import io.novafoundation.nova.common.data.network.runtime.binding.AccountBalance
import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindOrmlAccountBalanceOrEmpty
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.domain.balance.TransferableMode
import io.novafoundation.nova.common.domain.balance.calculateTransferable
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.tokens
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core_api.data.primitive.SwapQuoting
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.typed.account
import io.novafoundation.nova.runtime.storage.typed.system
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Named

/**
 * This is a simplified version of [AssetBalanceSource] which we use here because usage of AssetBalanceSource
 * would create a circular dependency
 *
 * TODO fix this: balances should be extracted to a separate module to allow better reusability
 */
interface HydrationBalanceFetcher {

    suspend fun subscribeToTransferableBalance(
        chainId: ChainId,
        type: HydrationAssetType,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<BigInteger>
}

@FeatureScope
class HydrationBalanceFetcherFactory @Inject constructor(
    @Named(REMOTE_STORAGE_SOURCE)
    private val remoteStorageDataSource: StorageDataSource,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi
) {

    fun create(swapHost: SwapQuoting.QuotingHost): HydrationBalanceFetcher {
        return RealHydrationBalanceFetcher(remoteStorageDataSource, multiChainRuntimeCallsApi, swapHost)
    }
}

class RealHydrationBalanceFetcher(
    private val remoteStorageDataSource: StorageDataSource,
    private val runtimeCallsApi: MultiChainRuntimeCallsApi,
    private val swapHost: SwapQuoting.QuotingHost,
) : HydrationBalanceFetcher {

    override suspend fun subscribeToTransferableBalance(
        chainId: ChainId,
        type: HydrationAssetType,
        accountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<BigInteger> {
        return when (type) {
            is HydrationAssetType.Native -> subscribeNativeAssetBalance(chainId, accountId, subscriptionBuilder)
            is HydrationAssetType.Orml -> subscribeOrmlAssetBalance(chainId, type.assetId, accountId, subscriptionBuilder)
            is HydrationAssetType.Erc20 -> subscribeErc20AssetBalance(chainId, type.assetId, accountId)
        }
    }

    private suspend fun subscribeNativeAssetBalance(
        chainId: ChainId,
        poolAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<BigInteger> {
        return remoteStorageDataSource.subscribe(chainId, subscriptionBuilder) {
            metadata.system.account.observe(poolAccountId).map {
                val accountInfo = it ?: AccountInfo.empty()

                accountInfo.transferableBalance()
            }
        }
    }

    private suspend fun subscribeOrmlAssetBalance(
        chainId: ChainId,
        hydrationAssetId: HydraDxAssetId,
        poolAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<BigInteger> {
        return remoteStorageDataSource.subscribe(chainId, subscriptionBuilder) {
            metadata.tokens().storage("Accounts").observe(
                poolAccountId,
                hydrationAssetId,
                binding = ::bindOrmlAccountBalanceOrEmpty
            ).map {
                TransferableMode.REGULAR.calculateTransferable(it)
            }
        }
    }

    private suspend fun subscribeErc20AssetBalance(
        chainId: ChainId,
        hydrationAssetId: HydraDxAssetId,
        accountId: AccountId,
    ): Flow<BigInteger> {
        val blockNumberFlow = swapHost.sharedSubscriptions.blockNumber(chainId)

        return flow {
            val initialBalance = fetchBalance(chainId, hydrationAssetId, accountId)
            emit(initialBalance)

            blockNumberFlow.collect {
                val newBalance = fetchBalance(chainId, hydrationAssetId, accountId)
                emit(newBalance)
            }
        }
            .map { TransferableMode.REGULAR.calculateTransferable(it) }
            .distinctUntilChanged()
    }

    private suspend fun fetchBalance(chainId: ChainId, hydrationAssetId: HydraDxAssetId, accountId: AccountId): AccountBalance {
        return runtimeCallsApi.forChain(chainId).fetchBalance(hydrationAssetId, accountId)
    }

    private suspend fun RuntimeCallsApi.fetchBalance(hydrationAssetId: HydraDxAssetId, accountId: AccountId): AccountBalance {
        return call(
            section = "CurrenciesApi",
            method = "account",
            arguments = mapOf(
                "asset_id" to hydrationAssetId,
                "who" to accountId
            ),
            returnBinding = { bindAssetBalance(it) }
        )
    }

    private fun bindAssetBalance(decoded: Any?): AccountBalance {
        val asStruct = decoded.castToStruct()

        return AccountBalance(
            free = bindNumber(asStruct["free"]),
            frozen = bindNumber(asStruct["frozen"]),
            reserved = bindNumber(asStruct["reserved"]),
        )
    }
}
