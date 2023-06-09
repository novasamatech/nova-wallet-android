package io.novafoundation.nova.feature_account_impl.data.repository

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface ParitySignerRepository {

    suspend fun addParitySignerWallet(
        name: String,
        substrateAccountId: AccountId,
        variant: PolkadotVaultVariant,
    ): Long
}

class RealParitySignerRepository(
    private val accountDao: MetaAccountDao
) : ParitySignerRepository {

    override suspend fun addParitySignerWallet(
        name: String,
        substrateAccountId: AccountId,
        variant: PolkadotVaultVariant
    ): Long {
        val metaAccount = MetaAccountLocal(
            // it is safe to assume that accountId is equal to public key since Parity Signer only uses SR25519
            substratePublicKey = substrateAccountId,
            substrateAccountId = substrateAccountId,
            substrateCryptoType = CryptoType.SR25519,
            ethereumPublicKey = null,
            ethereumAddress = null,
            name = name,
            isSelected = false,
            position = accountDao.nextAccountPosition(),
            type = variant.asMetaAccountTypeLocal()
        )

        return accountDao.insertMetaAccount(metaAccount)
    }

    private fun PolkadotVaultVariant.asMetaAccountTypeLocal(): MetaAccountLocal.Type {
        return when (this) {
            PolkadotVaultVariant.POLKADOT_VAULT -> MetaAccountLocal.Type.POLKADOT_VAULT
            PolkadotVaultVariant.PARITY_SIGNER -> MetaAccountLocal.Type.PARITY_SIGNER
        }
    }
}
