package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.feature_staking_impl.databinding.ItemBittensorSubnetFailureBinding
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorInlineFailure
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.setBittensorFailure

/** The failed catalogue, shown below the root row as on iOS. */
class BittensorSubnetFailureAdapter(private val onRetry: () -> Unit) : SingleItemAdapter<BittensorSubnetFailureAdapter.Holder>() {

    private var failure: BittensorInlineFailure? = null

    fun setFailure(failure: BittensorInlineFailure?) {
        this.failure = failure
        notifyChangedIfShown()
        show(failure != null)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(ItemBittensorSubnetFailureBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.failure.setBittensorFailure(failure, onRetry)
    }

    class Holder(val binding: ItemBittensorSubnetFailureBinding) : RecyclerView.ViewHolder(binding.root)
}
