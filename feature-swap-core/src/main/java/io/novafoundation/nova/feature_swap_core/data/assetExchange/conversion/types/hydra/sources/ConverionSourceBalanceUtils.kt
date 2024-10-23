package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources

import io.novafoundation.nova.common.data.network.ext.transferableBalance
import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.common.data.network.runtime.binding.bindOrmlAccountBalanceOrEmpty
import io.novafoundation.nova.common.domain.balance.TransferableMode
import io.novafoundation.nova.common.domain.balance.calculateTransferable
import io.novafoundation.nova.common.utils.tokens
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.runtime.ext.ormlCurrencyId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novafoundation.nova.runtime.storage.typed.account
import io.novafoundation.nova.runtime.storage.typed.system
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

suspend fun StorageDataSource.subscribeToTransferableBalance(
    chainAsset: Chain.Asset,
    poolAccountId: AccountId,
    subscriptionBuilder: SharedRequestsBuilder
): Flow<BigInteger> {
    return when (chainAsset.type) {
        is Chain.Asset.Type.Native -> subscribeNativeAssetBalance(chainAsset, poolAccountId, subscriptionBuilder)
        is Chain.Asset.Type.Orml -> subscribeOrmlAssetBalance(chainAsset, poolAccountId, subscriptionBuilder)
        else -> throw IllegalArgumentException("Unsupported asset type: ${chainAsset.type}")
    }
}

/**
 * Code duplication from [NativeAssetBalance]
 */
private suspend fun StorageDataSource.subscribeNativeAssetBalance(
    chainAsset: Chain.Asset,
    poolAccountId: AccountId,
    subscriptionBuilder: SharedRequestsBuilder
): Flow<BigInteger> {
    return subscribe(chainAsset.chainId, subscriptionBuilder) {
        metadata.system.account.observe(poolAccountId).map {
            val accountInfo = it ?: AccountInfo.empty()

            accountInfo.transferableBalance()
        }
    }
}

/**
 * Code duplication from [OrmlAssetBalance]
 */
private suspend fun StorageDataSource.subscribeOrmlAssetBalance(
    chainAsset: Chain.Asset,
    poolAccountId: AccountId,
    subscriptionBuilder: SharedRequestsBuilder
): Flow<BigInteger> {
    return subscribe(chainAsset.chainId, subscriptionBuilder) {
        metadata.tokens().storage("Accounts").observe(
            poolAccountId,
            chainAsset.ormlCurrencyId(runtime),
            binding = ::bindOrmlAccountBalanceOrEmpty
        ).map {
            TransferableMode.REGULAR.calculateTransferable(it)
        }
    }
}
