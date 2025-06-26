@file:Suppress("EXPERIMENTAL_API_USAGE")

package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain

import io.novafoundation.nova.common.data.network.runtime.binding.AccountInfo
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.feature_wallet_api.data.repository.AccountInfoRepository
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.queryNonNull
import io.novafoundation.nova.runtime.storage.typed.account
import io.novafoundation.nova.runtime.storage.typed.system
import io.novasama.substrate_sdk_android.runtime.AccountId
import javax.inject.Inject
import javax.inject.Named

@FeatureScope
internal class RealAccountInfoRepository @Inject constructor(
    @Named(REMOTE_STORAGE_SOURCE) private val remoteStorageSource: StorageDataSource,
) : AccountInfoRepository {

    override suspend fun getAccountInfo(
        chainId: ChainId,
        accountId: AccountId,
    ): AccountInfo {
        return remoteStorageSource.query(chainId, applyStorageDefault = true) {
            metadata.system.account.queryNonNull(accountId)
        }
    }
}
