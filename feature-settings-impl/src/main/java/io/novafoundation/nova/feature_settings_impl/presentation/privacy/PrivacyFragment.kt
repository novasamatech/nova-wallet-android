package io.novafoundation.nova.feature_settings_impl.presentation.privacy

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.databinding.FragmentPrivacyBinding
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent

class PrivacyFragment : BaseFragment<PrivacyViewModel, FragmentPrivacyBinding>() {

    override fun createBinding() = FragmentPrivacyBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.privacyToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.privacyNeverAddresses.privacyCheckRowText.setText(R.string.analytics_consent_never_addresses)
        binder.privacyNeverIds.privacyCheckRowText.setText(R.string.analytics_consent_never_ids)
        binder.privacyIpCountry.privacyCheckRowText.setText(R.string.analytics_consent_ip_country)
        binder.privacyChangeAnytime.privacyCheckRowText.setText(R.string.analytics_consent_change_anytime)

        binder.privacyAnalytics.setOnClickListener { viewModel.analyticsClicked() }
        binder.privacyNotice.setOnClickListener { viewModel.privacyNoticeClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<SettingsFeatureComponent>(
            requireContext(),
            SettingsFeatureApi::class.java
        )
            .privacyFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: PrivacyViewModel) {
        observeBrowserEvents(viewModel)

        viewModel.analyticsEnabledState.observe(binder.privacyAnalytics::setChecked)
    }
}
