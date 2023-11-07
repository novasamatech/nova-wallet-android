package io.novafoundation.nova.feature_assets.presentation.receive.flow

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_nft_impl.NftRouter
import io.novafoundation.nova.feature_nft_impl.R
import io.novafoundation.nova.feature_nft_impl.domain.nft.chains.NftChainsInteractor
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.flow

class NftReceiveFlowViewModel(
    private val router: NftRouter,
    private val nftChainsInteractor: NftChainsInteractor
) : BaseViewModel() {

    val nftChainsFlow = flow { emit(nftChainsInteractor.getAvailableChains()) }
        .inBackground()
        .share()

    fun chainSelected(item: Chain) {
        openNextScreen(item)
    }

    private fun openNextScreen(item: Chain) {
        router.openReceive(item.id, R.string.nft_receive)
    }

    fun backClicked() {
        router.back()
    }
}
