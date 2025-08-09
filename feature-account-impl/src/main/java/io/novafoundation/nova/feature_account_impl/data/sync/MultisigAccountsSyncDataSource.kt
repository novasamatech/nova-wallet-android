package io.novafoundation.nova.feature_account_impl.data.sync

import com.google.gson.Gson
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.format.AddressFormat
import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.common.address.format.addressOf
import io.novafoundation.nova.common.address.format.getAddressScheme
import io.novafoundation.nova.common.address.format.isEvm
import io.novafoundation.nova.common.address.format.isSubstrate
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MultisigTypeExtras
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_impl.data.multisig.MultisigRepository
import io.novafoundation.nova.feature_account_impl.data.multisig.model.otherSignatories
import io.novafoundation.nova.runtime.ext.addressScheme
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.findChains
import javax.inject.Inject

@FeatureScope
internal class MultisigAccountsSyncDataSourceFactory @Inject constructor(
    private val multisigRepository: MultisigRepository,
    private val gson: Gson,
    private val accountDao: MetaAccountDao,
    private val chainRegistry: ChainRegistry
) : ExternalAccountsSyncDataSource.Factory {

    override suspend fun create(): ExternalAccountsSyncDataSource {
        val chainsWithMultisigs = chainRegistry.findChains(multisigRepository::supportsMultisigSync)

        return MultisigAccountsSyncDataSource(multisigRepository, gson, accountDao, chainsWithMultisigs)
    }
}

