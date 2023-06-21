package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common.list

import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_staking_impl.R

class DashboardSectionAdapter(
    private val textRes: Int
) : RecyclerView.Adapter<DashboardSectionHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardSectionHolder {
        val item = parent.inflateChild(R.layout.item_dashboard_section) as TextView

        return DashboardSectionHolder(item, textRes)
    }

    override fun getItemCount(): Int = 1

    override fun onBindViewHolder(holder: DashboardSectionHolder, position: Int) {}
}

class DashboardSectionHolder(
    containerView: TextView,
    @StringRes textRes: Int
) : RecyclerView.ViewHolder(containerView) {

    init {
        containerView.setText(textRes)
    }
}
