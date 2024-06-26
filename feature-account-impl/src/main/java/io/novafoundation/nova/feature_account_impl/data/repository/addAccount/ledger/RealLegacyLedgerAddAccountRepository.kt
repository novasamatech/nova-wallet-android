package io.novafoundation.nova.feature_account_impl.data.repository.addAccount.ledger

import io.novafoundation.nova.common.data.mappers.mapEncryptionToCryptoType
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.LegacyLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.LegacyLedgerAddAccountRepository.Payload
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.BaseAddAccountRepository
import io.novafoundation.nova.feature_ledger_api.data.repository.LedgerDerivationPath
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

class RealLegacyLedgerAddAccountRepository(
    private val accountDao: MetaAccountDao,
    private val chainRegistry: ChainRegistry,
    private val secretStoreV2: SecretStoreV2,
    metaAccountChangesEventBus: MetaAccountChangesEventBus
) : BaseAddAccountRepository<Payload>(metaAccountChangesEventBus), LegacyLedgerAddAccountRepository {

    override suspend fun addAccountInternal(payload: Payload): AddAccountResult {
        return when (payload) {
            is Payload.MetaAccount -> addMetaAccount(payload)
            is Payload.ChainAccount -> addChainAccount(payload)
        }
    }

    private suspend fun addMetaAccount(payload: Payload.MetaAccount): AddAccountResult {
        val metaAccount = MetaAccountLocal(
            substratePublicKey = null,
            substrateCryptoType = null,
            substrateAccountId = null,
            ethereumPublicKey = null,
            ethereumAddress = null,
            name = payload.name,
            parentMetaId = null,
            isSelected = false,
            position = accountDao.nextAccountPosition(),
            type = MetaAccountLocal.Type.LEDGER,
            status = MetaAccountLocal.Status.ACTIVE,
            globallyUniqueId = MetaAccountLocal.generateGloballyUniqueId()
        )

        val metaId = accountDao.insertMetaAndChainAccounts(metaAccount) { metaId ->
            payload.ledgerChainAccounts.map { (chainId, account) ->
                val chain = chainRegistry.getChain(chainId)

                ChainAccountLocal(
                    metaId = metaId,
                    chainId = chainId,
                    publicKey = account.publicKey,
                    accountId = chain.accountIdOf(account.publicKey),
                    cryptoType = mapEncryptionToCryptoType(account.encryptionType)
                )
            }
        }

        payload.ledgerChainAccounts.onEach { (chainId, ledgerAccount) ->
            val derivationPathKey = LedgerDerivationPath.legacyDerivationPathSecretKey(chainId)
            secretStoreV2.putAdditionalMetaAccountSecret(metaId, derivationPathKey, ledgerAccount.derivationPath)
        }

        return AddAccountResult.AccountAdded(metaId, type = Type.LEDGER_LEGACY)
    }

    private suspend fun addChainAccount(payload: Payload.ChainAccount): AddAccountResult {
        val chain = chainRegistry.getChain(payload.chainId)

        val chainAccount = ChainAccountLocal(
            metaId = payload.metaId,
            chainId = payload.chainId,
            publicKey = payload.ledgerChainAccount.publicKey,
            accountId = chain.accountIdOf(payload.ledgerChainAccount.publicKey),
            cryptoType = mapEncryptionToCryptoType(payload.ledgerChainAccount.encryptionType)
        )

        accountDao.insertChainAccount(chainAccount)

        val derivationPathKey = LedgerDerivationPath.legacyDerivationPathSecretKey(payload.chainId)
        secretStoreV2.putAdditionalMetaAccountSecret(payload.metaId, derivationPathKey, payload.ledgerChainAccount.derivationPath)

        return AddAccountResult.AccountChanged(payload.metaId, type = Type.LEDGER)
    }
}
