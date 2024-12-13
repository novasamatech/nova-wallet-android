package io.novafoundation.nova.app.root.navigation.navigators.cloudBackup

import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_cloud_backup_impl.presentation.CloudBackupRouter

class CloudBackupNavigator(navigationHoldersRegistry: NavigationHoldersRegistry) : CloudBackupRouter,
    BaseNavigator(navigationHoldersRegistry)
