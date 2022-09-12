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
import kotlinx.android.synthetic.main.item_validator.view.itemValidationCheck
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorActionIcon
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorIcon
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorInfo
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorName
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorSubtitleLabel
import kotlinx.android.synthetic.main.item_validator.view.itemValidatorSubtitleValue

class SelectCollatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    init {
        View.inflate(context, R.layout.item_validator, this)

        background = getRoundedCornerDrawable(R.color.white_8).withRipple()
        clipToOutline = true

        itemValidationCheck.makeGone()
        itemValidatorActionIcon.makeGone()
        itemValidatorSubtitleLabel.makeGone()

        itemValidatorInfo.setImageResource(R.drawable.ic_chevron_right)
        itemValidatorInfo.setImageTintRes(R.color.white_48)

        setSelectedCollator(null)
    }

    fun setSelectedCollator(selectedCollator: SelectCollatorModel?) {
        if (selectedCollator == null) {
            itemValidatorName.setText(R.string.staking_parachain_select_collator)
            itemValidatorIcon.setImageResource(R.drawable.ic_identicon_placeholder)
        } else {
            bindSelectedCollator(selectedCollator)
        }
    }
}

fun View.bindSelectedCollator(selectedCollator: SelectStakeTargetModel<*>) {
    itemValidatorName.text = selectedCollator.addressModel.nameOrAddress
    itemValidatorIcon.setImageDrawable(selectedCollator.addressModel.image)

    bindSubtitle(selectedCollator.subtitle)
}

private fun View.bindSubtitle(subtitle: CharSequence?) {
    itemValidatorSubtitleValue.setVisible(subtitle != null)

    if (subtitle != null) {
        itemValidatorSubtitleValue.text = subtitle
    }
}
