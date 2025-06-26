package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.start

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.view.setModelOrHide
import io.novafoundation.nova.feature_ledger_impl.databinding.ItemImportLedgerStartPageBinding

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
        val binder = ItemImportLedgerStartPageBinding.inflate(parent.inflater(), parent, false)
        return StartImportLedgerPageViewHolder(binder, handler)
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
    private val binder: ItemImportLedgerStartPageBinding,
    private val handler: StartImportLedgerPagerAdapter.Handler
) : ViewHolder(binder.root) {

    private val adapter = StartImportLedgerInstructionAdapter()

    init {
        binder.startImportLedgerInstructions.adapter = adapter
        binder.startImportLedgerGuideLink.setOnClickListener { handler.guideLinkClicked() }
    }

    fun bind(page: ConnectionModePageModel, alertModel: AlertModel?) {
        adapter.submitList(page.guideItems)
        binder.startImportLedgerWarning.setModelOrHide(alertModel)
    }
}
