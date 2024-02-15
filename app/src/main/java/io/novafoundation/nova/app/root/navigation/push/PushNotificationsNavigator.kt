package io.novafoundation.nova.app.root.navigation.push

import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsRouter

class PushNotificationsNavigator(
    navigationHolder: NavigationHolder,
) : BaseNavigator(navigationHolder), PushNotificationsRouter
