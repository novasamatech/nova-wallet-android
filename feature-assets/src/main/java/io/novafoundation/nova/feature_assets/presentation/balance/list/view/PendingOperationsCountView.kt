package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.addRipple
import io.novafoundation.nova.common.utils.getRoundedCornerDrawable
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.ViewPendingOperationsCountBinding

class PendingOperationsCountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewPendingOperationsCountBinding.inflate(inflater(), this)

    init {
        background = addRipple(getRoundedCornerDrawable(R.color.block_background))
    }

    fun setPendingOperationsCount(model: PendingOperationsCountModel) {
        when (model) {
            PendingOperationsCountModel.Gone -> makeGone()
            is PendingOperationsCountModel.Visible -> {
                makeVisible()
                binding.pendingOperationsCountCounter.text = model.countLabel
            }
        }
    }
}

sealed class PendingOperationsCountModel {

    data object Gone : PendingOperationsCountModel()

    class Visible(val countLabel: String) : PendingOperationsCountModel()
}
