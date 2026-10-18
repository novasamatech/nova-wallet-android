package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details.select

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.presentation.ellipsizeAddress
import io.novafoundation.nova.common.view.TableCellView

class BittensorUnstakePositionAdapter(
    private val options: List<BittensorSelectUnstakePositionCommunicator.Option>,
    private val onSelect: (BittensorSelectUnstakePositionCommunicator.Option) -> Unit
) : RecyclerView.Adapter<BittensorUnstakePositionAdapter.Holder>() {

    override fun getItemCount() = options.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val row = TableCellView.createTableCellView(parent.context)
        row.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return Holder(row)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(options[position])

    inner class Holder(private val row: TableCellView) : RecyclerView.ViewHolder(row) {

        fun bind(option: BittensorSelectUnstakePositionCommunicator.Option) {
            row.title.text = option.address.ellipsizeAddress()
            row.showValue(option.amount, option.fiat)
            row.setOnClickListener { onSelect(option) }
        }
    }
}
