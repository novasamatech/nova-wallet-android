package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_staking_impl.R

class AlertsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    private val alertsAdapter = AlertsAdapter()

    init {
        View.inflate(context, R.layout.view_alerts, this)

        orientation = VERTICAL

        with(context) {
            background = getBlockDrawable()
        }

        alertsRecycler.adapter = alertsAdapter
    }

    fun setStatus(alerts: List<AlertModel>) {
        if (alerts.isEmpty()) {
            makeGone()
        } else {
            makeVisible()

            alertShimmer.makeGone()
            alertsRecycler.makeVisible()

            alertsAdapter.submitList(alerts)
        }
    }

    fun showLoading() {
        alertShimmer.makeVisible()
        alertsRecycler.makeGone()
    }
}

fun AlertsView.setState(alertsState: AlertsState?) {
    when (alertsState) {
        is LoadingState.Loaded -> setStatus(alertsState.data)
        is LoadingState.Loading -> showLoading()
        null -> makeGone()
    }
}
