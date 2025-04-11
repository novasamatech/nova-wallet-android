package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.recyclerView.WithViewType
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.ItemManageAssetsBinding

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
        val binder = ItemManageAssetsBinding.inflate(parent.inflater(), parent, false)
        return ManageAssetsHolder(binder, handler)
    }

    override fun onBindViewHolder(holder: ManageAssetsHolder, position: Int) {
        holder.bind(assetViewModeModel)
    }

    override fun getItemViewType(position: Int): Int {
        return ManageAssetsHolder.viewType
    }

    override fun getItemCount(): Int {
        return 1
    }
}

class ManageAssetsHolder(
    private val binder: ItemManageAssetsBinding,
    handler: ManageAssetsAdapter.Handler,
) : RecyclerView.ViewHolder(binder.root) {

    companion object : WithViewType {
        override val viewType: Int = R.layout.item_manage_assets
    }

    init {
        with(binder) {
            balanceListManage.setOnClickListener { handler.manageClicked() }
            balanceListSearch.setOnClickListener { handler.searchClicked() }
            balanceListAssetTitle.setOnClickListener { handler.assetViewModeClicked() }
        }
    }

    fun bind(assetViewModeModel: AssetViewModeModel?) {
        assetViewModeModel?.let { binder.balanceListAssetTitle.switchTextTo(assetViewModeModel) }
    }
}
