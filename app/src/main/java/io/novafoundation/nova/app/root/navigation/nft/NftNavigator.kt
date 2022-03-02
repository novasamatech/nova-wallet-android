package io.novafoundation.nova.app.root.navigation.nft

import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_nft_impl.NftRouter

class NftNavigator(
    navigationHolder: NavigationHolder,
) : BaseNavigator(navigationHolder), NftRouter
