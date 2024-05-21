package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.list

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.NetworkListFragment

class ExistingNetworkListFragment : NetworkListFragment<ExistingNetworkListViewModel>() {

    override fun inject() {
        FeatureUtils.getFeature<SettingsFeatureComponent>(
            requireContext(),
            SettingsFeatureApi::class.java
        )
            .existingNetworkListFactory()
            .create(this)
            .inject(this)
    }
}
