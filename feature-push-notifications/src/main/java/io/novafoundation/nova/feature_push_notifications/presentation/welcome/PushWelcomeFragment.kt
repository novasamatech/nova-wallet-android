package io.novafoundation.nova.feature_push_notifications.presentation.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.formatting.applyTermsAndPrivacyPolicy
import io.novafoundation.nova.common.utils.permissions.setupPermissionAsker
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureComponent

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
        pushWelcomeTermsAndConditions.applyTermsAndPrivacyPolicy(
            R.string.push_welcome_terms_and_conditions,
            R.string.common_terms_and_conditions_formatting,
            R.string.common_privacy_policy_formatting,
            viewModel::termsClicked,
            viewModel::privacyClicked
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
