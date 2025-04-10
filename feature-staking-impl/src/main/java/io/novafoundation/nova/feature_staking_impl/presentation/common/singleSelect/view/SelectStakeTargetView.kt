package io.novafoundation.nova.feature_staking_impl.presentation.common.singleSelect.view

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ItemValidatorBinding
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.SelectStakeTargetModel

class SelectStakeTargetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    val binder = ItemValidatorBinding.inflate(inflater(), this, true)

    private var unselectedLabel: Int = R.string.staking_parachain_select_collator

    init {
        background = getRoundedCornerDrawable(R.color.block_background).withRippleMask()
        clipToOutline = true

        binder.itemStakingTargetCheck.makeGone()
        binder.itemStakingTargetActionIcon.makeGone()
        binder.itemStakingTargetSubtitleLabel.makeGone()

        binder.itemStakingTargetInfo.setImageResource(R.drawable.ic_chevron_right)
        binder.itemStakingTargetInfo.setImageTintRes(R.color.icon_secondary)

        setSelectedTarget(null)
    }

    fun setSelectedTarget(selectedTarget: SelectStakeTargetModel<*>?) {
        if (selectedTarget == null) {
            binder.itemStakingTargetName.setText(unselectedLabel)
            binder.itemStakingTargetIcon.setImageResource(R.drawable.ic_identicon_placeholder)
        } else {
            binder.bindSelectedCollator(selectedTarget)
        }
    }

    fun setUnselectedLabel(@StringRes label: Int) {
        unselectedLabel = label
    }
}

fun ItemValidatorBinding.bindSelectedCollator(selectedCollator: SelectStakeTargetModel<*>) {
    itemStakingTargetName.text = selectedCollator.addressModel.nameOrAddress
    itemStakingTargetIcon.setImageDrawable(selectedCollator.addressModel.image)

    bindSubtitle(selectedCollator.subtitle)
}

private fun ItemValidatorBinding.bindSubtitle(subtitle: CharSequence?) {
    itemStakingTargetSubtitleValue.setVisible(subtitle != null)

    if (subtitle != null) {
        itemStakingTargetSubtitleValue.text = subtitle
    }
}
