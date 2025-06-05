package io.novafoundation.nova.feature_push_notifications.di

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.interfaces.ActivityIntentProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.presentation.balance.detail.deeplink.AssetDetailsDeepLinkConfigurator
import io.novafoundation.nova.feature_governance_api.presentation.referenda.common.ReferendaStatusFormatter
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.deeplink.configurators.ReferendumDetailsDeepLinkConfigurator
import io.novafoundation.nova.feature_push_notifications.presentation.handling.CompoundNotificationHandler
import io.novafoundation.nova.feature_push_notifications.presentation.handling.types.SystemNotificationHandler
import io.novafoundation.nova.feature_push_notifications.presentation.handling.NotificationHandler
import io.novafoundation.nova.feature_push_notifications.presentation.handling.NotificationIdProvider
import io.novafoundation.nova.feature_push_notifications.presentation.handling.RealNotificationIdProvider
import io.novafoundation.nova.feature_push_notifications.presentation.handling.types.DebugNotificationHandler
import io.novafoundation.nova.feature_push_notifications.presentation.handling.types.NewReferendumNotificationHandler
import io.novafoundation.nova.feature_push_notifications.presentation.handling.types.NewReleaseNotificationHandler
import io.novafoundation.nova.feature_push_notifications.presentation.handling.types.ReferendumStateUpdateNotificationHandler
import io.novafoundation.nova.feature_push_notifications.presentation.handling.types.StakingRewardNotificationHandler
import io.novafoundation.nova.feature_push_notifications.presentation.handling.types.TokenReceivedNotificationHandler
import io.novafoundation.nova.feature_push_notifications.presentation.handling.types.TokenSentNotificationHandler
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module()
class NotificationHandlersModule {

    @Provides
    fun provideNotificationIdProvider(preferences: Preferences): NotificationIdProvider {
        return RealNotificationIdProvider(preferences)
    }

    @Provides
    fun provideNotificationManagerCompat(context: Context): NotificationManagerCompat {
        return NotificationManagerCompat.from(context)
    }

    @Provides
    @IntoSet
    fun systemNotificationHandler(
        context: Context,
        activityIntentProvider: ActivityIntentProvider,
        notificationIdProvider: NotificationIdProvider,
        notificationManagerCompat: NotificationManagerCompat,
        resourceManager: ResourceManager,
        gson: Gson
    ): NotificationHandler {
        return SystemNotificationHandler(context, activityIntentProvider, notificationIdProvider, gson, notificationManagerCompat, resourceManager)
    }

    @Provides
    @IntoSet
    fun tokenSentNotificationHandler(
        context: Context,
        notificationIdProvider: NotificationIdProvider,
        notificationManagerCompat: NotificationManagerCompat,
        resourceManager: ResourceManager,
        gson: Gson,
        activityIntentProvider: ActivityIntentProvider,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        tokenRepository: TokenRepository,
        configurator: AssetDetailsDeepLinkConfigurator
    ): NotificationHandler {
        return TokenSentNotificationHandler(
            context,
            accountRepository,
            tokenRepository,
            chainRegistry,
            configurator,
            activityIntentProvider,
            notificationIdProvider,
            gson,
            notificationManagerCompat,
            resourceManager
        )
    }

    @Provides
    @IntoSet
    fun tokenReceivedNotificationHandler(
        context: Context,
        notificationIdProvider: NotificationIdProvider,
        notificationManagerCompat: NotificationManagerCompat,
        resourceManager: ResourceManager,
        gson: Gson,
        activityIntentProvider: ActivityIntentProvider,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        tokenRepository: TokenRepository,
        configurator: AssetDetailsDeepLinkConfigurator
    ): NotificationHandler {
        return TokenReceivedNotificationHandler(
            context,
            accountRepository,
            tokenRepository,
            configurator,
            chainRegistry,
            activityIntentProvider,
            notificationIdProvider,
            gson,
            notificationManagerCompat,
            resourceManager
        )
    }

    @Provides
    @IntoSet
    fun stakingRewardNotificationHandler(
        context: Context,
        notificationIdProvider: NotificationIdProvider,
        notificationManagerCompat: NotificationManagerCompat,
        resourceManager: ResourceManager,
        gson: Gson,
        activityIntentProvider: ActivityIntentProvider,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        tokenRepository: TokenRepository,
        configurator: AssetDetailsDeepLinkConfigurator
    ): NotificationHandler {
        return StakingRewardNotificationHandler(
            context,
            accountRepository,
            tokenRepository,
            configurator,
            chainRegistry,
            activityIntentProvider,
            notificationIdProvider,
            gson,
            notificationManagerCompat,
            resourceManager
        )
    }

    @Provides
    @IntoSet
    fun referendumStateUpdateNotificationHandler(
        context: Context,
        notificationIdProvider: NotificationIdProvider,
        notificationManagerCompat: NotificationManagerCompat,
        resourceManager: ResourceManager,
        activityIntentProvider: ActivityIntentProvider,
        referendaStatusFormatter: ReferendaStatusFormatter,
        gson: Gson,
        chainRegistry: ChainRegistry,
        configurator: ReferendumDetailsDeepLinkConfigurator
    ): NotificationHandler {
        return ReferendumStateUpdateNotificationHandler(
            context,
            configurator,
            referendaStatusFormatter,
            chainRegistry,
            activityIntentProvider,
            notificationIdProvider,
            gson,
            notificationManagerCompat,
            resourceManager
        )
    }

    @Provides
    @IntoSet
    fun newReleaseNotificationHandler(
        context: Context,
        appLinksProvider: AppLinksProvider,
        activityIntentProvider: ActivityIntentProvider,
        notificationIdProvider: NotificationIdProvider,
        notificationManagerCompat: NotificationManagerCompat,
        resourceManager: ResourceManager,
        gson: Gson
    ): NotificationHandler {
        return NewReleaseNotificationHandler(
            context,
            appLinksProvider,
            activityIntentProvider,
            notificationIdProvider,
            gson,
            notificationManagerCompat,
            resourceManager
        )
    }

    @Provides
    @IntoSet
    fun newReferendumNotificationHandler(
        context: Context,
        notificationIdProvider: NotificationIdProvider,
        notificationManagerCompat: NotificationManagerCompat,
        activityIntentProvider: ActivityIntentProvider,
        resourceManager: ResourceManager,
        gson: Gson,
        chainRegistry: ChainRegistry,
        configurator: ReferendumDetailsDeepLinkConfigurator
    ): NotificationHandler {
        return NewReferendumNotificationHandler(
            context,
            configurator,
            chainRegistry,
            activityIntentProvider,
            notificationIdProvider,
            gson,
            notificationManagerCompat,
            resourceManager
        )
    }

    @Provides
    fun debugNotificationHandler(
        context: Context,
        activityIntentProvider: ActivityIntentProvider,
        notificationManagerCompat: NotificationManagerCompat,
        resourceManager: ResourceManager
    ): DebugNotificationHandler {
        return DebugNotificationHandler(context, activityIntentProvider, notificationManagerCompat, resourceManager)
    }

    @Provides
    @FeatureScope
    fun provideCompoundNotificationHandler(
        handlers: Set<@JvmSuppressWildcards NotificationHandler>,
        debugNotificationHandler: DebugNotificationHandler
    ): NotificationHandler {
        val handlersWithDebugHandler = handlers + debugNotificationHandler // Add debug handler as a fallback in the end
        return CompoundNotificationHandler(handlersWithDebugHandler)
    }
}
