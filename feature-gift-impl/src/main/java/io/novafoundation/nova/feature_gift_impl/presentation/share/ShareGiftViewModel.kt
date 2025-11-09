package io.novafoundation.nova.feature_gift_impl.presentation.share

import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.share.ImageWithTextSharing
import io.novafoundation.nova.common.utils.write
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.DeepLinkConfigurator
import io.novafoundation.nova.feature_gift_impl.R
import io.novafoundation.nova.feature_gift_impl.domain.ClaimGiftInteractor
import io.novafoundation.nova.feature_gift_impl.domain.ShareGiftInteractor
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.feature_gift_impl.presentation.common.PackingGiftAnimationFactory
import io.novafoundation.nova.feature_gift_impl.presentation.claim.deeplink.ClaimGiftDeepLinkConfigurator
import io.novafoundation.nova.feature_gift_impl.presentation.claim.deeplink.ClaimGiftDeepLinkData
import io.novafoundation.nova.feature_gift_impl.presentation.common.claim.ClaimGiftException
import io.novafoundation.nova.feature_gift_impl.presentation.common.claim.ClaimGiftMixinFactory
import io.novafoundation.nova.feature_gift_impl.presentation.share.model.GiftAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatToken
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novasama.substrate_sdk_android.extensions.fromHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val GIFT_FILE_NAME = "share-gift.png"

class ShareGiftViewModel(
    private val router: GiftRouter,
    private val payload: ShareGiftPayload,
    private val shareGiftInteractor: ShareGiftInteractor,
    private val chainRegistry: ChainRegistry,
    private val packingGiftAnimationFactory: PackingGiftAnimationFactory,
    private val assetIconProvider: AssetIconProvider,
    private val tokenFormatter: TokenFormatter,
    private val claimGiftDeepLinkConfigurator: ClaimGiftDeepLinkConfigurator,
    private val fileProvider: FileProvider,
    private val claimGiftMixinFactory: ClaimGiftMixinFactory,
    private val claimGiftInteractor: ClaimGiftInteractor,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    private val giftFlow = shareGiftInteractor.observeGift(payload.giftId)
        .shareInBackground()

    private val chainAssetFlow = giftFlow.map {
        chainRegistry.asset(it.chainId, it.assetId)
    }.shareInBackground()

    val giftAnimationRes = giftFlow.map {
        val chainAsset = chainRegistry.asset(it.chainId, it.assetId)
        val animationRes = packingGiftAnimationFactory.getAnimationForAsset(chainAsset.symbol)

        when (payload.isSecondOpen) {
            true -> ShareGiftAnimationState(animationRes, ShareGiftAnimationState.State.IDLE_END)
            false -> ShareGiftAnimationState(animationRes, ShareGiftAnimationState.State.START)
        }
    }.distinctUntilChanged()
        .shareInBackground()

    val amountModel = giftFlow.map {
        val chainAsset = chainRegistry.asset(it.chainId, it.assetId)
        val tokenIcon = assetIconProvider.getAssetIconOrFallback(chainAsset)
        val giftAmount = tokenFormatter.formatToken(it.amount, chainAsset)
        GiftAmountModel(tokenIcon, giftAmount)
    }

    private val _shareEvent = MutableLiveData<Event<ImageWithTextSharing>>()
    val shareEvent: LiveData<Event<ImageWithTextSharing>> = _shareEvent

    private val claimGiftMixin = claimGiftMixinFactory.create(this)

    private val _isReclaimInProgress = MutableStateFlow(false)
    val isReclaimInProgress: Flow<Boolean> = _isReclaimInProgress

    val reclaimButtonVisible = flowOf { payload.isSecondOpen }

    val confirmReclaimGiftAction = actionAwaitableMixinFactory.confirmingAction<GiftAmountModel>()

    fun back() {
        router.finishCreateGift()
    }

    fun shareDeepLinkClicked() = launchUnit {
        runCatching {
            val giftAmount = amountModel.first().amount
            val sharingLink = getSharingLink()
            val sharingText = resourceManager.getString(R.string.share_gift_text, giftAmount, sharingLink.toString())
            val giftImageFile = generateShareImageFile()
            _shareEvent.value = Event(ImageWithTextSharing(giftImageFile, sharingText))
        }.onFailure(::showError)
    }

    private suspend fun getSharingLink(): Uri {
        val chainAsset = chainAssetFlow.first()
        val giftSeed = shareGiftInteractor.getGiftSeed(payload.giftId)
        val sharingPayload = ClaimGiftDeepLinkData(giftSeed, chainAsset.chainId, chainAsset.symbol)
        return claimGiftDeepLinkConfigurator.configure(sharingPayload, type = DeepLinkConfigurator.Type.APP_LINK)
    }

    suspend fun generateShareImageFile(): Uri = withContext(Dispatchers.IO) {
        val shareBitmap = resourceManager.getDrawable(R.drawable.ic_nova_gift).toBitmap()
        val file = fileProvider.generateTempFile(fixedName = GIFT_FILE_NAME)
        file.write(shareBitmap)

        fileProvider.uriOf(file)
    }

    fun reclaimClicked() = launchUnit {
        val giftAmount = amountModel.first()
        confirmReclaimGiftAction.awaitAction(giftAmount)

        _isReclaimInProgress.value = true
        val giftModel = giftFlow.first()
        val giftSeed = shareGiftInteractor.getGiftSeed(payload.giftId)
        val claimableGift = claimGiftInteractor.getClaimableGift(giftSeed.fromHex(), giftModel.chainId, giftModel.assetId)
        val tempMetaAccount = claimGiftInteractor.createTempMetaAccount(claimableGift)
        val amountWithFee = claimGiftInteractor.getGiftAmountWithFee(claimableGift, tempMetaAccount, coroutineScope)

        claimGiftMixin.claimGift(claimableGift, amountWithFee, tempMetaAccount)
            .onSuccess {
                shareGiftInteractor.setGiftStateAsReclaimed(giftModel.id)
                router.back()
            }
            .onFailure {
                _isReclaimInProgress.value = false
                when (it as ClaimGiftException) {
                    is ClaimGiftException.GiftAlreadyClaimed -> showError(
                        resourceManager.getString(R.string.claim_gift_already_claimed_title),
                        resourceManager.getString(R.string.claim_gift_already_claimed_message)
                    )

                    is ClaimGiftException.UnknownError -> showError(
                        resourceManager.getString(R.string.claim_gift_default_error_title),
                        resourceManager.getString(R.string.claim_gift_default_error_message)
                    )
                }
            }
    }
}
