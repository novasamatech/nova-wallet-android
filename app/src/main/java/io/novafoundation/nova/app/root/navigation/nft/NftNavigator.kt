package io.novafoundation.nova.app.root.navigation.nft

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.BaseNavigator
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.feature_assets.presentation.receive.ReceiveFragment
import io.novafoundation.nova.feature_assets.presentation.receive.ReceivePayload
import io.novafoundation.nova.feature_assets.presentation.send.amount.InputAddressNftFragment
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_nft_impl.presentation.NftPayload
import io.novafoundation.nova.feature_nft_impl.presentation.nft.details.NftDetailsFragment
import io.novafoundation.nova.feature_nft_impl.presentation.nft.send.NftTransferDraft
import io.novafoundation.nova.feature_nft_impl.presentation.nft.send.confirm.ConfirmNftSendFragment
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class NftNavigator(
    navigationHolder: NavigationHolder,
) : BaseNavigator(navigationHolder), NftRouter {

    override fun openNftDetails(nftId: String) = performNavigation(
        actionId = R.id.action_to_nftDetailsFragment,
        args = NftDetailsFragment.getBundle(nftId)
    )

    override fun openInputAddressNftFromNftList(nftPayload: NftPayload) {
        return performNavigation(
            actionId = R.id.action_nftListFragment_to_inputAddressNftFragment,
            args = InputAddressNftFragment.getBundle(nftPayload)
        )
    }

    override fun openInputAddressNftFromSendFlow(nftPayload: NftPayload) {
        return performNavigation(
            actionId = R.id.action_nftSendFlowFragment_to_inputAddressNftFragment,
            args = InputAddressNftFragment.getBundle(nftPayload)
        )
    }

    override fun openConfirmScreen(nftTransferDraft: NftTransferDraft) {
        return performNavigation(
            actionId = R.id.action_inputAddressNftFragment_to_confirmSendFragment,
            args = ConfirmNftSendFragment.getBundle(nftTransferDraft)
        )
    }

    override fun closeSendNftFlow() {
        return performNavigation(actionId = R.id.action_show_nftListFragment)
    }

    override fun openNftReceiveFlowFragment() {
        return performNavigation(actionId = R.id.action_show_nftReceiveFlowFragment)
    }

    override fun openNftSendFlowFragment() {
        return performNavigation(actionId = R.id.action_show_nftSendFlowFragment)
    }

    override fun openReceive(chainId: ChainId) {
        return performNavigation(R.id.action_open_receive, ReceiveFragment.getBundle(ReceivePayload.Chain(chainId)))
    }
}
