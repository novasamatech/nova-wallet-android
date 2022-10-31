package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.core.view.isGone
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.feature_dapp_api.presentation.view.DAppView
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.model.ReferendumDAppModel

typealias OnDAppClicked = (ReferendumDAppModel) -> Unit

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

    fun setDApps(dApps: List<ReferendumDAppModel>) {
        removeAllDApps()

        dApps.forEach(::addDApp)
    }

    fun setDAppsOrHide(dApps: List<ReferendumDAppModel>) {
        isGone = dApps.isEmpty()
        setDApps(dApps)
    }

    private fun addDApp(model: ReferendumDAppModel) {
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

    private fun removeAllDApps() {
        children.toList()
            .filterIsInstance<DAppView>()
            .forEach { removeView(it) }
    }
}
