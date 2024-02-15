package io.novafoundation.nova.feature_push_notifications.data.di

import android.content.Context
import com.google.gson.Gson
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface PushNotificationsFeatureDependencies {

    val gson: Gson

    val rootScope: RootScope

    val preferences: Preferences

    val context: Context

    val chainRegistry: ChainRegistry

    val permissionsAskerFactory: PermissionsAskerFactory

    val resourceManager: ResourceManager
}
