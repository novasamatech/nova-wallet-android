package io.novafoundation.nova.app.root.navigation.navigators.nft

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.BaseNavigator
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_nft_impl.presentation.nft.details.NftDetailsFragment

class NftNavigator(
    navigationHolder: SplitScreenNavigationHolder,
) : BaseNavigator(navigationHolder), NftRouter {

    override fun openNftDetails(nftId: String) = performNavigation(
        actionId = R.id.action_nftListFragment_to_nftDetailsFragment,
        args = NftDetailsFragment.getBundle(nftId)
    )
}
