package io.novafoundation.nova.feature_assets.presentation.tokens.add.selectChain

import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_assets.R
import kotlinx.android.synthetic.main.item_select_chain_group.view.itemSelectChainGroupTextView

class SelectChainGroupAdapter(
    @StringRes val textResId: Int
) : RecyclerView.Adapter<SelectChainGroupHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectChainGroupHolder {
        return SelectChainGroupHolder(
            containerView = parent.inflateChild(R.layout.item_select_chain_group)
        )
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: SelectChainGroupHolder, position: Int) {
        holder.bind(textResId)
    }
}

class SelectChainGroupHolder(containerView: View) : BaseViewHolder(containerView) {

    fun bind(@StringRes textResId: Int) {
        with(containerView) {
            itemSelectChainGroupTextView.setText(textResId)
        }
    }

    override fun unbind() {
    }
}
