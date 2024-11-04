package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_staking_impl.databinding.ViewAlertsBinding

class AlertsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    private val alertsAdapter = AlertsAdapter()

    private val binder = ViewAlertsBinding.inflate(inflater(), this)

    init {
        orientation = VERTICAL

        with(context) {
            background = getBlockDrawable()
        }

        binder.alertsRecycler.adapter = alertsAdapter
    }

    fun setStatus(alerts: List<AlertModel>) {
        if (alerts.isEmpty()) {
            makeGone()
        } else {
            makeVisible()

            binder.alertShimmer.makeGone()
            binder.alertsRecycler.makeVisible()

            alertsAdapter.submitList(alerts)
        }
    }

    fun showLoading() {
        binder.alertShimmer.makeVisible()
        binder.alertsRecycler.makeGone()
    }
}

fun AlertsView.setState(alertsState: AlertsState?) {
    when (alertsState) {
        is LoadingState.Loaded -> setStatus(alertsState.data)
        is LoadingState.Loading -> showLoading()
        null -> makeGone()
    }
}
