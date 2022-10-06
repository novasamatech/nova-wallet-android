package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetModel
import kotlinx.android.synthetic.main.item_referenda_header.view.*

class ReferendaListHeaderAdapter(val imageLoader: ImageLoader) : RecyclerView.Adapter<ReferendaListHeaderAdapter.HeaderHolder>() {

    private var assetModel: AssetModel? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
        return HeaderHolder(parent.inflateChild(R.layout.item_referenda_header))
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
        holder.bind(assetModel)
    }

    override fun getItemCount(): Int {
        return 1
    }

    fun setAsset(assetModel: AssetModel) {
        this.assetModel = assetModel
        notifyItemChanged(0, true)
    }

    inner class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(assetModel: AssetModel?) {
            assetModel?.let { itemView.referendaAssetHeader.setState(imageLoader, assetModel) }
        }
    }
}
