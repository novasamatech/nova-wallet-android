package io.novafoundation.nova.feature_account_impl.data.sync

import com.google.gson.Gson
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.common.address.format.getAddressScheme
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.core_db.model.chain.account.DerivativeAccountTypeExtras
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.model.DerivativeMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_impl.data.derivative.DerivativeAccountRepository
import io.novafoundation.nova.feature_account_impl.data.sync.common.DelegatedAccountCreator
import io.novafoundation.nova.feature_account_impl.data.sync.common.DelegatedAccountCreator.DelegatedAccountAvailability
import io.novafoundation.nova.runtime.ext.addressScheme
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.findChains
import javax.inject.Inject

@FeatureScope
internal class DerivativeAccountsSyncDataSourceFactory @Inject constructor(
    private val repository: DerivativeAccountRepository,
    private val gson: Gson,
    private val chainRegistry: ChainRegistry,
    private val delegatedAccountCreator: DelegatedAccountCreator,
) : ExternalAccountsSyncDataSource.Factory {

    override suspend fun create(): ExternalAccountsSyncDataSource {
        val chainsWithDerivatives = chainRegistry.findChains(repository::areDerivativeAccountsSupported)

        return DerivativeAccountsSyncDataSource(repository, gson, delegatedAccountCreator, chainsWithDerivatives)
    }
}

private class DerivativeAccountsSyncDataSource(
    private val repository: DerivativeAccountRepository,
    private val gson: Gson,
    private val delegatedAccountCreator: DelegatedAccountCreator,
    private val derivativeChains: List<Chain>,
) : ExternalAccountsSyncDataSource {

    private val derivativeChainIds = derivativeChains.mapToSet { it.id }

    override fun supportedChains(): Collection<Chain> {
        return derivativeChains
    }

    override suspend fun isCreatedFromDataSource(metaAccount: MetaAccount): Boolean {
        return metaAccount is DerivativeMetaAccount
    }

    override suspend fun getExternalCreatedAccount(metaAccount: MetaAccount): ExternalSourceCreatedAccount? {
        return if (isCreatedFromDataSource(metaAccount)) {
            DerivativeExternalSourceAccount()
        } else {
            null
        }
    }

    override suspend fun getControllableExternalAccounts(accountIdsToQuery: Set<AccountIdKey>): List<ExternalControllableAccount> {
        if (derivativeChains.isEmpty()) return emptyList()

        return repository.getDerivativeAccounts(accountIdsToQuery).mapNotNull {
            DerivativeExternalControllableAccount(
                derivative = it.derivative,
                parent = it.parent,
                index = it.index,
                addressScheme = it.derivative.getAddressScheme() ?: return@mapNotNull null
            )
        }
    }

    private inner class DerivativeExternalControllableAccount(
        private val derivative: AccountIdKey,
        private val parent: AccountIdKey,
        private val index: Int,
        private val addressScheme: AddressScheme
    ) : ExternalControllableAccount {

        override val accountId: AccountIdKey
            get() = derivative

        override val controllerAccountId: AccountIdKey
            get() = parent


        override fun isRepresentedBy(localAccount: MetaAccount): Boolean {
            // Assuming accountId and controllerAccountId match, nothing else to check since both parent and index determine accountId
            return localAccount is DerivativeMetaAccount
        }

        override fun isAvailableOn(chain: Chain): Boolean {
            return chain.id in derivativeChainIds && chain.addressScheme == addressScheme
        }

        override suspend fun addControlledAccount(
            controller: MetaAccount,
            identity: Identity?,
            position: Int,
            missingAccountChain: Chain,
        ): AddAccountResult.AccountAdded {
            return delegatedAccountCreator.addDelegatedAccount(
                accountId = accountId,
                identity = identity,
                position = position,
                type = MetaAccountLocal.Type.DERIVATIVE,
                typeExtras = typeExtras(),
                availability = DelegatedAccountAvailability.DeriveFromController(
                    controller = controller,
                    chain = missingAccountChain
                )
            )
        }

        override fun dispatchChangesOriginFilters(): Boolean {
            // utility.as_derivative uses the same origin filters as the parent origin
            return false
        }


        private fun typeExtras(): String {
            val extras = DerivativeAccountTypeExtras(
                index = index,
                parentAccountId = parent
            )
            return gson.toJson(extras)
        }
    }

    private class DerivativeExternalSourceAccount : ExternalSourceCreatedAccount {

        override fun canControl(candidate: ExternalControllableAccount): Boolean {
            return true
        }
    }
}
