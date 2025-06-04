package io.novafoundation.nova.feature_account_impl.data.sync

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface ExternalAccountsSyncDataSource {

    suspend fun isCreatedFromDataSource(metaAccount: MetaAccount): Boolean

    suspend fun getControllableExternalAccounts(
        accountIdsToQuery: Set<AccountIdKey>,
        chain: Chain
    ): List<ExternalControllableAccount>
}
