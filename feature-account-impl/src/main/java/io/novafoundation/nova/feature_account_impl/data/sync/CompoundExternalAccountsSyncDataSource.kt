package io.novafoundation.nova.feature_account_impl.data.sync

import android.util.Log
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.flatMapAsync
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novasama.substrate_sdk_android.extensions.tryFindNonNull

internal class CompoundExternalAccountsSyncDataSource(
    private val delegates: List<ExternalAccountsSyncDataSource>
) : ExternalAccountsSyncDataSource {

    override suspend fun isCreatedFromDataSource(metaAccount: MetaAccount): Boolean {
        return delegates.any { it.isCreatedFromDataSource(metaAccount) }
    }

    override suspend fun getExternalCreatedAccount(metaAccount: MetaAccount): ExternalSourceCreatedAccount? {
        return delegates.tryFindNonNull { it.getExternalCreatedAccount(metaAccount) }
    }

    override suspend fun getControllableExternalAccounts(
        accountIdsToQuery: Set<AccountIdKey>,
    ): List<ExternalControllableAccount> {
        return delegates.flatMapAsync {
            val label = it::class.simpleName

            Log.d("ExternalAccountsDiscovery", "Started fetching ${accountIdsToQuery.size} accounts using $label")

            try {
                it.getControllableExternalAccounts(accountIdsToQuery).also { result ->
                    Log.d("ExternalAccountsDiscovery", "Finished fetching accounts using $label. Got ${result.size} accounts")
                }
            } catch (e: Throwable) {
                Log.e("ExternalAccountsDiscovery", "Failed to fetch accounts using $label", e)
                throw e
            }
        }
    }
}
