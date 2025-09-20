package io.novafoundation.nova.feature_account_impl.data.sync.common

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.format.AddressFormat
import io.novafoundation.nova.common.address.format.addressOf
import io.novafoundation.nova.common.address.format.getAddressSchemeOrThrow
import io.novafoundation.nova.common.address.format.isEvm
import io.novafoundation.nova.common.address.format.isSubstrate
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_impl.data.mappers.AccountMappers
import io.novafoundation.nova.feature_account_impl.data.sync.common.DelegatedAccountCreator.DelegatedAccountAvailability.DeriveFromController
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

interface DelegatedAccountCreator {

    sealed interface DelegatedAccountAvailability {

        class DeriveFromController(val controller: MetaAccount, val chain: Chain) : DelegatedAccountAvailability
    }

    suspend fun addDelegatedAccount(
        accountId: AccountIdKey,
        identity: Identity?,
        position: Int,
        type: MetaAccountLocal.Type,
        typeExtras: String,
        availability: DelegatedAccountAvailability,
    ): AddAccountResult.AccountAdded
}

@FeatureScope
class RealDelegatedAccountCreator @Inject constructor(
    private val accountDao: MetaAccountDao,
    private val accountMappers: AccountMappers,
) : DelegatedAccountCreator {

    override suspend fun addDelegatedAccount(
        accountId: AccountIdKey,
        identity: Identity?,
        position: Int,
        type: MetaAccountLocal.Type,
        typeExtras: String,
        availability: DelegatedAccountCreator.DelegatedAccountAvailability
    ): AddAccountResult.AccountAdded {
        val newId = when (availability) {
            is DeriveFromController -> addDelegatedAccountFromController(
                accountId = accountId,
                identity = identity,
                position = position,
                type = type,
                typeExtras = typeExtras,
                chain = availability.chain,
                controller = availability.controller
            )
        }

        return AddAccountResult.AccountAdded(newId, accountMappers.mapMetaAccountTypeFromLocal(type))
    }

    private suspend fun addDelegatedAccountFromController(
        accountId: AccountIdKey,
        identity: Identity?,
        position: Int,
        type: MetaAccountLocal.Type,
        typeExtras: String,
        chain: Chain,
        controller: MetaAccount
    ): Long {
        return when (controller.type) {
            LightMetaAccount.Type.SECRETS,
            LightMetaAccount.Type.WATCH_ONLY -> addForComplexSigner(accountId, controller, identity, position, type, typeExtras, chain)

            LightMetaAccount.Type.PARITY_SIGNER,
            LightMetaAccount.Type.POLKADOT_VAULT -> addUniversalAccount(accountId, controller, identity, position, type, typeExtras)

            LightMetaAccount.Type.LEDGER_LEGACY,
            LightMetaAccount.Type.LEDGER -> addSingleChainAccount(accountId, controller, identity, position, type, typeExtras, chain)

            LightMetaAccount.Type.PROXIED -> addSingleChainAccount(accountId, controller, identity, position, type, typeExtras, chain)

            LightMetaAccount.Type.MULTISIG -> addForComplexSigner(accountId, controller, identity, position, type, typeExtras, chain)

            LightMetaAccount.Type.DERIVATIVE -> addForComplexSigner(accountId, controller, identity, position, type, typeExtras, chain)
        }
    }

    private suspend fun addForComplexSigner(
        accountId: AccountIdKey,
        controller: MetaAccount,
        identity: Identity?,
        position: Int,
        type: MetaAccountLocal.Type,
        typeExtras: String,
        chain: Chain,
    ): Long {
        return if (controller.chainAccounts.isEmpty()) {
            addUniversalAccount(accountId, controller, identity, position, type, typeExtras)
        } else {
            addSingleChainAccount(accountId, controller, identity, position, type, typeExtras, chain)
        }
    }

    private suspend fun addSingleChainAccount(
        accountId: AccountIdKey,
        controller: MetaAccount,
        identity: Identity?,
        position: Int,
        type: MetaAccountLocal.Type,
        typeExtras: String,
        chain: Chain
    ): Long {
        val metaAccount = createSingleChainMetaAccount(accountId, controller.id, identity, position, type, typeExtras)
        return accountDao.insertMetaAndChainAccounts(metaAccount) { newId ->
            listOf(createChainAccount(newId, accountId, chain))
        }
    }

    private suspend fun addUniversalAccount(
        accountId: AccountIdKey,
        controller: MetaAccount,
        identity: Identity?,
        position: Int,
        type: MetaAccountLocal.Type,
        typeExtras: String
    ): Long {
        val metaAccount = createUniversalMetaAccount(accountId, controller.id, identity, position, type, typeExtras)
        return accountDao.insertMetaAccount(metaAccount)
    }

    private fun createUniversalMetaAccount(
        accountId: AccountIdKey,
        controllerMetaId: Long,
        identity: Identity?,
        position: Int,
        type: MetaAccountLocal.Type,
        typeExtras: String,
    ): MetaAccountLocal {
        return MetaAccountLocal(
            substratePublicKey = null,
            substrateCryptoType = null,
            substrateAccountId = substrateAccountId(accountId),
            ethereumPublicKey = null,
            ethereumAddress = ethereumAddress(accountId),
            name = accountName(identity, accountId),
            parentMetaId = controllerMetaId,
            isSelected = false,
            position = position,
            type = type,
            status = MetaAccountLocal.Status.ACTIVE,
            globallyUniqueId = MetaAccountLocal.generateGloballyUniqueId(),
            typeExtras = typeExtras
        )
    }

    private fun createSingleChainMetaAccount(
        accountId: AccountIdKey,
        controllerMetaId: Long,
        identity: Identity?,
        position: Int,
        type: MetaAccountLocal.Type,
        typeExtras: String
    ): MetaAccountLocal {
        return MetaAccountLocal(
            substratePublicKey = null,
            substrateCryptoType = null,
            substrateAccountId = null,
            ethereumPublicKey = null,
            ethereumAddress = null,
            name = accountName(identity, accountId),
            parentMetaId = controllerMetaId,
            isSelected = false,
            position = position,
            type = type,
            status = MetaAccountLocal.Status.ACTIVE,
            globallyUniqueId = MetaAccountLocal.generateGloballyUniqueId(),
            typeExtras = typeExtras
        )
    }

    private fun createChainAccount(
        selfMetaId: Long,
        accountId: AccountIdKey,
        chain: Chain
    ): ChainAccountLocal {
        return ChainAccountLocal(
            metaId = selfMetaId,
            chainId = chain.id,
            publicKey = null,
            accountId = accountId.value,
            cryptoType = null
        )
    }

    private fun ethereumAddress(accountId: AccountIdKey): ByteArray? {
        return accountId.value.takeIf { accountId.getAddressSchemeOrThrow().isEvm() }
    }

    private fun substrateAccountId(accountId: AccountIdKey): ByteArray? {
        return accountId.value.takeIf { accountId.getAddressSchemeOrThrow().isSubstrate() }
    }

    private fun accountName(identity: Identity?, accountId: AccountIdKey): String {
        if (identity != null) return identity.name

        val addressFormat = AddressFormat.defaultForScheme(accountId.getAddressSchemeOrThrow())
        return addressFormat.addressOf(accountId).value
    }
}
