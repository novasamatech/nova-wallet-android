package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.start

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.view.setModelOrHide
import io.novafoundation.nova.feature_ledger_impl.R
import kotlinx.android.synthetic.main.item_import_ledger_start_page.view.startImportLedgerGuideLink
import kotlinx.android.synthetic.main.item_import_ledger_start_page.view.startImportLedgerInstructions
import kotlinx.android.synthetic.main.item_import_ledger_start_page.view.startImportLedgerWarning

class ConnectionModePageModel(
    val modeName: String,
    val guideItems: List<LedgerGuideItem>
)

class StartImportLedgerPagerAdapter(
    private val pages: List<ConnectionModePageModel>,
    private val handler: Handler
) : RecyclerView.Adapter<StartImportLedgerPageViewHolder>() {

    interface Handler {
        fun guideLinkClicked()
    }

    private var alertModel: AlertModel? = null

    override fun getItemCount(): Int {
        return pages.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StartImportLedgerPageViewHolder {
        return StartImportLedgerPageViewHolder(parent.inflateChild(R.layout.item_import_ledger_start_page), handler)
    }

    override fun onBindViewHolder(holder: StartImportLedgerPageViewHolder, position: Int) {
        holder.bind(pages[position], alertModel)
    }

    fun showWarning(alertModel: AlertModel?) {
        this.alertModel = alertModel
        repeat(pages.size) { notifyItemChanged(it) }
    }

    fun getPageTitle(position: Int): CharSequence {
        return pages[position].modeName
    }
}

class StartImportLedgerPageViewHolder(
    itemView: View,
    private val handler: StartImportLedgerPagerAdapter.Handler
) : ViewHolder(itemView) {

    private val adapter = StartImportLedgerInstructionAdapter()

    init {
        itemView.startImportLedgerInstructions.adapter = adapter
        itemView.startImportLedgerGuideLink.setOnClickListener { handler.guideLinkClicked() }
    }

    fun bind(page: ConnectionModePageModel, alertModel: AlertModel?) {
        adapter.submitList(page.guideItems)
        itemView.startImportLedgerWarning.setModelOrHide(alertModel)
    }
}
