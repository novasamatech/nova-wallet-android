package io.novafoundation.nova.app.root.navigation.cloudBackup

import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_cloud_backup_impl.presentation.CloudBackupRouter

class CloudBackupNavigator(navigationHolder: NavigationHolder) : CloudBackupRouter, BaseNavigator(navigationHolder)
