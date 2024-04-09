package io.novafoundation.nova.feature_account_impl.data.repository.addAccount

import io.novafoundation.nova.common.data.secrets.v2.MetaAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novasama.substrate_sdk_android.scale.EncodableStruct

class LocalAddMetaAccountRepository(
    proxySyncService: ProxySyncService,
    metaAccountChangesEventBus: MetaAccountChangesEventBus,
    private val metaAccountDao: MetaAccountDao,
    private val secretStoreV2: SecretStoreV2
) : BaseAddAccountRepository<LocalAddMetaAccountRepository.Payload>(
    proxySyncService,
    metaAccountChangesEventBus
) {

    class Payload(val metaAccountLocal: MetaAccountLocal, val secrets: EncodableStruct<MetaAccountSecrets>)

    override suspend fun addAccountInternal(payload: Payload): AddAccountResult {
        val metaId = metaAccountDao.insertMetaAccount(payload.metaAccountLocal)
        secretStoreV2.putMetaAccountSecrets(metaId, payload.secrets)
        return AddAccountResult.AccountAdded(metaId)
    }
}
