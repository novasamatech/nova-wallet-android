package io.novafoundation.nova.app.root.navigation.governance

import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter

class GovernanceNavigator(
    navigationHolder: NavigationHolder
) : BaseNavigator(navigationHolder), GovernanceRouter
