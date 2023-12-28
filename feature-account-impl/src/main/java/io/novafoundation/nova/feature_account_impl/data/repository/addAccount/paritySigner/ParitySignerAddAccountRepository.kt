package io.novafoundation.nova.feature_account_impl.data.repository.addAccount.paritySigner

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.BaseAddAccountRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class ParitySignerAddAccountRepository(
    private val accountDao: MetaAccountDao,
    private val chainRegistry: ChainRegistry,
    private val proxySyncService: ProxySyncService,
) : BaseAddAccountRepository<ParitySignerAddAccountRepository.Payload>(proxySyncService) {

    class Payload(
        val name: String,
        val substrateAccountId: AccountId,
        val variant: PolkadotVaultVariant
    )

    override suspend fun addAccountInternal(payload: Payload): Long {
        val metaAccount = MetaAccountLocal(
            // it is safe to assume that accountId is equal to public key since Parity Signer only uses SR25519
            substratePublicKey = payload.substrateAccountId,
            substrateAccountId = payload.substrateAccountId,
            substrateCryptoType = CryptoType.SR25519,
            ethereumPublicKey = null,
            ethereumAddress = null,
            name = payload.name,
            parentMetaId = null,
            isSelected = false,
            position = accountDao.nextAccountPosition(),
            type = payload.variant.asMetaAccountTypeLocal(),
            status = MetaAccountLocal.Status.ACTIVE
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
