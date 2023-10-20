package io.novafoundation.nova.feature_staking_impl.presentation.validators

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.PayloadGenerator
import io.novafoundation.nova.common.list.resolvePayload
import io.novafoundation.nova.common.presentation.setColoredText
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.StakeTargetModel
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_validator.view.itemStakingTargetCheck
import kotlinx.android.synthetic.main.item_validator.view.itemStakingTargetActionIcon
import kotlinx.android.synthetic.main.item_validator.view.itemStakingTargetIcon
import kotlinx.android.synthetic.main.item_validator.view.itemStakingTargetInfo
import kotlinx.android.synthetic.main.item_validator.view.itemStakingTargetName
import kotlinx.android.synthetic.main.item_validator.view.itemStakingTargetScoringPrimary
import kotlinx.android.synthetic.main.item_validator.view.itemStakingTargetScoringSecondary
import kotlinx.android.synthetic.main.item_validator.view.itemStakingTargetSubtitleLabel
import kotlinx.android.synthetic.main.item_validator.view.itemStakingTargetSubtitleValue

class StakeTargetAdapter<V>(
    private val itemHandler: ItemHandler<V>,
    initialMode: Mode = Mode.VIEW
) : ListAdapter<StakeTargetModel<V>, StakingTargetViewHolder<V>>(StakingTargetDiffCallback()) {

    private var mode = initialMode

    interface ItemHandler<V> {

        fun stakeTargetInfoClicked(stakeTargetModel: StakeTargetModel<V>)

        fun stakeTargetClicked(stakeTargetModel: StakeTargetModel<V>) {
            // default empty
        }

        fun removeClicked(StakeTargetModel: StakeTargetModel<V>) {
            // default empty
        }
    }

    enum class Mode {
        VIEW, EDIT
    }

    fun modeChanged(newMode: Mode) {
        mode = newMode

        notifyItemRangeChanged(0, itemCount, mode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StakingTargetViewHolder<V> {
        val view = parent.inflateChild(R.layout.item_validator)

        return StakingTargetViewHolder(view)
    }

    override fun onBindViewHolder(holder: StakingTargetViewHolder<V>, position: Int) {
        val item = getItem(position)

        holder.bind(item, itemHandler, mode)
    }

    override fun onBindViewHolder(holder: StakingTargetViewHolder<V>, position: Int, payloads: MutableList<Any>) {
        val item = getItem(position)

        resolvePayload(
            holder,
            position,
            payloads,
            onUnknownPayload = { holder.bindIcon(mode, item, itemHandler) },
            onDiffCheck = {
                when (it) {
                    StakeTargetModel<*>::isChecked -> holder.bindIcon(mode, item, itemHandler)
                    StakeTargetModel<*>::scoring -> holder.bindScoring(item)
                    StakeTargetModel<*>::subtitle -> holder.bindSubtitle(item)
                }
            }
        )
    }
}

class StakingTargetViewHolder<V>(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(
        stakeTargetModel: StakeTargetModel<V>,
        itemHandler: StakeTargetAdapter.ItemHandler<V>,
        mode: StakeTargetAdapter.Mode
    ) = with(containerView) {
        itemStakingTargetName.text = stakeTargetModel.addressModel.nameOrAddress
        itemStakingTargetIcon.setImageDrawable(stakeTargetModel.addressModel.image)

        itemStakingTargetInfo.setOnClickListener {
            itemHandler.stakeTargetInfoClicked(stakeTargetModel)
        }

        setOnClickListener {
            itemHandler.stakeTargetClicked(stakeTargetModel)
        }

        bindIcon(mode, stakeTargetModel, itemHandler)

        bindScoring(stakeTargetModel)
        bindSubtitle(stakeTargetModel)
    }

    fun bindIcon(
        mode: StakeTargetAdapter.Mode,
        StakeTargetModel: StakeTargetModel<V>,
        handler: StakeTargetAdapter.ItemHandler<V>
    ) = with(containerView) {
        when {
            mode == StakeTargetAdapter.Mode.EDIT -> {
                itemStakingTargetActionIcon.makeVisible()
                itemStakingTargetCheck.makeGone()

                itemStakingTargetActionIcon.setOnClickListener { handler.removeClicked(StakeTargetModel) }
            }

            StakeTargetModel.isChecked == null -> {
                itemStakingTargetActionIcon.makeGone()
                itemStakingTargetCheck.makeGone()
            }

            else -> {
                itemStakingTargetActionIcon.makeGone()
                itemStakingTargetCheck.makeVisible()

                itemStakingTargetCheck.isChecked = StakeTargetModel.isChecked
            }
        }
    }

    fun bindScoring(StakeTargetModel: StakeTargetModel<*>) = with(containerView) {
        when (val scoring = StakeTargetModel.scoring) {
            null -> {
                itemStakingTargetScoringPrimary.makeGone()
                itemStakingTargetScoringSecondary.makeGone()
            }

            is StakeTargetModel.Scoring.OneField -> {
                itemStakingTargetScoringPrimary.setTextColorRes(R.color.text_tertiary)
                itemStakingTargetScoringPrimary.makeVisible()
                itemStakingTargetScoringSecondary.makeGone()
                itemStakingTargetScoringPrimary.setColoredText(scoring.field)
            }

            is StakeTargetModel.Scoring.TwoFields -> {
                itemStakingTargetScoringPrimary.setTextColorRes(R.color.text_primary)
                itemStakingTargetScoringPrimary.makeVisible()
                itemStakingTargetScoringSecondary.makeVisible()
                itemStakingTargetScoringPrimary.text = scoring.primary
                itemStakingTargetScoringSecondary.text = scoring.secondary
            }
        }
    }

    fun bindSubtitle(item: StakeTargetModel<*>) = with(containerView) {
        if (item.subtitle != null) {
            itemStakingTargetSubtitleLabel.makeVisible()
            itemStakingTargetSubtitleValue.makeVisible()

            itemStakingTargetSubtitleLabel.text = item.subtitle.label

            itemStakingTargetSubtitleValue.text = item.subtitle.value.text
            itemStakingTargetSubtitleValue.setTextColorRes(item.subtitle.value.colorRes)
        } else {
            itemStakingTargetSubtitleValue.makeGone()
            itemStakingTargetSubtitleLabel.makeGone()
        }
    }
}

class StakingTargetDiffCallback<V> : DiffUtil.ItemCallback<StakeTargetModel<V>>() {

    override fun areItemsTheSame(oldItem: StakeTargetModel<V>, newItem: StakeTargetModel<V>): Boolean {
        return oldItem.accountIdHex == newItem.accountIdHex
    }

    override fun areContentsTheSame(oldItem: StakeTargetModel<V>, newItem: StakeTargetModel<V>): Boolean {
        return oldItem.scoring == newItem.scoring && oldItem.isChecked == newItem.isChecked && oldItem.subtitle == newItem.subtitle
    }

    override fun getChangePayload(oldItem: StakeTargetModel<V>, newItem: StakeTargetModel<V>): Any? {
        return StakingTargetPayloadGenerator.diff(oldItem, newItem)
    }
}

private object StakingTargetPayloadGenerator : PayloadGenerator<StakeTargetModel<*>>(
    StakeTargetModel<*>::isChecked,
    StakeTargetModel<*>::scoring,
    StakeTargetModel<*>::subtitle
)
