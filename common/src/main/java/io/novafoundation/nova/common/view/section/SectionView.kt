package io.novafoundation.nova.common.view.section

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable

abstract class SectionView(
    layoutId: Int,
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions {

    override val providedContext: Context = context

    init {
        View.inflate(context, layoutId, this)

        background = with(context) {
            addRipple(getRoundedCornerDrawable(R.color.block_background))
        }
    }
}
