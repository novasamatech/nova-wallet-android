package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.list

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.BaseViewHolder
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.PrimaryButton
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_ledger_impl.R

class LedgerSelectAddressLoadMoreAdapter(
    private val handler: Handler,
    private val lifecycleOwner: LifecycleOwner,
) : RecyclerView.Adapter<LedgerSelectAddressLoadMoreViewHolder>() {

    interface Handler {

        fun loadMoreClicked()
    }

    private var state: DescriptiveButtonState? = null

    fun setState(newState: DescriptiveButtonState) {
        state = newState

        notifyItemChanged(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LedgerSelectAddressLoadMoreViewHolder {
        val containerView = parent.inflateChild(R.layout.item_select_address_load_more) as PrimaryButton

        return LedgerSelectAddressLoadMoreViewHolder(containerView, handler, lifecycleOwner)
    }

    override fun onBindViewHolder(holder: LedgerSelectAddressLoadMoreViewHolder, position: Int) {
        state?.let(holder::bind)
    }

    override fun getItemCount(): Int = 1
}

class LedgerSelectAddressLoadMoreViewHolder(
    override val containerView: PrimaryButton,
    handler: LedgerSelectAddressLoadMoreAdapter.Handler,
    lifecycleOwner: LifecycleOwner,
) : BaseViewHolder(containerView) {

    init {
        containerView.prepareForProgress(lifecycleOwner)
        containerView.setOnClickListener { handler.loadMoreClicked() }
    }

    fun bind(state: DescriptiveButtonState) {
        containerView.setState(state)
    }

    override fun unbind() {}
}
