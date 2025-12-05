package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.start

import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.novafoundation.nova.common.list.instruction.InstructionAdapter
import io.novafoundation.nova.common.list.instruction.InstructionItem
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_account_impl.databinding.ItemImportParitySignerPageBinding

class ParitySignerPageModel(
    val modeName: String,
    val guideItems: List<InstructionItem>
)

class StartImportParitySignerPagerAdapter(
    private val pages: List<ParitySignerPageModel>
) : RecyclerView.Adapter<StartImportParitySignerViewHolder>() {

    @DrawableRes
    private var targetImage: Int? = null

    override fun getItemCount(): Int {
        return pages.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StartImportParitySignerViewHolder {
        val binder = ItemImportParitySignerPageBinding.inflate(parent.inflater(), parent, false)
        return StartImportParitySignerViewHolder(binder)
    }

    override fun onBindViewHolder(holder: StartImportParitySignerViewHolder, position: Int) {
        holder.bind(pages[position], targetImage)
    }

    fun getPageTitle(position: Int): CharSequence {
        return pages[position].modeName
    }

    fun setTargetImage(@DrawableRes targetImage: Int) {
        this.targetImage = targetImage
    }
}

class StartImportParitySignerViewHolder(
    private val binder: ItemImportParitySignerPageBinding
) : ViewHolder(binder.root) {

    private val adapter = InstructionAdapter()

    init {
        binder.startImportParitySignerInstruction.adapter = adapter
    }

    fun bind(page: ParitySignerPageModel, @DrawableRes targetImage: Int?) {
        adapter.submitList(page.guideItems)
        targetImage?.let { binder.startImportParitySignerConnectOverview.setTargetImage(it) }
    }
}
