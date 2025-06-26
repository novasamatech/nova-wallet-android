package io.novafoundation.nova.feature_banners_api.presentation

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.recyclerView.WithViewType
import io.novafoundation.nova.feature_banners_api.R
import io.novafoundation.nova.feature_banners_api.databinding.ItemPromotionBannerBinding
import io.novafoundation.nova.feature_banners_api.presentation.view.BannerPagerView

class PromotionBannerAdapter(
    private val closable: Boolean
) : SingleItemAdapter<BannerHolder>(isShownByDefault = false) {

    private var banners: List<BannerPageModel> = listOf()
    private var bannerCallback: BannerPagerView.Callback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerHolder {
        return BannerHolder(ItemPromotionBannerBinding.inflate(parent.inflater(), parent, false), closable)
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

class BannerHolder(private val binder: ItemPromotionBannerBinding, closable: Boolean) : RecyclerView.ViewHolder(binder.root) {

    companion object : WithViewType {
        override val viewType: Int = R.layout.item_promotion_banner
    }

    init {
        binder.bannerPager.setClosable(closable)
    }

    fun bind(banners: List<BannerPageModel>, bannerCallback: BannerPagerView.Callback?) = with(binder) {
        bannerPager.setCallback(bannerCallback)
        showBanners(banners)
    }

    fun showBanners(banners: List<BannerPageModel>) = with(binder) {
        bannerPager.setBanners(banners)
    }
}
