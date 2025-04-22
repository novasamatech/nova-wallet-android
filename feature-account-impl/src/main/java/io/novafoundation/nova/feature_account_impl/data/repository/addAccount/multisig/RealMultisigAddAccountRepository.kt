package io.novafoundation.nova.feature_account_impl.data.repository.addAccount.multisig

import com.google.gson.Gson
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.dao.withTransaction
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MultisigTypeExtras
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.batchIfNeeded
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.multisig.MultisigAddAccountRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.multisig.MultisigAddAccountRepository.AccountPayload
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.multisig.MultisigAddAccountRepository.Payload
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.BaseAddAccountRepository
import io.novafoundation.nova.runtime.ext.addressOf
import javax.inject.Inject

@FeatureScope
class RealMultisigAddAccountRepository @Inject constructor(
    private val accountDao: MetaAccountDao,
    private val gson: Gson,
    metaAccountChangesEventBus: MetaAccountChangesEventBus
) : BaseAddAccountRepository<Payload>(metaAccountChangesEventBus), MultisigAddAccountRepository {

    override suspend fun addAccountInternal(payload: Payload): AddAccountResult {
        var position = accountDao.nextAccountPosition()

        val results = accountDao.withTransaction {
            payload.accounts.map { accountPayload ->
                val account = createMetaAccount(accountPayload, position++)
                val createdId = accountDao.insertMetaAccount(account)
                AddAccountResult.AccountAdded(createdId, LightMetaAccount.Type.MULTISIG)
            }
        }

        return results.batchIfNeeded()
    }

    private fun createMetaAccount(
        payload: AccountPayload,
        position: Int
    ): MetaAccountLocal {
        return MetaAccountLocal(
            substratePublicKey = null,
            substrateCryptoType = null,
            substrateAccountId = payload.substrateAccountId(),
            ethereumPublicKey = null,
            ethereumAddress = payload.ethereumAddress(),
            name = payload.accountName(),
            parentMetaId = payload.signatoryMetaId,
            isSelected = false,
            position = position,
            type = MetaAccountLocal.Type.MULTISIG,
            status = MetaAccountLocal.Status.ACTIVE,
            globallyUniqueId = MetaAccountLocal.generateGloballyUniqueId(),
            typeExtras = payload.typeExtras()
        )
    }

    private fun AccountPayload.ethereumAddress(): ByteArray? {
        return multisigAccountId.value.takeIf { chain.isEthereumBased }
    }

    private fun AccountPayload.substrateAccountId(): ByteArray? {
        return multisigAccountId.value.takeIf { !chain.isEthereumBased }
    }

    private fun AccountPayload.accountName(): String {
        return identity?.name ?: chain.addressOf(multisigAccountId)
    }

    private fun AccountPayload.typeExtras(): String {
        val extras = MultisigTypeExtras(otherSignatories, threshold, signatoryAccountId)
        return gson.toJson(extras)
    }
}
