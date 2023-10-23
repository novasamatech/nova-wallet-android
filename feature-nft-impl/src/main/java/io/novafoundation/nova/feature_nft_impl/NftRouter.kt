package io.novafoundation.nova.feature_nft_impl

import io.novafoundation.nova.feature_nft_impl.presentation.NftPayload
import io.novafoundation.nova.feature_nft_impl.presentation.nft.send.NftTransferDraft
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface NftRouter {

    fun openInputAddressNftFromNftList(nftPayload: NftPayload)

    fun openInputAddressNftFromSendFlow(nftPayload: NftPayload)

    fun openNftDetails(nftId: String)

    fun openConfirmScreen(nftTransferDraft: NftTransferDraft)

    fun back()

    fun closeSendNftFlow()

    fun openNftReceiveFlowFragment()

    fun openNftSendFlowFragment()

    fun openReceive(chainId: ChainId)
}
