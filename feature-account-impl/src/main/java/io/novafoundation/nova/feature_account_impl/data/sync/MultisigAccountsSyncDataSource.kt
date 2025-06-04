package io.novafoundation.nova.feature_account_impl.data.sync

import com.google.gson.Gson
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.core_db.dao.MetaAccountDao
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

class MultisigAccountsSyncDataSource(
    private val multisigRepository: MultisigRepository,
    private val gson: Gson,
    private val accountDao: MetaAccountDao
) : ExternalAccountsSyncDataSource {

    override suspend fun isCreatedFromDataSource(metaAccount: MetaAccount): Boolean {
        return metaAccount is MultisigMetaAccount
    }

    override suspend fun getControllableExternalAccounts(
        accountIdsToQuery: Set<AccountIdKey>,
        chain: Chain
    ): List<ExternalControllableAccount> {
        return multisigRepository.findMultisigAccounts(chain, accountIdsToQuery).flatMap { discoveredMultisig ->
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

        override suspend fun addAccount(controllerMetaId: Long, identity: Identity?, position: Int): AddAccountResult.AccountAdded {
            val metaAccount = createMetaAccount(controllerMetaId, identity, position)
            val newMetaId = accountDao.insertMetaAccount(metaAccount)
            return AddAccountResult.AccountAdded(newMetaId, LightMetaAccount.Type.MULTISIG)
        }

        private fun createMetaAccount(controllerMetaId: Long, identity: Identity?, position: Int): MetaAccountLocal {
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
            val extras = MultisigTypeExtras(otherSignatories, threshold, signatoryAccountId = controllerAccountId)
            return gson.toJson(extras)
        }
    }
}
