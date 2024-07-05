package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
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

    companion object {

        private const val KEY_PAYLOAD = "key_payload"

        fun getBundle(payload: AddNetworkPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

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

        val payload: AddNetworkPayload? = argumentOrNull(KEY_PAYLOAD)
        val adapter = AddNetworkMainPagerAdapter(this, payload)
        addNetworkMainViewPager.adapter = adapter

        if (payload == null) {
            addNetworkMainTabLayout.setupWithViewPager2(addNetworkMainViewPager, adapter::getPageTitle)
        } else {
            addNetworkMainTabLayout.makeGone()
        }
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
