package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_staking_impl.databinding.ItemAlertBinding

class AlertsAdapter : ListAdapter<AlertModel, AlertsAdapter.AlertViewHolder>(AlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        return AlertViewHolder(ItemAlertBinding.inflate(parent.inflater(), parent, false))
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val item = getItem(position)

        holder.bind(item)
    }

    class AlertViewHolder(private val binder: ItemAlertBinding) : RecyclerView.ViewHolder(binder.root) {

        fun bind(alert: AlertModel) = with(binder) {
            alertItemTitle.text = alert.title
            alertItemMessage.text = alert.extraMessage

            if (alert.type is AlertModel.Type.CallToAction) {
                alertItemGoToFlowIcon.makeVisible()

                root.setOnClickListener {
                    alert.type.action()
                }
            } else {
                alertItemGoToFlowIcon.makeGone()
            }
        }
    }
}

private class AlertDiffCallback : DiffUtil.ItemCallback<AlertModel>() {
    override fun areItemsTheSame(oldItem: AlertModel, newItem: AlertModel): Boolean {
        return oldItem.title == newItem.title && oldItem.extraMessage == newItem.extraMessage
    }

    override fun areContentsTheSame(oldItem: AlertModel, newItem: AlertModel): Boolean {
        return true
    }
}
