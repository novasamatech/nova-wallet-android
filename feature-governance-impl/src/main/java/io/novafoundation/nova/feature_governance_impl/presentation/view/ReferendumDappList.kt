package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.feature_dapp_api.presentation.view.DAppView
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.GovernanceDAppModel

typealias OnDAppClicked = (GovernanceDAppModel) -> Unit

class ReferendumDappList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    init {
        View.inflate(context, R.layout.view_referendum_dapp_list, this)
        background = getRoundedCornerDrawable(R.color.white_8)
        orientation = VERTICAL
        setPadding(0, 16.dp, 0, 8.dp)
    }

    private var onDAppClicked: OnDAppClicked? = null

    fun onDAppClicked(listener: OnDAppClicked) {
        onDAppClicked = listener
    }

    fun setDApps(dApps: List<GovernanceDAppModel>) {
        removeAllViews()

        dApps.forEach(::addDApp)
    }

    private fun addDApp(model: GovernanceDAppModel) {
        val dAppView = DAppView.createUsingMathParentWidth(context).apply {
            setTitle(model.name)
            setSubtitle(model.description)
            setIconUrl(model.iconUrl)
            setActionResource(iconRes = R.drawable.ic_chevron_right, colorRes = R.color.white_48)
            setActionEndPadding(16.dp)

            setOnClickListener { onDAppClicked?.invoke(model) }
        }

        addView(dAppView)
    }
}
