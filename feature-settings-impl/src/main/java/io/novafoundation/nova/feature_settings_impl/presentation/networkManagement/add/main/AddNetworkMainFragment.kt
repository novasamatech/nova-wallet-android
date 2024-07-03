package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.setupWithViewPager2
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent
import kotlinx.android.synthetic.main.fragment_add_network_main.addNetworkMainTabLayout
import kotlinx.android.synthetic.main.fragment_add_network_main.addNetworkMainToolbar
import kotlinx.android.synthetic.main.fragment_add_network_main.addNetworkMainViewPager
import kotlinx.android.synthetic.main.fragment_network_management.networkManagementTabLayout
import kotlinx.android.synthetic.main.fragment_network_management.networkManagementToolbar
import kotlinx.android.synthetic.main.fragment_network_management.networkManagementViewPager

class AddNetworkMainFragment : BaseFragment<AddNetworkMainViewModel>() {

    private val adapter by lazy(LazyThreadSafetyMode.NONE) { AddNetworkMainPagerAdapter(this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_add_network_main, container, false)
    }

    override fun initViews() {
        addNetworkMainToolbar.applyStatusBarInsets()
        addNetworkMainToolbar.setHomeButtonListener { viewModel.backClicked() }
        addNetworkMainViewPager.adapter = AddNetworkMainPagerAdapter(this)
        addNetworkMainTabLayout.setupWithViewPager2(networkManagementViewPager, adapter::getPageTitle)
    }

    override fun inject() {
        FeatureUtils.getFeature<SettingsFeatureComponent>(
            requireContext(),
            SettingsFeatureApi::class.java
        )
            .addNetworkMainFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: AddNetworkMainViewModel) {
    }
}
