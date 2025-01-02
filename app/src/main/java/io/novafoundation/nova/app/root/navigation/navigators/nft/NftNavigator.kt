package io.novafoundation.nova.app.root.navigation.navigators.nft

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.navigators.NavigationHoldersRegistry
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_nft_impl.presentation.nft.details.NftDetailsFragment

class NftNavigator(
    navigationHoldersRegistry: NavigationHoldersRegistry
) : BaseNavigator(navigationHoldersRegistry), NftRouter {

    override fun openNftDetails(nftId: String) {
        navigationBuilder(R.id.action_nftListFragment_to_nftDetailsFragment)
            .setArgs(NftDetailsFragment.getBundle(nftId))
            .perform()
    }
}
