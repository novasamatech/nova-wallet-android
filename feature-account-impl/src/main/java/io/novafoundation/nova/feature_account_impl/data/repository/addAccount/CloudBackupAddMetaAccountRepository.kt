package io.novafoundation.nova.feature_account_impl.data.repository.addAccount

import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_impl.domain.common.mapLocalAccountFromCloudBackup
import io.novafoundation.nova.feature_account_impl.domain.common.mapLocalChainAccountFromCloudBackup
import io.novafoundation.nova.feature_account_impl.domain.common.mapLocalChainAccountsFromCloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup

class CloudBackupAddMetaAccountRepository(
    proxySyncService: ProxySyncService,
    metaAccountChangesEventBus: MetaAccountChangesEventBus,
    private val metaAccountDao: MetaAccountDao
) : BaseAddAccountRepository<CloudBackupAddMetaAccountRepository.Payload>(
    proxySyncService,
    metaAccountChangesEventBus
) {

    class Payload(val cloudBackup: CloudBackup)

    override suspend fun addAccountInternal(payload: Payload): AddAccountResult {
        val addedMetaIds = payload.cloudBackup.wallets.map { wallet ->
            val metaAccount = mapLocalAccountFromCloudBackup(wallet, metaAccountDao::nextAccountPosition)
            metaAccountDao.insertMetaAndChainAccounts(metaAccount) {
                mapLocalChainAccountsFromCloudBackup(it, wallet)
            }
        }

        return AddAccountResult.AccountAdded(addedMetaIds)
    }
}
