package io.novafoundation.nova.feature_wallet_impl.presentation.currency

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_wallet_api.presentation.model.CurrencyModel
import io.novafoundation.nova.feature_wallet_impl.R
import io.novafoundation.nova.feature_wallet_impl.di.WalletFeatureComponent
import kotlinx.android.synthetic.main.fragment_select_currency.currencyList

class SelectCurrencyFragment : BaseFragment<SelectCurrencyViewModel>(), CurrencyAdapter.Handler {

    private val adapter = CurrencyAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_select_currency, container, false)
    }

    override fun initViews() {
        currencyList.adapter = adapter
    }

    override fun inject() {
        FeatureUtils.getFeature<WalletFeatureComponent>(
            requireContext(),
            WalletFeatureApi::class.java
        ).selectCurrencyComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SelectCurrencyViewModel) {
        viewModel.currencyModels.observe {
            adapter.submitList(it)
        }
    }

    override fun itemClicked(currency: CurrencyModel) {
        viewModel.selectCurrency(currency)
    }
}
