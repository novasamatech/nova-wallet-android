@file:Suppress("EXPERIMENTAL_API_USAGE")

package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain

import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountInfo
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey

class WssSubstrateSource(
    private val remoteStorageSource: StorageDataSource,
) : SubstrateRemoteSource {

    override suspend fun getAccountInfo(
        chainId: ChainId,
        accountId: AccountId,
    ): AccountInfo {
        return remoteStorageSource.query(
            chainId = chainId,
            keyBuilder = {
                it.metadata.system().storage("Account").storageKey(it, accountId)
            },
            binding = { scale, runtime ->
                scale?.let { bindAccountInfo(it, runtime) } ?: AccountInfo.empty()
            }
        )
    }
}
