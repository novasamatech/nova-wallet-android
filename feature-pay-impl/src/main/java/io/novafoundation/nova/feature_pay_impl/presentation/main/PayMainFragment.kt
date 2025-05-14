package io.novafoundation.nova.feature_pay_impl.presentation.main

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.setupWithViewPager2
import io.novafoundation.nova.common.view.bindWithViewPager2
import io.novafoundation.nova.feature_pay_api.di.PayFeatureApi
import io.novafoundation.nova.feature_pay_impl.R
import io.novafoundation.nova.feature_pay_impl.databinding.FragmentPayMainBinding
import io.novafoundation.nova.feature_pay_impl.di.PayFeatureComponent
import io.novafoundation.nova.feature_pay_impl.presentation.PayRouter
import javax.inject.Inject

class PayMainFragment : BaseFragment<PayMainViewModel, FragmentPayMainBinding>() {

    override fun createBinding() = FragmentPayMainBinding.inflate(layoutInflater)

    @Inject
    lateinit var router: PayRouter

    override fun initViews() {
        binder.payAppBarContainer.applyStatusBarInsets()

        val adapter = PayPagerAdapter(this)
        binder.payViewPager.adapter = adapter
        binder.payTabs.setupWithViewPager2(binder.payViewPager, adapter::getPageTitle)

        binder.payAppBar.onWalletClick { viewModel.avatarClicked() }
        binder.payAppBar.onWalletConnectClick { viewModel.walletConnectClicked() }
        binder.payAppBar.onSettingsClick { }

        binder.payAppBarContainer.bindWithViewPager2(binder.payViewPager, R.drawable.bg_wallet_app_bar)
    }

    override fun inject() {
        FeatureUtils.getFeature<PayFeatureComponent>(requireContext(), PayFeatureApi::class.java)
            .mainPayComponentFactory()
            .create(fragment = this)
            .inject(this)
    }

    override fun subscribe(viewModel: PayMainViewModel) {
        viewModel.selectedWalletModel.observe {
            binder.payAppBar.setSelectedWallet(it.typeIcon?.icon, it.name)
        }

        viewModel.walletConnectAccountSessions.observe {
            binder.payAppBar.setWalletConnectActive(it.hasConnections)
        }
    }
}
