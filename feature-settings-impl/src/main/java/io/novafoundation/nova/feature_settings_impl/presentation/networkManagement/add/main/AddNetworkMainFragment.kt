package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main

import android.os.Bundle
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.setupWithViewPager2
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.databinding.FragmentAddNetworkMainBinding
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent

class AddNetworkMainFragment : BaseFragment<AddNetworkMainViewModel, FragmentAddNetworkMainBinding>() {

    companion object {

        private const val KEY_PAYLOAD = "key_payload"

        fun getBundle(payload: AddNetworkPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    override val binder: FragmentAddNetworkMainBinding by viewBinding(FragmentAddNetworkMainBinding::bind)

    override fun initViews() {
        binder.addNetworkMainToolbar.applyStatusBarInsets()
        binder.addNetworkMainToolbar.setHomeButtonListener { viewModel.backClicked() }

        val payload: AddNetworkPayload? = argumentOrNull(KEY_PAYLOAD)
        val adapter = AddNetworkMainPagerAdapter(this, payload)
        binder.addNetworkMainViewPager.adapter = adapter

        if (payload == null) {
            binder.addNetworkMainTabLayout.setupWithViewPager2(binder.addNetworkMainViewPager, adapter::getPageTitle)
        } else {
            binder.addNetworkMainTabLayout.makeGone()
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
