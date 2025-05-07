package io.novafoundation.nova.feature_pay_impl.presentation.shop

import androidx.recyclerview.widget.ConcatAdapter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_pay_api.di.PayFeatureApi
import io.novafoundation.nova.feature_pay_impl.databinding.FragmentShopBinding
import io.novafoundation.nova.feature_pay_impl.di.PayFeatureComponent

class ShopFragment : BaseFragment<ShopViewModel, FragmentShopBinding>() {

    override fun createBinding() = FragmentShopBinding.inflate(layoutInflater)

    private val unavailableWalletAdapter by lazy(LazyThreadSafetyMode.NONE) { ShopUnavailableAccountPlaceholderAdapter() }

    private val adapter by lazy(LazyThreadSafetyMode.NONE) { ConcatAdapter(unavailableWalletAdapter) }

    override fun initViews() {
        binder.shopList.adapter = adapter
    }

    override fun inject() {
        FeatureUtils.getFeature<PayFeatureComponent>(requireContext(), PayFeatureApi::class.java)
            .shopComponentFactory()
            .create(fragment = this)
            .inject(this)
    }

    override fun subscribe(viewModel: ShopViewModel) {
        viewModel.isWalletAvailableFlow.observe {
            unavailableWalletAdapter.show(!it)
        }
    }
}
