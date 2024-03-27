package io.novafoundation.nova.feature_cloud_backup_impl.di

import io.novafoundation.nova.common.data.GoogleApiAvailabilityProvider
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor

interface CloudBackupFeatureDependencies {

    val contextManager: ContextManager

    val systemCallExecutor: SystemCallExecutor

    val googleApiAvailabilityProvider: GoogleApiAvailabilityProvider
}
