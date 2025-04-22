package io.novafoundation.nova.feature_account_api.domain.interfaces

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.utils.Filter
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountListingItem
import io.novafoundation.nova.feature_account_api.domain.model.ProxiedAndProxyMetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

interface MetaAccountGroupingInteractor {

    fun metaAccountsWithTotalBalanceFlow(): Flow<GroupedList<LightMetaAccount.Type, MetaAccountListingItem>>

    fun metaAccountWithTotalBalanceFlow(metaId: Long): Flow<MetaAccountListingItem>

    fun getMetaAccountsWithFilter(metaAccountFilter: Filter<MetaAccount>): Flow<GroupedList<LightMetaAccount.Type, MetaAccount>>

    fun updatedProxieds(): Flow<GroupedList<LightMetaAccount.Status, ProxiedAndProxyMetaAccount>>

    suspend fun hasAvailableMetaAccountsForChain(
        chainId: ChainId,
        metaAccountFilter: Filter<MetaAccount>
    ): Boolean
}
