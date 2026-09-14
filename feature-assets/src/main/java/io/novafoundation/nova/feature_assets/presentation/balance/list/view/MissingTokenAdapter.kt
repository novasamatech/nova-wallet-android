package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.recyclerView.WithViewType
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.ItemMissingTokenBinding

/**
 * Closes the list with a way out for a token that is simply hidden: without it, a wallet that
 * starts on the default token list gives no hint that the rest exist.
 */
class MissingTokenAdapter(
    private val handler: Handler
) : SingleItemAdapter<MissingTokenHolder>(isShownByDefault = true) {

    interface Handler {

        fun manageTokensClicked()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MissingTokenHolder {
        return MissingTokenHolder(ItemMissingTokenBinding.inflate(parent.inflater(), parent, false), handler)
    }

    override fun onBindViewHolder(holder: MissingTokenHolder, position: Int) {
        // Static row
    }

    override fun getItemViewType(position: Int): Int {
        return MissingTokenHolder.viewType
    }
}

class MissingTokenHolder(
    binder: ItemMissingTokenBinding,
    handler: MissingTokenAdapter.Handler
) : RecyclerView.ViewHolder(binder.root) {

    companion object : WithViewType {

        override val viewType: Int = R.layout.item_missing_token
    }

    init {
        // The chevron makes the whole row the affordance, not just the label
        binder.root.setOnClickListener { handler.manageTokensClicked() }
    }
}
