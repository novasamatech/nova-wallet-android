package io.novafoundation.nova.feature_ledger_impl.data.repository

import io.novafoundation.nova.common.data.mappers.mapEncryptionToCryptoType
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.MetaAccountLocal
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.LedgerSubstrateAccount
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface LedgerRepository {

    suspend fun insertLedgerMetaAccount(
        name: String,
        ledgerChainAccounts: Map<ChainId, LedgerSubstrateAccount>
    ): Long
}

class RealLedgerRepository(
    private val metaAccountDao: MetaAccountDao,
    private val chainRegistry: ChainRegistry,
) : LedgerRepository {

    override suspend fun insertLedgerMetaAccount(
        name: String,
        ledgerChainAccounts: Map<ChainId, LedgerSubstrateAccount>
    ): Long {
        val metaAccount = MetaAccountLocal(
            substratePublicKey = null,
            substrateCryptoType = null,
            substrateAccountId = null,
            ethereumPublicKey = null,
            ethereumAddress = null,
            name = name,
            isSelected = false,
            position = metaAccountDao.nextAccountPosition(),
            type = MetaAccountLocal.Type.LEDGER
        )
        val metaId = metaAccountDao.insertMetaAccount(metaAccount)

        val chainAccounts = ledgerChainAccounts.map { (chainId, account) ->
            val chain = chainRegistry.getChain(chainId)

            ChainAccountLocal(
                metaId = metaId,
                chainId = chainId,
                publicKey = account.publicKey,
                accountId = chain.accountIdOf(account.publicKey),
                cryptoType = mapEncryptionToCryptoType(account.encryptionType)
            )
        }
        metaAccountDao.insertChainAccounts(chainAccounts)

        return metaId
    }
}
