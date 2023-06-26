package io.novafoundation.nova.feature_staking_impl.presentation.staking.landing

import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.utils.SpannableFormatter
import io.novafoundation.nova.common.utils.clickableSpan
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.drawableSpan
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.setEndSpan
import io.novafoundation.nova.common.utils.setFullSpan
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.synthetic.main.item_start_staking_landing_footer.view.itemStakingLandingFooterMoreInfo
import kotlinx.android.synthetic.main.item_start_staking_landing_footer.view.itemStakingLandingFooterTermsOfUse

class StartStakingLandingFooterAdapter : RecyclerView.Adapter<StartStakingLandingFooterViewHolder>() {

    private var moreInfoText: CharSequence = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StartStakingLandingFooterViewHolder {
        return StartStakingLandingFooterViewHolder(parent.inflateChild(R.layout.item_start_staking_landing_footer))
    }

    override fun getItemCount(): Int {
        return 1
    }

    override fun onBindViewHolder(holder: StartStakingLandingFooterViewHolder, position: Int) {
        holder.bind(moreInfoText)
    }

    fun setMoreInformationText(text: CharSequence) {
        this.moreInfoText = text
        notifyItemChanged(0)
    }
}

class StartStakingLandingFooterViewHolder(containerView: View) : RecyclerView.ViewHolder(containerView) {

    init {
        with(itemView) {
            val iconColor = context.getColor(R.color.chip_icon)
            val clickablePartColor = context.getColor(R.color.text_secondary)
            val chevronSize = 20.dp(context)
            val chevronRight = ContextCompat.getDrawable(context, R.drawable.ic_chevron_right)
                ?.apply {
                    setBounds(0, 0, chevronSize, chevronSize)
                    setTint(iconColor)
                }
            val termsClickablePart = resources.getText(R.string.start_staking_fragment_terms_of_use_clicable_part)
                .toSpannable(colorSpan(clickablePartColor))
                .setFullSpan(clickableSpan { })
                .setEndSpan(drawableSpan(chevronRight!!))

            itemStakingLandingFooterTermsOfUse.text = SpannableFormatter.format(
                resources.getString(R.string.start_staking_fragment_terms_of_use),
                termsClickablePart
            )
        }
    }

    fun bind(title: CharSequence) {
        with(itemView) {
            itemStakingLandingFooterMoreInfo.text = title
        }
    }
}
