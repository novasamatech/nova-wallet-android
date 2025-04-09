package io.novafoundation.nova.feature_banners_api.presentation

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.recyclerView.WithViewType
import io.novafoundation.nova.feature_banners_api.R
import io.novafoundation.nova.feature_banners_api.presentation.view.BannerPagerView
import kotlinx.android.synthetic.main.item_promotion_banner.view.bannerPager

class PromotionBannerAdapter(
    private val closable: Boolean
) : SingleItemAdapter<BannerHolder>(isShownByDefault = false) {

    private var banners: List<BannerPageModel> = listOf()
    private var bannerCallback: BannerPagerView.Callback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerHolder {
        return BannerHolder(parent.inflateChild(viewType), closable)
    }

    override fun onBindViewHolder(holder: BannerHolder, position: Int) {
        holder.bind(banners, bannerCallback)
    }

    override fun getItemViewType(position: Int): Int {
        return BannerHolder.viewType
    }

    fun setBanners(banners: List<BannerPageModel>) {
        this.banners = banners
        notifyChangedIfShown()
    }

    fun setCallback(bannerCallback: BannerPagerView.Callback?) {
        this.bannerCallback = bannerCallback
        notifyChangedIfShown()
    }
}

class BannerHolder(view: View, closable: Boolean) : RecyclerView.ViewHolder(view) {

    companion object : WithViewType {
        override val viewType: Int = R.layout.item_promotion_banner
    }

    init {
        itemView.bannerPager.setClosable(closable)
    }

    fun bind(banners: List<BannerPageModel>, bannerCallback: BannerPagerView.Callback?) = with(itemView) {
        bannerPager.setCallback(bannerCallback)
        showBanners(banners)
    }

    fun showBanners(banners: List<BannerPageModel>) = with(itemView) {
        bannerPager.setBanners(banners)
    }
}
