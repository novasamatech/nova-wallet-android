package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.setPadding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.feature_governance_impl.R

class VotesViewOverview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), WithContextExtensions {

    override val providedContext: Context = context

    init {
        orientation = VERTICAL

        View.inflate(context, R.layout.view_votes_overview, this)

        background = getRoundedCornerDrawable(R.color.white_8)
        setPadding(16f.dp)
    }

}
