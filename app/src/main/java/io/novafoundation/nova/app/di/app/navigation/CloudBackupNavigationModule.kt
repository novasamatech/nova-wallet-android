package io.novafoundation.nova.app.di.app.navigation

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.navigation.cloudBackup.CloudBackupNavigator
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_cloud_backup_impl.presentation.CloudBackupRouter

@Module
class CloudBackupNavigationModule {

    @ApplicationScope
    @Provides
    fun provideRouter(navigationHolder: NavigationHolder): CloudBackupRouter = CloudBackupNavigator(navigationHolder)
}
