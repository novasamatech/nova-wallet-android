package io.novafoundation.nova.feature_push_notifications.presentation.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
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
        pushWelcomeEnableButton.setOnClickListener { viewModel.askPermissionAndOpenSettings() }
        pushWelcomeCancelButton.setOnClickListener { viewModel.backClicked() }

        configureTermsAndPrivacy()
    }

    private fun configureTermsAndPrivacy() {
        val linkColor = requireContext().getColor(R.color.text_primary)

        pushWelcomeTermsAndConditions.text = SpannableFormatter.format(
            requireContext().getString(R.string.push_welcome_terms_and_conditions),
            requireContext().getString(R.string.about_terms).toSpannable(clickableSpan { viewModel.termsClicked() })
                .setFullSpan(colorSpan(linkColor)),
            requireContext().getString(R.string.about_privacy).toSpannable(clickableSpan { viewModel.privacyClicked() })
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
        observeRetries(viewModel)
        setupPermissionAsker(viewModel)

        viewModel.buttonState.observe { state ->
            pushWelcomeEnableButton.setState(state)
            pushWelcomeCancelButton.setState(state)
        }
    }
}
