package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.landing

import android.text.method.LinkMovementMethod
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.list.SingleItemAdapter
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.clickableSpan
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.drawableSpan
import io.novafoundation.nova.common.utils.fontSpan
import io.novafoundation.nova.common.utils.inflateChild
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setEndSpan
import io.novafoundation.nova.common.utils.setFullSpan
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ItemStartStakingLandingFooterBinding

class StartStakingLandingFooterAdapter(private val handler: ClickHandler) : SingleItemAdapter<StartStakingLandingFooterViewHolder>() {

    interface ClickHandler {
        fun onTermsOfUseClicked()
    }

    private var moreInfoText: CharSequence = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StartStakingLandingFooterViewHolder {
        parent.inflateChild(R.layout.item_start_staking_landing_footer)
        return StartStakingLandingFooterViewHolder(handler, ItemStartStakingLandingFooterBinding.inflate(parent.inflater(), parent, false))
    }

    override fun onBindViewHolder(holder: StartStakingLandingFooterViewHolder, position: Int) {
        holder.bind(moreInfoText)
    }

    fun setMoreInformationText(text: CharSequence) {
        this.moreInfoText = text
        notifyItemChanged(0)
    }
}

class StartStakingLandingFooterViewHolder(
    private val clickHandler: StartStakingLandingFooterAdapter.ClickHandler,
    private val binder: ItemStartStakingLandingFooterBinding
) : RecyclerView.ViewHolder(binder.root) {

    init {
        with(binder) {
            val context = root.context
            val resources = context.resources
            val iconColor = context.getColor(R.color.chip_icon)
            val clickablePartColor = context.getColor(R.color.link_text)
            val chevronSize = 20.dp(context)
            val chevronRight = ContextCompat.getDrawable(context, R.drawable.ic_chevron_right)
                ?.apply {
                    setBounds(0, 0, chevronSize, chevronSize)
                    setTint(iconColor)
                }
            val termsClickablePart = resources.getText(R.string.start_staking_fragment_terms_of_use_clicable_part)
                .toSpannable(colorSpan(clickablePartColor))
                .setFullSpan(clickableSpan { clickHandler.onTermsOfUseClicked() })
                .setFullSpan(fontSpan(context, R.font.public_sans_semi_bold))
                .setEndSpan(drawableSpan(chevronRight!!))

            itemStakingLandingFooterTermsOfUse.text = SpannableFormatter.format(
                resources.getString(R.string.start_staking_fragment_terms_of_use),
                termsClickablePart
            )

            itemStakingLandingFooterTermsOfUse.movementMethod = LinkMovementMethod.getInstance()
            itemStakingLandingFooterMoreInfo.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    fun bind(title: CharSequence) {
        with(binder) {
            itemStakingLandingFooterMoreInfo.text = title
        }
    }
}
