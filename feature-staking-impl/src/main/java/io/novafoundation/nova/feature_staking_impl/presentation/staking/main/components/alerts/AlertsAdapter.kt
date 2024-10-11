package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.extensions.LayoutContainer

class AlertsAdapter : ListAdapter<AlertModel, AlertsAdapter.AlertViewHolder>(AlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = parent.inflateChild(R.layout.item_alert)

        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val item = getItem(position)

        holder.bind(item)
    }

    class AlertViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(alert: AlertModel) = with(containerView) {
            alertItemTitle.text = alert.title
            alertItemMessage.text = alert.extraMessage

            if (alert.type is AlertModel.Type.CallToAction) {
                alertItemGoToFlowIcon.makeVisible()

                setOnClickListener {
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
