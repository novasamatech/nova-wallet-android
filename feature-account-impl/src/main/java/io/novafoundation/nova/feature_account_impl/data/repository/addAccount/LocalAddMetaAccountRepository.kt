package io.novafoundation.nova.feature_account_impl.data.repository.addAccount

import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult

class LocalAddMetaAccountRepository(
    proxySyncService: ProxySyncService,
    metaAccountChangesEventBus: MetaAccountChangesEventBus,
    private val metaAccountDao: MetaAccountDao
) : BaseAddAccountRepository<LocalAddMetaAccountRepository.Payload>(
    proxySyncService,
    metaAccountChangesEventBus
) {

    class Payload(val metaAccountLocal: MetaAccountLocal)

    override suspend fun addAccountInternal(payload: Payload): AddAccountResult {
        val metaId = metaAccountDao.insertMetaAccount(payload.metaAccountLocal)
        return AddAccountResult.AccountAdded(metaId)
    }
}
