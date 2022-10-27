package io.novafoundation.nova.feature_governance_impl.presentation.unlock.list

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.startTimer
import io.novafoundation.nova.common.view.stopTimer
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.unlock.list.model.GovernanceLockModel
import io.novafoundation.nova.feature_governance_impl.presentation.unlock.list.model.GovernanceLockModel.StatusContent
import kotlinx.android.synthetic.main.item_governance_lock.view.leftToUnlock
import kotlinx.android.synthetic.main.item_governance_lock.view.unlockableTokensAmount

class UnlockableTokensAdapter : ListAdapter<GovernanceLockModel, UnlockableTokenHolder>(GovernanceLockCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnlockableTokenHolder {
        return UnlockableTokenHolder(parent.inflateChild(R.layout.item_governance_lock, false))
    }

    override fun onBindViewHolder(holder: UnlockableTokenHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object GovernanceLockCallback : DiffUtil.ItemCallback<GovernanceLockModel>() {

    override fun areItemsTheSame(oldItem: GovernanceLockModel, newItem: GovernanceLockModel): Boolean {
        return oldItem.index == newItem.index
    }

    override fun areContentsTheSame(oldItem: GovernanceLockModel, newItem: GovernanceLockModel): Boolean {
        return oldItem == newItem
    }
}

class UnlockableTokenHolder(
    containerView: View,
) : RecyclerView.ViewHolder(containerView) {

    fun bind(item: GovernanceLockModel) = with(itemView) {
        unlockableTokensAmount.text = item.amount

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
