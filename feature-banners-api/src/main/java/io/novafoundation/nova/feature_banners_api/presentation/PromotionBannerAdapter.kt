package io.novafoundation.nova.feature_banners_api.presentation

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_banners_api.R
import io.novafoundation.nova.feature_banners_api.presentation.view.BannerPagerView
import kotlinx.android.synthetic.main.item_promotion_banner.view.bannerPager
import kotlinx.android.synthetic.main.item_promotion_banner.view.bannerShimmering

class PromotionBannerAdapter(
    private val closable: Boolean,
    private val bannerCallback: BannerPagerView.Callback?
) : SingleItemAdapter<BannerHolder>(isShownByDefault = true) {

    private var showShimmering = true
    private var banners: List<BannerPageModel> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerHolder {
        return BannerHolder(parent.inflateChild(R.layout.item_promotion_banner), closable, bannerCallback)
    }

    override fun onBindViewHolder(holder: BannerHolder, position: Int) {
        holder.bind(showShimmering, banners)
    }

    fun showShimmering(show: Boolean) {
        showShimmering = show
        notifyChangedIfShown()
    }

    fun setBanners(banners: List<BannerPageModel>) {
        this.banners = banners
        notifyChangedIfShown()
    }
}

class BannerHolder(view: View, closable: Boolean, bannerCallback: BannerPagerView.Callback?) : RecyclerView.ViewHolder(view) {

    init {
        itemView.bannerPager.setClosable(closable)
        itemView.bannerPager.setCallback(bannerCallback)
    }

    fun bind(showShimmering: Boolean, banners: List<BannerPageModel>) = with(itemView) {
        showShimmering(showShimmering)
        showBanners(banners)
    }

    fun showShimmering(show: Boolean) = with(itemView) {
        bannerPager.isVisible = !show
        bannerShimmering.isVisible = show
    }

    fun showBanners(banners: List<BannerPageModel>) = with(itemView) {
        bannerPager.setBanners(banners)
    }
}
