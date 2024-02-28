package io.novafoundation.nova.feature_push_notifications.data.di

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.presentation.referenda.common.ReferendaStatusFormatter
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.CompoundNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.types.SystemNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.NotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.types.DebugNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.types.NewReferendumNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.types.NewReleaseNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.types.ReferendumStateUpdateNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.types.StakingRewardNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.types.TokenReceivedNotificationHandler
import io.novafoundation.nova.feature_push_notifications.data.presentation.handling.types.TokenSentNotificationHandler
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module()
class NotificationHandlersModule {

    @Provides
    fun provideNotificationManagerCompat(context: Context): NotificationManagerCompat {
        return NotificationManagerCompat.from(context)
    }

    @Provides
    @IntoSet
    fun systemNotificationHandler(
        context: Context,
        notificationManagerCompat: NotificationManagerCompat,
        resourceManager: ResourceManager,
        gson: Gson
    ): NotificationHandler {
        return SystemNotificationHandler(context, gson, notificationManagerCompat, resourceManager)
    }

    @Provides
    @IntoSet
    fun tokenSentNotificationHandler(
        context: Context,
        notificationManagerCompat: NotificationManagerCompat,
        resourceManager: ResourceManager,
        gson: Gson,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        tokenRepository: TokenRepository
    ): NotificationHandler {
        return TokenSentNotificationHandler(
            context,
            accountRepository,
            tokenRepository,
            chainRegistry,
            gson,
            notificationManagerCompat,
            resourceManager
        )
    }

    @Provides
    @IntoSet
    fun tokenReceivedNotificationHandler(
        context: Context,
        notificationManagerCompat: NotificationManagerCompat,
        resourceManager: ResourceManager,
        gson: Gson,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        tokenRepository: TokenRepository
    ): NotificationHandler {
        return TokenReceivedNotificationHandler(
            context,
            accountRepository,
            tokenRepository,
            chainRegistry,
            gson,
            notificationManagerCompat,
            resourceManager
        )
    }

    @Provides
    @IntoSet
    fun stakingRewardNotificationHandler(
        context: Context,
        notificationManagerCompat: NotificationManagerCompat,
        resourceManager: ResourceManager,
        gson: Gson,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        tokenRepository: TokenRepository
    ): NotificationHandler {
        return StakingRewardNotificationHandler(
            context,
            accountRepository,
            tokenRepository,
            chainRegistry,
            gson,
            notificationManagerCompat,
            resourceManager
        )
    }

    @Provides
    @IntoSet
    fun referendumStateUpdateNotificationHandler(
        context: Context,
        notificationManagerCompat: NotificationManagerCompat,
        resourceManager: ResourceManager,
        referendaStatusFormatter: ReferendaStatusFormatter,
        gson: Gson,
        chainRegistry: ChainRegistry,
    ): NotificationHandler {
        return ReferendumStateUpdateNotificationHandler(
            context,
            referendaStatusFormatter,
            chainRegistry,
            gson,
            notificationManagerCompat,
            resourceManager
        )
    }

    @Provides
    @IntoSet
    fun newReleaseNotificationHandler(
        context: Context,
        notificationManagerCompat: NotificationManagerCompat,
        resourceManager: ResourceManager,
        gson: Gson
    ): NotificationHandler {
        return NewReleaseNotificationHandler(
            context,
            gson,
            notificationManagerCompat,
            resourceManager
        )
    }

    @Provides
    @IntoSet
    fun newReferendumNotificationHandler(
        context: Context,
        notificationManagerCompat: NotificationManagerCompat,
        resourceManager: ResourceManager,
        gson: Gson,
        chainRegistry: ChainRegistry,
    ): NotificationHandler {
        return NewReferendumNotificationHandler(
            context,
            chainRegistry,
            gson,
            notificationManagerCompat,
            resourceManager
        )
    }

    @Provides
    fun debugNotificationHandler(
        context: Context,
        notificationManagerCompat: NotificationManagerCompat,
        resourceManager: ResourceManager
    ): DebugNotificationHandler {
        return DebugNotificationHandler(context, notificationManagerCompat, resourceManager)
    }

    @Provides
    @FeatureScope
    fun provideCompoundnotificationHandler(
        handlers: Set<@JvmSuppressWildcards NotificationHandler>,
        debugNotificationHandler: DebugNotificationHandler
    ): NotificationHandler {
        val handlersWithDebugHandler = handlers + debugNotificationHandler // Add debug handler as a fallback in the end
        return CompoundNotificationHandler(handlersWithDebugHandler)
    }
}
