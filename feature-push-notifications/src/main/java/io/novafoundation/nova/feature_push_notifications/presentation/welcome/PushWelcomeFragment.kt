package io.novafoundation.nova.feature_push_notifications.presentation.welcome

import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.formatting.applyTermsAndPrivacyPolicy
import io.novafoundation.nova.common.utils.permissions.setupPermissionAsker
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.databinding.FragmentPushWelcomeBinding
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureComponent

class PushWelcomeFragment : BaseFragment<PushWelcomeViewModel, FragmentPushWelcomeBinding>() {

    override val binder by viewBinding(FragmentPushWelcomeBinding::bind)

    override fun initViews() {
        binder.pushWelcomeToolbar.applyStatusBarInsets()
        binder.pushWelcomeEnableButton.prepareForProgress(this)
        binder.pushWelcomeCancelButton.prepareForProgress(this)
        binder.pushWelcomeToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.pushWelcomeEnableButton.setOnClickListener { viewModel.askPermissionAndEnablePushNotifications() }
        binder.pushWelcomeCancelButton.setOnClickListener { viewModel.backClicked() }

        configureTermsAndPrivacy()
    }

    private fun configureTermsAndPrivacy() {
        binder.pushWelcomeTermsAndConditions.applyTermsAndPrivacyPolicy(
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
            binder.pushWelcomeEnableButton.setState(state)
            binder.pushWelcomeCancelButton.setState(state)
        }
    }
}
