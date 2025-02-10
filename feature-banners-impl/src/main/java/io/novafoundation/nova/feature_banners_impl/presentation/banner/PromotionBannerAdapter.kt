package io.novafoundation.nova.feature_banners_impl.presentation.banner

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_banners_impl.presentation.banner.BannerPageModel
import io.novafoundation.nova.feature_banners_impl.presentation.banner.BannerPagerView
import kotlinx.android.synthetic.main.item_promotion_banner.view.bannerPager
import kotlinx.android.synthetic.main.item_promotion_banner.view.bannerShimmering

class PromotionBannerAdapter(
    private val closable: Boolean,
    private val bannerCallback: io.novafoundation.nova.feature_banners_impl.presentation.banner.BannerPagerView.Callback?
) : SingleItemAdapter<BannerHolder>(isShownByDefault = true) {

    private var showShimmering = true
    private var banners: List<io.novafoundation.nova.feature_banners_impl.presentation.banner.BannerPageModel> = listOf()

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

    fun setBanners(banners: List<io.novafoundation.nova.feature_banners_impl.presentation.banner.BannerPageModel>) {
        this.banners = banners
        notifyChangedIfShown()
    }
}

class BannerHolder(view: View, closable: Boolean, bannerCallback: io.novafoundation.nova.feature_banners_impl.presentation.banner.BannerPagerView.Callback?) : RecyclerView.ViewHolder(view) {

    init {
        itemView.bannerPager.setClosable(closable)
        itemView.bannerPager.setCallback(bannerCallback)
    }

    fun bind(showShimmering: Boolean, banners: List<io.novafoundation.nova.feature_banners_impl.presentation.banner.BannerPageModel>) = with(itemView) {
        showShimmering(showShimmering)
        showBanners(banners)
    }

    fun showShimmering(show: Boolean) = with(itemView) {
        bannerPager.isVisible = !show
        bannerShimmering.isVisible = show
    }

    fun showBanners(banners: List<io.novafoundation.nova.feature_banners_impl.presentation.banner.BannerPageModel>) = with(itemView) {
        bannerPager.setBanners(banners)
    }
}
