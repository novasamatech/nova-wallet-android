package io.novafoundation.nova.feature_account_impl.data.repository

import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.MetaAccountLocal
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.scan.ParitySignerAccount

interface ParitySignerRepository {

    suspend fun addParitySignerWallet(
        name: String,
        paritySignerAccount: ParitySignerAccount,
    ): Long
}

class RealParitySignerRepository(
    private val accountDao: MetaAccountDao
) : ParitySignerRepository {

    override suspend fun addParitySignerWallet(name: String, paritySignerAccount: ParitySignerAccount): Long {
        val metaAccount = when (paritySignerAccount.accountType) {
            ParitySignerAccount.Type.SUBSTRATE -> substrateMetaAccount(name, paritySignerAccount.accountId)
            ParitySignerAccount.Type.ETHEREUM -> ethereumMetaAccount(name, paritySignerAccount.accountId)
        }

        return accountDao.insertMetaAccount(metaAccount)
    }

    private suspend fun substrateMetaAccount(name: String, accountId: ByteArray): MetaAccountLocal {
        return MetaAccountLocal(
            substratePublicKey = null,
            substrateAccountId = accountId,
            substrateCryptoType = null,
            ethereumPublicKey = null,
            ethereumAddress = null,
            name = name,
            isSelected = false,
            position = accountDao.nextAccountPosition(),
            type = MetaAccountLocal.Type.PARITY_SIGNER
        )
    }

    private suspend fun ethereumMetaAccount(name: String, accountId: ByteArray): MetaAccountLocal {
        return MetaAccountLocal(
            substratePublicKey = null,
            substrateAccountId = null,
            substrateCryptoType = null,
            ethereumPublicKey = null,
            ethereumAddress = accountId,
            name = name,
            isSelected = false,
            position = accountDao.nextAccountPosition(),
            type = MetaAccountLocal.Type.PARITY_SIGNER
        )
    }
}
