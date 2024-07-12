package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.main

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import kotlinx.android.synthetic.main.fragment_network_management.networkManagementTabLayout
import kotlinx.android.synthetic.main.fragment_network_management.networkManagementToolbar
import kotlinx.android.synthetic.main.fragment_network_management.networkManagementViewPager

class NetworkManagementListFragment : BaseFragment<NetworkManagementListViewModel>() {

    companion object {

        private const val KEY_OPEN_ADDED_TAB = "key_payload"

        fun getBundle(openAddedTab: Boolean): Bundle {
            return Bundle().apply {
                putBoolean(KEY_OPEN_ADDED_TAB, openAddedTab)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_network_management, container, false)
    }

    override fun initViews() {
        networkManagementToolbar.applyStatusBarInsets()
        networkManagementToolbar.setHomeButtonListener { viewModel.backClicked() }
        networkManagementToolbar.setRightActionClickListener { viewModel.addNetworkClicked() }

        val adapter = NetworkManagementPagerAdapter(this)
        networkManagementViewPager.adapter = adapter
        networkManagementTabLayout.setupWithViewPager2(networkManagementViewPager, adapter::getPageTitle)

        Handler(Looper.getMainLooper()).post {
            setDefaultTab(adapter)
        }
    }

    private fun setDefaultTab(adapter: NetworkManagementPagerAdapter) {
        val openAddedTab = argumentOrNull<Boolean>(KEY_OPEN_ADDED_TAB) ?: return
        val tabIndex = if (openAddedTab) adapter.addedTabIndex() else adapter.defaultTabIndex()
        networkManagementViewPager.currentItem = tabIndex
    }

    override fun inject() {
        FeatureUtils.getFeature<SettingsFeatureComponent>(
            requireContext(),
            SettingsFeatureApi::class.java
        )
            .networkManagementListFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: NetworkManagementListViewModel) {
    }
}
