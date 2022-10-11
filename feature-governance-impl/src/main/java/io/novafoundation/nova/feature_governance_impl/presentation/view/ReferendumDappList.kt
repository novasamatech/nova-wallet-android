package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.feature_dapp_api.presentation.view.DAppView
import io.novafoundation.nova.feature_governance_impl.R

class ReferendumDappList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle), WithContextExtensions {

    override val providedContext: Context = context

    init {
        View.inflate(context, R.layout.view_referendum_dapp_list, this)
        background = getRoundedCornerDrawable(R.color.white_8)
        setPadding(16.dp)
    }

    fun addDApp(title: String, subtitle: String, iconUrl: String?) {
        val dAppView = DAppView.createMathParentWidth(context)
        dAppView.setTitle(title)
        dAppView.setSubtitle(subtitle)
        dAppView.setIconUrl(iconUrl)
        addView(dAppView)
    }
}
