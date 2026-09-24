package io.novafoundation.nova.app.root.presentation.analytics

import android.graphics.Color
import android.os.Bundle
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import io.novafoundation.nova.app.databinding.FragmentAnalyticsConsentBinding
import io.novafoundation.nova.app.root.di.RootApi
import io.novafoundation.nova.app.root.di.RootComponent
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.navigation.DelayedNavigation
import io.novafoundation.nova.common.utils.clickableSpan
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.insets.applyNavigationBarInsets
import io.novafoundation.nova.common.utils.insets.applyStatusBarInsets
import io.novafoundation.nova.common.utils.setFullSpan
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.common.R as CommonR

class AnalyticsConsentFragment : BaseFragment<AnalyticsConsentViewModel, FragmentAnalyticsConsentBinding>() {

    override fun createBinding() = FragmentAnalyticsConsentBinding.inflate(layoutInflater)

    override fun applyInsets(rootView: View) {
        binder.analyticsConsentScroll.applyStatusBarInsets()
        binder.analyticsConsentButtons.applyNavigationBarInsets()
    }

    override fun initViews() {
        // An explicit answer is the point of the screen, so back is not one of the ways out
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {}

        binder.analyticsConsentNeverAddresses.analyticsConsentCheckRowText.setText(CommonR.string.analytics_consent_never_addresses)
        binder.analyticsConsentNeverIds.analyticsConsentCheckRowText.setText(CommonR.string.analytics_consent_never_ids)
        binder.analyticsConsentIpCountry.analyticsConsentCheckRowText.setText(CommonR.string.analytics_consent_ip_country)
        binder.analyticsConsentChangeAnytime.analyticsConsentCheckRowText.setText(CommonR.string.analytics_consent_change_anytime)

        binder.analyticsConsentBody.applyPrivacyNotice { viewModel.privacyNoticeClicked() }
        binder.analyticsConsentLearnMore.setOnClickListener { viewModel.privacyNoticeClicked() }

        binder.analyticsConsentDisclosureHeader.setOnClickListener { viewModel.disclosureClicked() }

        binder.analyticsConsentAgree.setOnClickListener { viewModel.agreeClicked() }
        binder.analyticsConsentDecline.setOnClickListener { viewModel.declineClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<RootComponent>(this, RootApi::class.java)
            .analyticsConsentComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: AnalyticsConsentViewModel) {
        observeBrowserEvents(viewModel)

        viewModel.disclosureExpanded.observe { expanded ->
            binder.analyticsConsentDisclosureList.setVisible(expanded)
            binder.analyticsConsentDisclosureChevron.rotation = if (expanded) 0f else CHEVRON_COLLAPSED_ROTATION
        }
    }

    /**
     * The sentence ends with a link to the privacy notice, underlined in place rather than coloured -
     * the design keeps it in the body colour.
     */
    private fun TextView.applyPrivacyNotice(onClick: () -> Unit) {
        updatePadding(bottom = 4.dp) // Leaves room for the underline
        movementMethod = android.text.method.LinkMovementMethod.getInstance()
        highlightColor = Color.TRANSPARENT

        val link = context.getString(CommonR.string.analytics_consent_privacy_notice)
            .toSpannable(clickableSpan(onClick))
            .setFullSpan(UnderlineSpan())

        text = SpannableFormatter.format(context.getString(CommonR.string.analytics_consent_body), link)
    }

    companion object {

        private const val CHEVRON_COLLAPSED_ROTATION = 180f

        private const val KEY_NEXT = "AnalyticsConsentFragment.next"

        fun bundle(next: DelayedNavigation) = Bundle().apply { putParcelable(KEY_NEXT, next) }

        fun nextNavigation(fragment: Fragment): DelayedNavigation = requireNotNull(fragment.requireArguments().getParcelable(KEY_NEXT))
    }
}
