package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.R
import io.novafoundation.nova.common.utils.WithContextExtensions

class GoToNftsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions {

    override val providedContext: Context = context

    init {
        View.inflate(context, R.layout.view_go_to_nfts, this)

        background = addRipple(getRoundedCornerDrawable(R.color.black_48))
    }
}
