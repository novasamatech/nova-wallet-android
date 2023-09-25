package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.SelectStakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.model.SelectCollatorModel
import kotlinx.android.synthetic.main.item_validator.view.itemStakingTargetCheck
import kotlinx.android.synthetic.main.item_validator.view.itemStakingTargetActionIcon
import kotlinx.android.synthetic.main.item_validator.view.itemStakingTargetIcon
import kotlinx.android.synthetic.main.item_validator.view.itemStakingTargetInfo
import kotlinx.android.synthetic.main.item_validator.view.itemStakingTargetName
import kotlinx.android.synthetic.main.item_validator.view.itemStakingTargetSubtitleLabel
import kotlinx.android.synthetic.main.item_validator.view.itemStakingTargetSubtitleValue

class SelectCollatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    init {
        View.inflate(context, R.layout.item_validator, this)

        background = getRoundedCornerDrawable(R.color.block_background).withRippleMask()
        clipToOutline = true

        itemStakingTargetCheck.makeGone()
        itemStakingTargetActionIcon.makeGone()
        itemStakingTargetSubtitleLabel.makeGone()

        itemStakingTargetInfo.setImageResource(R.drawable.ic_chevron_right)
        itemStakingTargetInfo.setImageTintRes(R.color.icon_secondary)

        setSelectedCollator(null)
    }

    fun setSelectedCollator(selectedCollator: SelectCollatorModel?) {
        if (selectedCollator == null) {
            itemStakingTargetName.setText(R.string.staking_parachain_select_collator)
            itemStakingTargetIcon.setImageResource(R.drawable.ic_identicon_placeholder)
        } else {
            bindSelectedCollator(selectedCollator)
        }
    }
}

fun View.bindSelectedCollator(selectedCollator: SelectStakeTargetModel<*>) {
    itemStakingTargetName.text = selectedCollator.addressModel.nameOrAddress
    itemStakingTargetIcon.setImageDrawable(selectedCollator.addressModel.image)

    bindSubtitle(selectedCollator.subtitle)
}

private fun View.bindSubtitle(subtitle: CharSequence?) {
    itemStakingTargetSubtitleValue.setVisible(subtitle != null)

    if (subtitle != null) {
        itemStakingTargetSubtitleValue.text = subtitle
    }
}
