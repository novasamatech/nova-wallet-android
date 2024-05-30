package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.defaultNetworks

import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.addedNetworks.AddedNetworkListViewModel
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.NetworkListFragment

class ExistingNetworkListFragment : NetworkListFragment<ExistingNetworkListViewModel>() {

    override val adapter: RecyclerView.Adapter<*> by lazy(LazyThreadSafetyMode.NONE) { networksAdapter }

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
