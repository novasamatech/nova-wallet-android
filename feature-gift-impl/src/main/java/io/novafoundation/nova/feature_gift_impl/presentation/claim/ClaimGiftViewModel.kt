package io.novafoundation.nova.feature_gift_impl.presentation.claim

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.share.ImageWithTextSharing
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_gift_impl.domain.ShareGiftInteractor
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.feature_gift_impl.presentation.common.UnpackingGiftAnimationFactory
import io.novafoundation.nova.feature_gift_impl.presentation.share.ShareGiftPayload
import io.novafoundation.nova.feature_gift_impl.presentation.share.deeplink.ShareGiftDeepLinkConfigurator
import io.novafoundation.nova.feature_gift_impl.presentation.share.model.GiftAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatToken
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ClaimGiftViewModel(
    private val router: GiftRouter,
    private val payload: ShareGiftPayload,
    private val shareGiftInteractor: ShareGiftInteractor,
    private val chainRegistry: ChainRegistry,
    private val unpackingGiftAnimationFactory: UnpackingGiftAnimationFactory,
    private val assetIconProvider: AssetIconProvider,
    private val tokenFormatter: TokenFormatter,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    private val giftFlow = shareGiftInteractor.observeGift(payload.giftId)
        .shareInBackground()

    private val chainAssetFlow = giftFlow.map {
        chainRegistry.asset(it.chainId, it.assetId)
    }.shareInBackground()

    val giftAnimationRes = giftFlow.map {
        val chainAsset = chainRegistry.asset(it.chainId, it.assetId)
        unpackingGiftAnimationFactory.getAnimationForAsset(chainAsset.symbol)
    }.distinctUntilChanged()
        .shareInBackground()

    val amountModel = giftFlow.map {
        val chainAsset = chainRegistry.asset(it.chainId, it.assetId)
        val tokenIcon = assetIconProvider.getAssetIconOrFallback(chainAsset)
        val giftAmount = tokenFormatter.formatToken(it.amount, chainAsset)
        GiftAmountModel(tokenIcon, giftAmount)
    }

    private val _giftClaimedEvent = MutableLiveData<Event<Unit>>()
    val giftClaimedEvent: LiveData<Event<Unit>> = _giftClaimedEvent

    fun back() {
        router.back()
    }

    fun claimGift() {


    }

    fun onGiftClaimedAnimationFinished() {
        router.openMainScreen()
    }
}
