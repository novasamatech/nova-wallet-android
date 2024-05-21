package io.novafoundation.nova.feature_cloud_backup_impl.di

import io.novafoundation.nova.common.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory

interface CloudBackupFeatureDependencies {

    val contextManager: ContextManager

    val systemCallExecutor: SystemCallExecutor

    val googleApiAvailabilityProvider: GoogleApiAvailabilityProvider

    val preferences: Preferences

    val encryptedPreferences: EncryptedPreferences

    val resourceManager: ResourceManager

    val actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory
}
