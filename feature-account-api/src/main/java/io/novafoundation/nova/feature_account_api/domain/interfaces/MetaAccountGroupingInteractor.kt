package io.novafoundation.nova.feature_account_api.domain.interfaces

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountWithTotalBalance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

interface MetaAccountGroupingInteractor {

    fun metaAccountsWithTotalBalanceFlow(): Flow<GroupedList<LightMetaAccount.Type, MetaAccountWithTotalBalance>>

    fun getControlledMetaAccountsFlow(): Flow<GroupedList<LightMetaAccount.Type, MetaAccount>>

    suspend fun hasAvailableMetaAccountsForDestination(chainId: ChainId): Boolean
}
