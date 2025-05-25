package io.novafoundation.nova.feature_deep_linking.di

import android.content.Context
import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate

interface DeepLinkingFeatureDependencies {

    val rootScope: RootScope

    val preferences: Preferences

    val context: Context

    val permissionsAskerFactory: PermissionsAskerFactory

    val resourceManager: ResourceManager

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory

    val imageLoader: ImageLoader

    val gson: Gson

    val automaticInteractionGate: AutomaticInteractionGate
}
