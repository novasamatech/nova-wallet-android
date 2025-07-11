package io.novafoundation.nova.feature_account_impl.data.sync

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

internal interface ExternalAccountsSyncDataSource {

    interface Factory {

        suspend fun create(): ExternalAccountsSyncDataSource
    }

    fun supportedChains(): Collection<Chain>

    suspend fun isCreatedFromDataSource(metaAccount: MetaAccount): Boolean

    suspend fun getExternalCreatedAccount(metaAccount: MetaAccount): ExternalSourceCreatedAccount?

    suspend fun getControllableExternalAccounts(accountIdsToQuery: Set<AccountIdKey>): List<ExternalControllableAccount>
}

internal interface ExternalControllableAccount {

    val accountId: AccountIdKey

    val controllerAccountId: AccountIdKey

    /**
     * Check whether [localAccount] represents self in the data-base
     * Implementation can assume that [accountId] and [controllerAccountId] check has already been done
     */
    fun isRepresentedBy(localAccount: MetaAccount): Boolean

    fun isAvailableOn(chain: Chain): Boolean

    /**
     * Add account to the data-base, WITHOUT notifying any external entities,
     * like [MetaAccountChangesEventBus] - this is expected to be done by the calling code
     *
     * @return id of newly created account
     */
    suspend fun addControlledAccount(
        controller: MetaAccount,
        identity: Identity?,
        position: Int,
        missingAccountChain: Chain
    ): AddAccountResult.AccountAdded

    /**
     * Whether dispatching call on behalf of this account changes the original call filters
     *
     * This might be used by certain data-sources to understand whether control of such account is actually possible
     */
    fun dispatchChangesOriginFilters(): Boolean

    sealed class Availability {

        data class Universal(val addressScheme: AddressScheme) : Availability()

        data class SingleChain(val chainId: ChainId) : Availability()
    }
}

internal interface ExternalSourceCreatedAccount {

    fun canControl(candidate: ExternalControllableAccount): Boolean
}

internal fun ExternalControllableAccount.address(chain: Chain): String {
    return chain.addressOf(accountId)
}

internal fun ExternalControllableAccount.controllerAddress(chain: Chain): String {
    return chain.addressOf(controllerAccountId)
}
