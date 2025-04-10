package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.removeCompoundDrawables
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.common.view.stopTimer
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ItemUnbondingBinding
import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding

import kotlin.time.ExperimentalTime

class UnbondingsAdapter : ListAdapter<UnbondingModel, UnbondingsHolder>(UnbondingModelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnbondingsHolder {
        return UnbondingsHolder(ItemUnbondingBinding.inflate(parent.inflater(), parent, false))
    }

    override fun onBindViewHolder(holder: UnbondingsHolder, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)

        resolvePayload(holder, position, payloads) {
            when (it) {
                UnbondingModel::status -> holder.bindStatus(item)
                UnbondingModel::amountModel -> holder.bindAmount(item)
            }
        }
    }

    @ExperimentalTime
    override fun onBindViewHolder(holder: UnbondingsHolder, position: Int) {
        val item = getItem(position)

        holder.bind(item)
    }
}

class UnbondingsHolder(private val binder: ItemUnbondingBinding) : RecyclerView.ViewHolder(binder.root) {

    fun bind(unbonding: UnbondingModel) {
        bindStatus(unbonding)
        bindAmount(unbonding)
    }

    fun bindAmount(unbonding: UnbondingModel) {
        binder.itemUnbondAmount.text = unbonding.amountModel.token
    }

    fun bindStatus(unbonding: UnbondingModel) = with(binder) {
        when (val status = unbonding.status) {
            Unbonding.Status.Redeemable -> {
                itemUnbondStatus.setTextColorRes(R.color.text_positive)
                itemUnbondStatus.removeCompoundDrawables()
                itemUnbondStatus.stopTimer()
                itemUnbondStatus.setText(R.string.wallet_balance_redeemable)
            }
            is Unbonding.Status.Unbonding -> {
                itemUnbondStatus.setTextColorRes(R.color.text_secondary)
                itemUnbondStatus.setDrawableEnd(R.drawable.ic_time_16, paddingInDp = 4, tint = R.color.icon_secondary)

                itemUnbondStatus.startTimer(status.timer)
            }
        }
    }
}

private val PAYLOAD_GENERATOR = PayloadGenerator(UnbondingModel::status, UnbondingModel::amountModel)

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
