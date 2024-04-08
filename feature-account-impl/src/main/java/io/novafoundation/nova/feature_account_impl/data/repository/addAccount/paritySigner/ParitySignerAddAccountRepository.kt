package io.novafoundation.nova.feature_account_impl.data.repository.addAccount.paritySigner

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.domain.model.PolkadotVaultVariant
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.BaseAddAccountRepository
import io.novasama.substrate_sdk_android.runtime.AccountId

class ParitySignerAddAccountRepository(
    private val accountDao: MetaAccountDao,
    proxySyncService: ProxySyncService,
    metaAccountChangesEventBus: MetaAccountChangesEventBus
) : BaseAddAccountRepository<ParitySignerAddAccountRepository.Payload>(proxySyncService, metaAccountChangesEventBus) {

    class Payload(
        val name: String,
        val substrateAccountId: AccountId,
        val variant: PolkadotVaultVariant
    )

    override suspend fun addAccountInternal(payload: Payload): AddAccountResult {
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
            status = MetaAccountLocal.Status.ACTIVE,
            globallyUniqueId = MetaAccountLocal.generateGloballyUniqueId()
        )

        val metaId = accountDao.insertMetaAccount(metaAccount)

        return AddAccountResult.AccountAdded(metaId)
    }

    private fun PolkadotVaultVariant.asMetaAccountTypeLocal(): MetaAccountLocal.Type {
        return when (this) {
            PolkadotVaultVariant.POLKADOT_VAULT -> MetaAccountLocal.Type.POLKADOT_VAULT
            PolkadotVaultVariant.PARITY_SIGNER -> MetaAccountLocal.Type.PARITY_SIGNER
        }
    }
}
