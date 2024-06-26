package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.feature_account_api.data.cloudBackup.LocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_impl.data.cloudBackup.CloudBackupAccountsModificationsTracker
import io.novafoundation.nova.feature_account_impl.data.cloudBackup.RealCloudBackupAccountsModificationsTracker
import io.novafoundation.nova.feature_account_impl.data.cloudBackup.RealLocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_account_impl.data.mappers.AccountMappers
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class CloudBackupModule {

    @FeatureScope
    @Provides
    fun provideModificationsTracker(
        preferences: Preferences
    ): CloudBackupAccountsModificationsTracker {
        return RealCloudBackupAccountsModificationsTracker(preferences)
    }

    @Provides
    @FeatureScope
    fun provideLocalAccountsCloudBackupFacade(
        secretsStoreV2: SecretStoreV2,
        accountDao: MetaAccountDao,
        cloudBackupAccountsModificationsTracker: CloudBackupAccountsModificationsTracker,
        metaAccountChangedEvents: MetaAccountChangesEventBus,
        chainRegistry: ChainRegistry,
        accountMappers: AccountMappers,
    ): LocalAccountsCloudBackupFacade {
        return RealLocalAccountsCloudBackupFacade(
            secretsStoreV2 = secretsStoreV2,
            accountDao = accountDao,
            cloudBackupAccountsModificationsTracker = cloudBackupAccountsModificationsTracker,
            metaAccountChangedEvents = metaAccountChangedEvents,
            chainRegistry = chainRegistry,
            accountMappers = accountMappers
        )
    }
}
