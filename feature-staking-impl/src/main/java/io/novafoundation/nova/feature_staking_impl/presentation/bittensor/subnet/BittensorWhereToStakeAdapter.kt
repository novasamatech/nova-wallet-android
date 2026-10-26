package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.LayoutInflater
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ItemBittensorWhereToStakeBinding

class BittensorWhereToStakeAdapter(
    private val onChoose: (BittensorWhereToStakeOption) -> Unit
) : RecyclerView.Adapter<BittensorWhereToStakeAdapter.Holder>() {

    var selectedPage: Int = 0
        set(value) {
            if (field == value) return
            val previous = field
            field = value
            notifyItemChanged(previous, Update.SELECTION)
            notifyItemChanged(value, Update.SELECTION)
        }

    var nativeSymbol: String? = null
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    var loading: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount, Update.BUTTON)
        }

    override fun getItemCount() = BittensorWhereToStakeOption.entries.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemBittensorWhereToStakeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.root.layoutParams.width = parent.width - 72.dp(parent.context)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(BittensorWhereToStakeOption.entries[position])
    }

    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty() || payloads.any { it !is Update }) {
            onBindViewHolder(holder, position)
            return
        }
        if (Update.SELECTION in payloads) holder.bindSelection(BittensorWhereToStakeOption.entries[position])
        if (Update.BUTTON in payloads) holder.bindButton()
    }

    inner class Holder(private val binding: ItemBittensorWhereToStakeBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(option: BittensorWhereToStakeOption) = with(binding) {
            val context = root.context
            root.background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                listOfNotNull(option.gradientStart, option.gradientMiddle, option.gradientEnd)
                    .map(context::getColor)
                    .toIntArray()
            ).apply {
                cornerRadius = 20.dp(context).toFloat()
            }
            bindSelection(option)
            artwork.setImageResource(option.artwork)
            // The source render layers use Screen blending over the card gradient.
            // A transparent Figma export still contains their black-backed image pixels.
            (artwork.drawable.mutate() as BitmapDrawable).paint.xfermode =
                if (option == BittensorWhereToStakeOption.ROOT) null else PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            title.setText(option.title)
            subtitle.text = if (option == BittensorWhereToStakeOption.ROOT && nativeSymbol == null) {
                ""
            } else {
                context.getString(option.subtitle, nativeSymbol)
            }
            reasons.removeAllViews()
            addReasons(R.string.bittensor_quick_pros, option.pros, "+")
            if (option.cons.isNotEmpty()) addReasons(R.string.bittensor_quick_cons, option.cons, "−")
            bindButton()
            choose.setOnClickListener { onChoose(option) }
        }

        fun bindSelection(option: BittensorWhereToStakeOption) = with(binding) {
            val context = root.context
            val selected = bindingAdapterPosition == selectedPage
            root.alpha = if (selected) 1f else 0.55f
            root.foreground = GradientDrawable().apply {
                cornerRadius = 20.dp(context).toFloat()
                setStroke(1.dp(context), context.getColor(if (selected) option.border else R.color.container_border))
            }
        }

        fun bindButton() {
            binding.choose.isEnabled = !loading && nativeSymbol != null
        }

        private fun addReasons(heading: Int, reasons: List<Int>, marker: String) {
            addText(binding.root.context.getString(heading), heading = true, topMargin = if (binding.reasons.childCount == 0) 0 else 16)
            reasons.forEach { resource ->
                val requiresSymbol = resource == R.string.bittensor_quick_pool_con_two ||
                    resource == R.string.bittensor_quick_root_pro_one || resource == R.string.bittensor_quick_root_pro_two
                if (!requiresSymbol || nativeSymbol != null) {
                    addReason(binding.root.context.getString(resource, nativeSymbol), marker)
                }
            }
        }

        private fun addReason(text: String, marker: String) {
            val context = binding.root.context
            val row = LinearLayout(context)
            row.orientation = LinearLayout.HORIZONTAL
            val markerView = TextView(context)
            markerView.setTextAppearance(R.style.TextAppearance_NovaFoundation_Regular_Caption1)
            markerView.gravity = Gravity.CENTER
            markerView.text = marker
            markerView.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(context.getColor(R.color.chips_background))
            }
            row.addView(markerView, LinearLayout.LayoutParams(16.dp(context), 16.dp(context)))

            val label = TextView(context)
            label.setTextAppearance(R.style.TextAppearance_NovaFoundation_Regular_Caption1)
            label.setTextColor(context.getColor(R.color.text_primary))
            label.text = text
            val labelLayout = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            labelLayout.marginStart = 8.dp(context)
            row.addView(label, labelLayout)
            val rowLayout = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            rowLayout.topMargin = 6.dp(context)
            binding.reasons.addView(row, rowLayout)
        }

        private fun addText(text: String, heading: Boolean, topMargin: Int) {
            val context = binding.root.context
            val label = TextView(context)
            label.setTextAppearance(
                if (heading) R.style.TextAppearance_NovaFoundation_SemiBold_Caps2 else R.style.TextAppearance_NovaFoundation_Regular_Caption1
            )
            label.setTextColor(context.getColor(if (heading) R.color.text_secondary else R.color.text_primary))
            label.text = text
            val layout = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            layout.topMargin = topMargin.dp(context)
            binding.reasons.addView(label, layout)
        }
    }

    private enum class Update {
        SELECTION, BUTTON
    }
}
