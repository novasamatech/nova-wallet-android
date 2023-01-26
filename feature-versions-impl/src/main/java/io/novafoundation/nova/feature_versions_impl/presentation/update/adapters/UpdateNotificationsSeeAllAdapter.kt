package io.novafoundation.nova.feature_versions_impl.presentation.update.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.GroupedListHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.feature_versions_impl.R

class UpdateNotificationsSeeAllAdapter(private val seeAllClickedListener: SeeAllClickedListener) : RecyclerView.Adapter<SeeAllButtonHolder>() {

    interface SeeAllClickedListener {
        fun onSeeAllClicked()
    }

    private var showBanner: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeeAllButtonHolder {
        return SeeAllButtonHolder(parent.inflateChild(R.layout.item_update_notification_see_all), seeAllClickedListener)
    }

    override fun getItemCount(): Int {
        return if (showBanner) 1 else 0
    }

    override fun onBindViewHolder(holder: SeeAllButtonHolder, position: Int) {}

    fun showButton(show: Boolean) {
        if (showBanner != show) {
            showBanner = show
            if (showBanner) {
                notifyItemInserted(0)
            } else {
                notifyItemRemoved(0)
            }
        }
    }
}

class SeeAllButtonHolder(view: View, seeAllClickedListener: UpdateNotificationsSeeAllAdapter.SeeAllClickedListener) : GroupedListHolder(view) {

    init {
        view.setOnClickListener { seeAllClickedListener.onSeeAllClicked() }
    }
}
