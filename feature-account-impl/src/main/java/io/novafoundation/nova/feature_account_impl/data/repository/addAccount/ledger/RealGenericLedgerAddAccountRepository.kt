package io.novafoundation.nova.feature_account_impl.data.repository.addAccount.ledger

import io.novafoundation.nova.common.data.mappers.mapEncryptionToCryptoType
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.GenericLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.GenericLedgerAddAccountRepository.Payload
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.BaseAddAccountRepository
import io.novafoundation.nova.feature_ledger_api.data.repository.LedgerDerivationPath
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId

class RealGenericLedgerAddAccountRepository(
    private val accountDao: MetaAccountDao,
    private val secretStoreV2: SecretStoreV2,
    metaAccountChangesEventBus: MetaAccountChangesEventBus
) : BaseAddAccountRepository<Payload>(metaAccountChangesEventBus), GenericLedgerAddAccountRepository {

    override suspend fun addAccountInternal(payload: Payload): AddAccountResult {
        return when (payload) {
            is Payload.NewWallet -> addNewWallet(payload)
        }
    }

    private suspend fun addNewWallet(payload: Payload.NewWallet): AddAccountResult {
        val metaAccount = MetaAccountLocal(
            substratePublicKey = payload.substrateAccount.publicKey,
            substrateCryptoType = mapEncryptionToCryptoType(payload.substrateAccount.encryptionType),
            substrateAccountId = payload.substrateAccount.address.toAccountId(),
            ethereumPublicKey = payload.evmAccount?.publicKey,
            ethereumAddress = payload.evmAccount?.accountId,
            name = payload.name,
            parentMetaId = null,
            isSelected = false,
            position = accountDao.nextAccountPosition(),
            type = MetaAccountLocal.Type.LEDGER_GENERIC,
            status = MetaAccountLocal.Status.ACTIVE,
            globallyUniqueId = MetaAccountLocal.generateGloballyUniqueId()
        )

        val metaId = accountDao.insertMetaAccount(metaAccount)
        val derivationPathKey = LedgerDerivationPath.genericDerivationPathSecretKey()
        secretStoreV2.putAdditionalMetaAccountSecret(metaId, derivationPathKey, payload.substrateAccount.derivationPath)

        return AddAccountResult.AccountAdded(metaId, LightMetaAccount.Type.LEDGER)
    }
}
