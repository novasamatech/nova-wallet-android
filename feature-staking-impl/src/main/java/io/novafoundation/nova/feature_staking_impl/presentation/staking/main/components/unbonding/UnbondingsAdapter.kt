package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.removeCompoundDrawables
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.common.view.stopTimer
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_unbonding.view.itemUnbondAmount
import kotlinx.android.synthetic.main.item_unbonding.view.itemUnbondStatus
import kotlin.time.ExperimentalTime

class UnbondingsAdapter : ListAdapter<UnbondingModel, UnbondingsHolder>(UnbondingModelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnbondingsHolder {
        val view = parent.inflateChild(R.layout.item_unbonding)

        return UnbondingsHolder(view)
    }

    override fun onBindViewHolder(holder: UnbondingsHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)

        resolvePayload(holder, position, payloads) {
            when (it) {
                UnbondingModel::status -> holder.bindStatus(item)
            }
        }
    }

    @ExperimentalTime
    override fun onBindViewHolder(holder: UnbondingsHolder, position: Int) {
        val item = getItem(position)

        holder.bind(item)
    }
}

class UnbondingsHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    @ExperimentalTime
    fun bind(unbonding: UnbondingModel) = with(containerView) {
        bindStatus(unbonding)

        itemUnbondAmount.text = unbonding.amountModel.token
    }

    fun bindStatus(unbonding: UnbondingModel) = with(containerView) {
        when (val status = unbonding.status) {
            Unbonding.Status.Redeemable -> {
                itemUnbondStatus.setTextColorRes(R.color.text_positive)
                itemUnbondStatus.removeCompoundDrawables()
                itemUnbondStatus.stopTimer()
                itemUnbondStatus.setText(R.string.wallet_balance_redeemable)
            }
            is Unbonding.Status.Unbonding -> {
                itemUnbondStatus.setTextColorRes(R.color.text_tertiary)
                itemUnbondStatus.setDrawableEnd(R.drawable.ic_time_16, paddingInDp = 4, tint = R.color.icon_secondary)

                itemUnbondStatus.startTimer(status.timeLeft, status.calculatedAt)
            }
        }
    }
}

private val PAYLOAD_GENERATOR = PayloadGenerator(UnbondingModel::status)

private class UnbondingModelDiffCallback : DiffUtil.ItemCallback<UnbondingModel>() {

    override fun areItemsTheSame(oldItem: UnbondingModel, newItem: UnbondingModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: UnbondingModel, newItem: UnbondingModel): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: UnbondingModel, newItem: UnbondingModel): Any? {
        return PAYLOAD_GENERATOR.diff(oldItem, newItem)
    }
}