private class MultisigAccountsSyncDataSource(
    private val multisigRepository: MultisigRepository,
    private val gson: Gson,
    private val accountDao: MetaAccountDao,
    private val multisigChains: List<Chain>,
) : ExternalAccountsSyncDataSource {

    private val multisigChainIds = multisigChains.mapToSet { it.id }

    override fun supportedChains(): Collection<Chain> {
        return multisigChains
    }

    override suspend fun isCreatedFromDataSource(metaAccount: MetaAccount): Boolean {
        return metaAccount is MultisigMetaAccount
    }

    override suspend fun getExternalCreatedAccount(metaAccount: MetaAccount): ExternalSourceCreatedAccount? {
        return if (isCreatedFromDataSource(metaAccount)) {
            MultisigExternalSourceAccount()
        } else {
            null
        }
    }

    override suspend fun getControllableExternalAccounts(accountIdsToQuery: Set<AccountIdKey>): List<ExternalControllableAccount> {
        if (multisigChains.isEmpty()) return emptyList()

        return multisigRepository.findMultisigAccounts(accountIdsToQuery)
            .flatMap { discoveredMultisig ->
                discoveredMultisig.allSignatories
                    .filter { it in accountIdsToQuery }
                    .mapNotNull { ourSignatory ->
                        MultisigExternalControllableAccount(
                            accountId = discoveredMultisig.accountId,
                            controllerAccountId = ourSignatory,
                            threshold = discoveredMultisig.threshold,
                            otherSignatories = discoveredMultisig.otherSignatories(ourSignatory),
                            addressScheme = discoveredMultisig.accountId.getAddressScheme() ?: return@mapNotNull null
                        )
                    }
            }
    }

    private inner class MultisigExternalControllableAccount(
        override val accountId: AccountIdKey,
        override val controllerAccountId: AccountIdKey,
        private val threshold: Int,
        private val otherSignatories: List<AccountIdKey>,
        private val addressScheme: AddressScheme
    ) : ExternalControllableAccount {

        override fun isRepresentedBy(localAccount: MetaAccount): Boolean {
            // Assuming accountId and controllerAccountId match, nothing else to check since both threshold and signers determine accountId
            return localAccount is MultisigMetaAccount
        }

        override fun isAvailableOn(chain: Chain): Boolean {
            return chain.id in multisigChainIds && chain.addressScheme == addressScheme
        }

        override suspend fun addControlledAccount(
            controller: MetaAccount,
            identity: Identity?,
            position: Int,
            missingAccountChain: Chain,
        ): AddAccountResult.AccountAdded {
            val newId = addMultisig(controller, identity, position, missingAccountChain)
            return AddAccountResult.AccountAdded(newId, LightMetaAccount.Type.MULTISIG)
        }

        override fun dispatchChangesOriginFilters(): Boolean {
            return true
        }

        private suspend fun addMultisig(
            controller: MetaAccount,
            identity: Identity?,
            position: Int,
            chain: Chain
        ): Long {
            return when (controller.type) {
                LightMetaAccount.Type.SECRETS,
                LightMetaAccount.Type.WATCH_ONLY -> addMultisigForComplexSigner(controller, identity, position, chain)

                LightMetaAccount.Type.PARITY_SIGNER,
                LightMetaAccount.Type.POLKADOT_VAULT -> addUniversalMultisig(controller, identity, position)

                LightMetaAccount.Type.LEDGER_LEGACY,
                LightMetaAccount.Type.LEDGER -> addSingleChainMultisig(controller, identity, position, chain)

                LightMetaAccount.Type.PROXIED -> addSingleChainMultisig(controller, identity, position, chain)

                LightMetaAccount.Type.MULTISIG -> addMultisigForComplexSigner(controller, identity, position, chain)
            }
        }

        private suspend fun addMultisigForComplexSigner(
            controller: MetaAccount,
            identity: Identity?,
            position: Int,
            chain: Chain
        ): Long {
            return if (controller.chainAccounts.isEmpty()) {
                addUniversalMultisig(controller, identity, position)
            } else {
                addSingleChainMultisig(controller, identity, position, chain)
            }
        }

        private suspend fun addSingleChainMultisig(
            controller: MetaAccount,
            identity: Identity?,
            position: Int,
            chain: Chain
        ): Long {
            val metaAccount = createSingleChainMetaAccount(controller.id, identity, position)
            return accountDao.insertMetaAndChainAccounts(metaAccount) { newId ->
                listOf(createChainAccount(newId, chain))
            }
        }

        private suspend fun addUniversalMultisig(
            controller: MetaAccount,
            identity: Identity?,
            position: Int
        ): Long {
            val metaAccount = createUniversalMetaAccount(controller.id, identity, position)
            return accountDao.insertMetaAccount(metaAccount)
        }

        private fun createUniversalMetaAccount(
            controllerMetaId: Long,
            identity: Identity?,
            position: Int
        ): MetaAccountLocal {
            return MetaAccountLocal(
                substratePublicKey = null,
                substrateCryptoType = null,
                substrateAccountId = substrateAccountId(),
                ethereumPublicKey = null,
                ethereumAddress = ethereumAddress(),
                name = accountName(identity),
                parentMetaId = controllerMetaId,
                isSelected = false,
                position = position,
                type = MetaAccountLocal.Type.MULTISIG,
                status = MetaAccountLocal.Status.ACTIVE,
                globallyUniqueId = MetaAccountLocal.generateGloballyUniqueId(),
                typeExtras = typeExtras()
            )
        }

        private fun createSingleChainMetaAccount(
            controllerMetaId: Long,
            identity: Identity?,
            position: Int
        ): MetaAccountLocal {
            return MetaAccountLocal(
                substratePublicKey = null,
                substrateCryptoType = null,
                substrateAccountId = null,
                ethereumPublicKey = null,
                ethereumAddress = null,
                name = accountName(identity),
                parentMetaId = controllerMetaId,
                isSelected = false,
                position = position,
                type = MetaAccountLocal.Type.MULTISIG,
                status = MetaAccountLocal.Status.ACTIVE,
                globallyUniqueId = MetaAccountLocal.generateGloballyUniqueId(),
                typeExtras = typeExtras()
            )
        }

        private fun createChainAccount(
            multisigId: Long,
            chain: Chain
        ): ChainAccountLocal {
            return ChainAccountLocal(
                metaId = multisigId,
                chainId = chain.id,
                publicKey = null,
                accountId = accountId.value,
                cryptoType = null
            )
        }

        private fun ethereumAddress(): ByteArray? {
            return accountId.value.takeIf { addressScheme.isEvm() }
        }

        private fun substrateAccountId(): ByteArray? {
            return accountId.value.takeIf { addressScheme.isSubstrate() }
        }

        private fun accountName(identity: Identity?): String {
            if (identity != null) return identity.name

            val addressFormat = AddressFormat.defaultForScheme(addressScheme)
            return addressFormat.addressOf(accountId).value
        }

        private fun typeExtras(): String {
            val extras = MultisigTypeExtras(
                otherSignatories,
                threshold,
                signatoryAccountId = controllerAccountId
            )
            return gson.toJson(extras)
        }
    }

    private class MultisigExternalSourceAccount : ExternalSourceCreatedAccount {

        override fun canControl(candidate: ExternalControllableAccount): Boolean {
            return true
        }
    }
}
