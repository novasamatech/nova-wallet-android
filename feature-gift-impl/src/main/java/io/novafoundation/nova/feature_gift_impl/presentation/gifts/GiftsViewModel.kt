package io.novafoundation.nova.feature_gift_impl.presentation.gifts

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_gift_impl.R
import io.novafoundation.nova.feature_gift_impl.domain.GiftsInteractor
import io.novafoundation.nova.feature_gift_impl.domain.models.Gift
import io.novafoundation.nova.feature_gift_impl.domain.models.isClaimed
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatToken
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.combine

class GiftsViewModel(
    private val router: GiftRouter,
    private val appLinksProvider: AppLinksProvider,
    private val giftsInteractor: GiftsInteractor,
    private val chainRegistry: ChainRegistry,
    private val giftsPayload: GiftsPayload,
    private val tokenFormatter: TokenFormatter,
    private val assetIconProvider: AssetIconProvider,
    private val resourceManager: ResourceManager
) : BaseViewModel(), Browserable {

    private val dateFormatter: SimpleDateFormat = SimpleDateFormat("d.M.yyyy", Locale.getDefault())

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    private val chains = chainRegistry.chainsById

    val gifts = combine(
        giftsInteractor.observeGifts(),
        chains
    ) { gifts, chains ->
        gifts.mapNotNull { mapGift(it, chains) }
    }

    fun back() {
        router.back()
    }

    fun learnMoreClicked() {
        openBrowserEvent.value = Event(appLinksProvider.wikiBase)
    }

    fun createGiftClicked() {
        when (giftsPayload) {
            GiftsPayload.AllAssets -> router.openGiftsFlow()
            is GiftsPayload.ByAsset -> router.openSelectGiftAmount(giftsPayload.assetPayload)
        }
    }

    fun giftClicked(gift: GiftRVItem) {
        router.openGiftSharing(gift.id)
    }

    private fun mapGift(gift: Gift, chains: Map<ChainId, Chain>): GiftRVItem? {
        val asset = chains[gift.chainId]?.assetsById[gift.assetId] ?: return null
        val isClaimed = gift.status.isClaimed()
        val subtitle = when (gift.status) {
            Gift.Status.CLAIMED -> resourceManager.getString(R.string.gift_claimed_subtitle)
            Gift.Status.RECLAIMED -> resourceManager.getString(R.string.gift_reclaimed_subtitle)
            Gift.Status.PENDING -> resourceManager.getString(
                R.string.gift_created_subtitle,
                dateFormatter.format(gift.creationDate)
            )
        }

        return GiftRVItem(
            id = gift.id,
            isClaimed = isClaimed,
            amount = tokenFormatter.formatToken(gift.amount, asset),
            assetIcon = assetIconProvider.getAssetIconOrFallback(asset),
            subtitle = subtitle,
            imageRes = if (isClaimed) R.drawable.ic_gift_unpacked else R.drawable.ic_gift_packed,
        )
    }
}
