package io.novafoundation.nova.feature_governance_impl.presentation.unlock.list

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.common.view.stopTimer
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.ItemGovernanceLockBinding
import io.novafoundation.nova.feature_governance_impl.presentation.unlock.list.model.GovernanceLockModel
import io.novafoundation.nova.feature_governance_impl.presentation.unlock.list.model.GovernanceLockModel.StatusContent

class UnlockableTokensAdapter : ListAdapter<GovernanceLockModel, UnlockableTokenHolder>(GovernanceLockCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnlockableTokenHolder {
        return UnlockableTokenHolder(ItemGovernanceLockBinding.inflate(parent.inflater(), parent, false))
    }

    override fun onBindViewHolder(holder: UnlockableTokenHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: UnlockableTokenHolder, position: Int, payloads: MutableList<Any>) {
        resolvePayload(holder, position, payloads) {
            val item = getItem(position)

            when (it) {
                GovernanceLockModel::amount -> holder.bindUnlockAmount(item)
                GovernanceLockModel::status -> holder.bindUnlockStatus(item)
            }
        }
    }
}

private val LocksPayloadGenerator = PayloadGenerator(
    GovernanceLockModel::amount,
    GovernanceLockModel::status
)

private object GovernanceLockCallback : DiffUtil.ItemCallback<GovernanceLockModel>() {

    override fun areItemsTheSame(oldItem: GovernanceLockModel, newItem: GovernanceLockModel): Boolean {
        return oldItem.index == newItem.index
    }

    override fun areContentsTheSame(oldItem: GovernanceLockModel, newItem: GovernanceLockModel): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: GovernanceLockModel, newItem: GovernanceLockModel): Any? {
        return LocksPayloadGenerator.diff(oldItem, newItem)
    }
}

class UnlockableTokenHolder(
    private val binder: ItemGovernanceLockBinding,
) : RecyclerView.ViewHolder(binder.root) {

    fun bind(item: GovernanceLockModel) = with(binder) {
        bindUnlockAmount(item)

        bindUnlockStatus(item)
    }

    fun bindUnlockAmount(item: GovernanceLockModel) = with(binder) {
        unlockableTokensAmount.text = item.amount
    }

    fun bindUnlockStatus(item: GovernanceLockModel) = with(binder) {
        when (val status = item.status) {
            is StatusContent.Text -> {
                leftToUnlock.stopTimer()

                leftToUnlock.text = status.text
            }
            is StatusContent.Timer -> {
                leftToUnlock.startTimer(value = status.timer, customMessageFormat = R.string.common_left)
            }
        }

        leftToUnlock.setDrawableEnd(item.statusIconRes, widthInDp = 16, paddingInDp = 4, tint = item.statusIconColorRes)
        leftToUnlock.setTextColorRes(item.statusColorRes)
    }
}
