package io.novafoundation.nova.feature_account_impl.data.repository.addAccount.ledger

import android.util.Log
import io.novafoundation.nova.common.data.mappers.mapEncryptionToCryptoType
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.batchIfNeeded
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.GenericLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.GenericLedgerAddAccountRepository.Payload
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.BaseAddAccountRepository
import io.novafoundation.nova.feature_ledger_api.data.repository.LedgerDerivationPath
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class RealGenericLedgerAddAccountRepository(
    private val accountDao: MetaAccountDao,
    private val secretStoreV2: SecretStoreV2,
    metaAccountChangesEventBus: MetaAccountChangesEventBus
) : BaseAddAccountRepository<Payload>(metaAccountChangesEventBus), GenericLedgerAddAccountRepository {

    override suspend fun addAccountInternal(payload: Payload): AddAccountResult {
        return when (payload) {
            is Payload.NewWallet -> addNewWallet(payload)
            is Payload.AddMissingChainAccounts -> addMissingChainAccounts(payload)
        }
    }

    private suspend fun addNewWallet(payload: Payload.NewWallet): AddAccountResult {
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
            type = MetaAccountLocal.Type.LEDGER_GENERIC,
            status = MetaAccountLocal.Status.ACTIVE,
            globallyUniqueId = MetaAccountLocal.generateGloballyUniqueId()
        )

        val universalPublicKey = payload.universalAccount.publicKey
        val universalEncryptionType = payload.universalAccount.encryptionType

        val metaId = accountDao.insertMetaAndChainAccounts(metaAccount) { metaId ->
            payload.availableChains.map { chain ->
                ChainAccountLocal(
                    metaId = metaId,
                    chainId = chain.id,
                    publicKey = universalPublicKey,
                    accountId = chain.accountIdOf(universalPublicKey),
                    cryptoType = mapEncryptionToCryptoType(universalEncryptionType)
                )
            }
        }

        val derivationPathKey = LedgerDerivationPath.genericDerivationPathSecretKey()
        secretStoreV2.putAdditionalMetaAccountSecret(metaId, derivationPathKey, payload.universalAccount.derivationPath)

        return AddAccountResult.AccountAdded(metaId, LightMetaAccount.Type.LEDGER)
    }

    private suspend fun addMissingChainAccounts(payload: Payload.AddMissingChainAccounts): AddAccountResult {
        // We could perform update for all meta accounts in once to lower the number of requests to db
        // But this would make implementation more complex
        // and we don't expect users to have that many ledger accounts for this optimization to place a significant difference
       return payload.metaIds.map { metaId ->
           addMissingChainAccounts(metaId, payload.allAvailableChainIds)
       }.batchIfNeeded()
    }

    private suspend fun addMissingChainAccounts(metaId: Long, allAvailableChainIds: Collection<ChainId>): AddAccountResult {
        val currentChainAccounts = accountDao.getChainAccounts(metaId)

        if (currentChainAccounts.isEmpty()) {
            // We should not end up in such situation but handle it gracefully
            Log.wtf(this.LOG_TAG, "Unexpected account storage state - found no chain accounts for Generic Ledger wallet")
            return AddAccountResult.NoOp
        }

        val currentChainIds = currentChainAccounts.mapToSet { it.chainId }
        val missingChainIds = allAvailableChainIds - currentChainIds

        if (missingChainIds.isEmpty()) {
            Log.d(this.LOG_TAG, "Checking account $metaId. Found no missing chain accounts")

            return AddAccountResult.NoOp
        }

        Log.d(this.LOG_TAG, "Checking account $metaId. Found ${missingChainIds.size} missing chain accounts: ${missingChainIds.joinToString()}")

        val someChainAccount = currentChainAccounts.first()

        val missingChainAccounts = missingChainIds.map { missingChainId ->
            someChainAccount.seedNewGenericLedgerChainAccount(missingChainId)
        }
        accountDao.insertChainAccounts(missingChainAccounts)

        return AddAccountResult.AccountChanged(metaId, LightMetaAccount.Type.LEDGER)
    }

    private fun ChainAccountLocal.seedNewGenericLedgerChainAccount(chainId: ChainId): ChainAccountLocal {
        return ChainAccountLocal(
            metaId = metaId,
            chainId = chainId,
            publicKey = publicKey,
            accountId = accountId,
            cryptoType = cryptoType
        )
    }
}
