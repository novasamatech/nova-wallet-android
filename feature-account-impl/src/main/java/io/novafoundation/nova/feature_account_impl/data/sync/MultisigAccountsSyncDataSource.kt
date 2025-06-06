package io.novafoundation.nova.feature_account_impl.data.sync

import com.google.gson.Gson
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.scope.FeatureScope
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
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

@FeatureScope
internal class MultisigAccountsSyncDataSourceFactory @Inject constructor(
    private val multisigRepository: MultisigRepository,
    private val gson: Gson,
    private val accountDao: MetaAccountDao,
) : ExternalAccountsSyncDataSource.Factory {

    override fun create(chain: Chain): ExternalAccountsSyncDataSource? {
        return if (multisigRepository.supportsMultisigSync(chain)) {
            MultisigAccountsSyncDataSource(multisigRepository, gson, accountDao, chain)
        } else {
            null
        }
    }
}

/*
TODO multisig:
1. Integrate multisigs to MetaAccountsUpdatesRegistry - just adding ids to the repository isn't enough as ui only shows proxies, see
MetaAccountGroupingInteractor.updatedProxieds
 */
private class MultisigAccountsSyncDataSource(
    private val multisigRepository: MultisigRepository,
    private val gson: Gson,
    private val accountDao: MetaAccountDao,
    private val chain: Chain,
) : ExternalAccountsSyncDataSource {

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
        return multisigRepository.findMultisigAccounts(chain, accountIdsToQuery)
            .flatMap { discoveredMultisig ->
                discoveredMultisig.allSignatories
                    .filter { it in accountIdsToQuery }
                    .map { ourSignatory ->
                        MultisigExternalControllableAccount(
                            accountId = discoveredMultisig.accountId,
                            controllerAccountId = ourSignatory,
                            threshold = discoveredMultisig.threshold,
                            otherSignatories = discoveredMultisig.otherSignatories(ourSignatory),
                            chain = chain
                        )
                    }
            }
    }

    private inner class MultisigExternalControllableAccount(
        override val accountId: AccountIdKey,
        override val controllerAccountId: AccountIdKey,
        private val threshold: Int,
        private val otherSignatories: List<AccountIdKey>,
        private val chain: Chain,
    ) : ExternalControllableAccount {

        override fun isRepresentedBy(localAccount: MetaAccount): Boolean {
            // Assuming accountId and controllerAccountId match, nothing else to check since both threshold and signers determine accountId
            return localAccount is MultisigMetaAccount
        }

        override suspend fun addControlledAccount(
            controller: MetaAccount,
            identity: Identity?,
            position: Int
        ): AddAccountResult.AccountAdded {
            val newId = addMultisig(controller, identity, position)
            return AddAccountResult.AccountAdded(newId, LightMetaAccount.Type.MULTISIG)
        }

        override fun dispatchChangesOriginFilters(): Boolean {
            return true
        }

        private suspend fun addMultisig(
            controller: MetaAccount,
            identity: Identity?,
            position: Int
        ): Long {
            return when (controller.type) {
                LightMetaAccount.Type.SECRETS,
                LightMetaAccount.Type.WATCH_ONLY -> addMultisigForComplexSigner(controller, identity, position)

                LightMetaAccount.Type.PARITY_SIGNER,
                LightMetaAccount.Type.POLKADOT_VAULT -> addUniversalMultisig(controller, identity, position)

                LightMetaAccount.Type.LEDGER_LEGACY,
                LightMetaAccount.Type.LEDGER -> addSingleChainMultisig(controller, identity, position)

                LightMetaAccount.Type.PROXIED -> addSingleChainMultisig(controller, identity, position)

                LightMetaAccount.Type.MULTISIG -> addMultisigForComplexSigner(controller, identity, position)
            }
        }

        private suspend fun addMultisigForComplexSigner(
            controller: MetaAccount,
            identity: Identity?,
            position: Int
        ): Long {
            return if (controller.chainAccounts.isEmpty()) {
                addUniversalMultisig(controller, identity, position)
            } else {
                addSingleChainMultisig(controller, identity, position)
            }
        }

        private suspend fun addSingleChainMultisig(
            controller: MetaAccount,
            identity: Identity?,
            position: Int
        ): Long {
            val metaAccount = createSingleChainMetaAccount(controller.id, identity, position)
            return accountDao.insertMetaAndChainAccounts(metaAccount) { newId ->
                listOf(createChainAccount(newId))
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
            return accountId.value.takeIf { chain.isEthereumBased }
        }

        private fun substrateAccountId(): ByteArray? {
            return accountId.value.takeIf { !chain.isEthereumBased }
        }

        private fun accountName(identity: Identity?): String {
            return identity?.name ?: chain.addressOf(accountId)
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
