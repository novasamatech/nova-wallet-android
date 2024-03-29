package io.novafoundation.nova.feature_push_notifications.presentation.welcome

import android.graphics.Color
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.clickableSpan
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.permissions.setupPermissionAsker
import io.novafoundation.nova.common.utils.setFullSpan
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureComponent
import kotlinx.android.synthetic.main.fragment_push_welcome.pushWelcomeCancelButton
import kotlinx.android.synthetic.main.fragment_push_welcome.pushWelcomeEnableButton
import kotlinx.android.synthetic.main.fragment_push_welcome.pushWelcomeTermsAndConditions
import kotlinx.android.synthetic.main.fragment_push_welcome.pushWelcomeToolbar

class PushWelcomeFragment : BaseFragment<PushWelcomeViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_push_welcome, container, false)
    }

    override fun initViews() {
        pushWelcomeToolbar.applyStatusBarInsets()
        pushWelcomeEnableButton.prepareForProgress(this)
        pushWelcomeCancelButton.prepareForProgress(this)
        pushWelcomeToolbar.setHomeButtonListener { viewModel.backClicked() }
        pushWelcomeEnableButton.setOnClickListener { viewModel.askPermissionAndEnablePushNotifications() }
        pushWelcomeCancelButton.setOnClickListener { viewModel.backClicked() }

        configureTermsAndPrivacy()
    }

    private fun configureTermsAndPrivacy() {
        val linkColor = requireContext().getColor(R.color.text_primary)

        pushWelcomeTermsAndConditions.movementMethod = LinkMovementMethod.getInstance()
        pushWelcomeTermsAndConditions.highlightColor = Color.TRANSPARENT

        pushWelcomeTermsAndConditions.text = SpannableFormatter.format(
            requireContext().getString(R.string.push_welcome_terms_and_conditions),
            requireContext().getString(R.string.common_terms_and_conditions_formatting)
                .toSpannable(clickableSpan { viewModel.termsClicked() })
                .setFullSpan(colorSpan(linkColor)),
            requireContext().getString(R.string.common_privacy_policy_formatting)
                .toSpannable(clickableSpan { viewModel.privacyClicked() })
                .setFullSpan(colorSpan(linkColor))
        )
    }

    override fun inject() {
        FeatureUtils.getFeature<PushNotificationsFeatureComponent>(requireContext(), PushNotificationsFeatureApi::class.java)
            .pushWelcomeComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: PushWelcomeViewModel) {
        observeBrowserEvents(viewModel)
        observeRetries(viewModel)
        setupPermissionAsker(viewModel)

        viewModel.buttonState.observe { state ->
            pushWelcomeEnableButton.setState(state)
            pushWelcomeCancelButton.setState(state)
        }
    }
}
