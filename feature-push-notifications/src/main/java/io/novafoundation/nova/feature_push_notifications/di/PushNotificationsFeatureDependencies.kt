package io.novafoundation.nova.feature_push_notifications.di

import android.content.Context
import coil.ImageLoader
import com.google.gson.Gson
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.AssetDetailsDeepLinkHandler
import io.novafoundation.nova.app.root.presentation.deepLinks.handlers.ReferendumDeepLinkHandler
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.interfaces.ActivityIntentProvider
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.permissions.PermissionsAskerFactory
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.presentation.referenda.common.ReferendaStatusFormatter
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface PushNotificationsFeatureDependencies {

    val rootScope: RootScope

    val preferences: Preferences

    val context: Context

    val chainRegistry: ChainRegistry

    val permissionsAskerFactory: PermissionsAskerFactory

    val resourceManager: ResourceManager

    val accountRepository: AccountRepository

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory

    val governanceSourceRegistry: GovernanceSourceRegistry

    val imageLoader: ImageLoader

    val gson: Gson

    val referendaStatusFormatter: ReferendaStatusFormatter

    val referendumDeepLinkHandler: ReferendumDeepLinkHandler

    val assetDetailsDeepLinkHandler: AssetDetailsDeepLinkHandler

    val tokenRepository: TokenRepository

    val provideActivityIntentProvider: ActivityIntentProvider

    val appLinksProvider: AppLinksProvider

    val metaAccountChangesEventBus: MetaAccountChangesEventBus
}
