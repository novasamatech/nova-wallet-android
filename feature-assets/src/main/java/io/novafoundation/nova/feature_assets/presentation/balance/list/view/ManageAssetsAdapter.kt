package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_assets.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_manage_assets.view.balanceListAssetTitle
import kotlinx.android.synthetic.main.item_manage_assets.view.balanceListManage
import kotlinx.android.synthetic.main.item_manage_assets.view.balanceListSearch

class ManageAssetsAdapter(private val handler: Handler) : RecyclerView.Adapter<ManageAssetsHolder>() {

    interface Handler {
        fun searchClicked()

        fun manageClicked()

        fun assetViewModeClicked()
    }

    private var assetViewModeModel: AssetViewModeModel? = null

    fun setAssetViewModeModel(assetViewModeModel: AssetViewModeModel) {
        this.assetViewModeModel = assetViewModeModel

        notifyItemChanged(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageAssetsHolder {
        return ManageAssetsHolder(parent.inflateChild(R.layout.item_manage_assets), handler)
    }

    override fun onBindViewHolder(holder: ManageAssetsHolder, position: Int) {
        holder.bind(assetViewModeModel)
    }

    override fun getItemCount(): Int {
        return 1
    }
}

class ManageAssetsHolder(
    override val containerView: View,
    handler: ManageAssetsAdapter.Handler,
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        with(containerView) {
            balanceListManage.setOnClickListener { handler.manageClicked() }
            balanceListSearch.setOnClickListener { handler.searchClicked() }
            balanceListAssetTitle.setOnClickListener { handler.assetViewModeClicked() }
        }
    }

    fun bind(assetViewModeModel: AssetViewModeModel?) {
        assetViewModeModel?.let { itemView.balanceListAssetTitle.switchTextTo(assetViewModeModel) }
    }
}
