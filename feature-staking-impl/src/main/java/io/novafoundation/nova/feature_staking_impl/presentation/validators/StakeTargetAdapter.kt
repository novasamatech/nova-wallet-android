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
import kotlinx.android.synthetic.main.item_validator.view.itemValidationCheck
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorActionIcon
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorIcon
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorInfo
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorName
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorScoringPrimary
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorScoringSecondary
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorSubtitleLabel
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorSubtitleValue

class StakeTargetAdapter<V>(
    private val itemHandler: ItemHandler<V>,
    initialMode: Mode = Mode.VIEW
) : ListAdapter<StakeTargetModel<V>, ValidatorViewHolder<V>>(ValidatorDiffCallback()) {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ValidatorViewHolder<V> {
        val view = parent.inflateChild(R.layout.item_validator)

        return ValidatorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ValidatorViewHolder<V>, position: Int) {
        val item = getItem(position)

        holder.bind(item, itemHandler, mode)
    }

    override fun onBindViewHolder(holder: ValidatorViewHolder<V>, position: Int, payloads: MutableList<Any>) {
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

class ValidatorViewHolder<V>(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(
        stakeTargetModel: StakeTargetModel<V>,
        itemHandler: StakeTargetAdapter.ItemHandler<V>,
        mode: StakeTargetAdapter.Mode
    ) = with(containerView) {
        itemValidatorName.text = stakeTargetModel.addressModel.nameOrAddress
        itemValidatorIcon.setImageDrawable(stakeTargetModel.addressModel.image)

        itemValidatorInfo.setOnClickListener {
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
                itemValidatorActionIcon.makeVisible()
                itemValidationCheck.makeGone()

                itemValidatorActionIcon.setOnClickListener { handler.removeClicked(StakeTargetModel) }
            }
            StakeTargetModel.isChecked == null -> {
                itemValidatorActionIcon.makeGone()
                itemValidationCheck.makeGone()
            }
            else -> {
                itemValidatorActionIcon.makeGone()
                itemValidationCheck.makeVisible()

                itemValidationCheck.isChecked = StakeTargetModel.isChecked
            }
        }
    }

    fun bindScoring(StakeTargetModel: StakeTargetModel<*>) = with(containerView) {
        when (val scoring = StakeTargetModel.scoring) {
            null -> {
                itemValidatorScoringPrimary.makeGone()
                itemValidatorScoringSecondary.makeGone()
            }

            is StakeTargetModel.Scoring.OneField -> {
                itemValidatorScoringPrimary.setTextColorRes(R.color.text_tertiary)
                itemValidatorScoringPrimary.makeVisible()
                itemValidatorScoringSecondary.makeGone()
                itemValidatorScoringPrimary.setColoredText(scoring.field)
            }

            is StakeTargetModel.Scoring.TwoFields -> {
                itemValidatorScoringPrimary.setTextColorRes(R.color.text_primary)
                itemValidatorScoringPrimary.makeVisible()
                itemValidatorScoringSecondary.makeVisible()
                itemValidatorScoringPrimary.text = scoring.primary
                itemValidatorScoringSecondary.text = scoring.secondary
            }
        }
    }

    fun bindSubtitle(item: StakeTargetModel<*>) = with(containerView) {
        if (item.subtitle != null) {
            itemValidatorSubtitleLabel.makeVisible()
            itemValidatorSubtitleValue.makeVisible()

            itemValidatorSubtitleLabel.text = item.subtitle.label

            itemValidatorSubtitleValue.text = item.subtitle.value.text
            itemValidatorSubtitleValue.setTextColorRes(item.subtitle.value.colorRes)
        } else {
            itemValidatorSubtitleValue.makeGone()
            itemValidatorSubtitleLabel.makeGone()
        }
    }
}

class ValidatorDiffCallback<V> : DiffUtil.ItemCallback<StakeTargetModel<V>>() {

    override fun areItemsTheSame(oldItem: StakeTargetModel<V>, newItem: StakeTargetModel<V>): Boolean {
        return oldItem.accountIdHex == newItem.accountIdHex
    }

    override fun areContentsTheSame(oldItem: StakeTargetModel<V>, newItem: StakeTargetModel<V>): Boolean {
        return oldItem.scoring == newItem.scoring && oldItem.isChecked == newItem.isChecked && oldItem.subtitle == newItem.subtitle
    }

    override fun getChangePayload(oldItem: StakeTargetModel<V>, newItem: StakeTargetModel<V>): Any? {
        return ValidatorPayloadGenerator.diff(oldItem, newItem)
    }
}

private object ValidatorPayloadGenerator : PayloadGenerator<StakeTargetModel<*>>(
    StakeTargetModel<*>::isChecked,
    StakeTargetModel<*>::scoring,
    StakeTargetModel<*>::subtitle
)
