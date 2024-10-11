package io.novafoundation.nova.feature_currency_impl.presentation.currency

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_currency_impl.R
import io.novafoundation.nova.feature_currency_impl.di.CurrencyFeatureComponent

class SelectCurrencyFragment : BaseFragment<SelectCurrencyViewModel>(), CurrencyAdapter.Handler {

    private val adapter = CurrencyAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_select_currency, container, false)
    }

    override fun initViews() {
        currencyList.adapter = adapter

        currencyToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<CurrencyFeatureComponent>(
            requireContext(),
            CurrencyFeatureApi::class.java
        ).selectCurrencyComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: SelectCurrencyViewModel) {
        viewModel.currencyModels.observe {
            adapter.submitList(it)
        }
    }

    override fun itemClicked(currency: io.novafoundation.nova.feature_currency_api.presentation.model.CurrencyModel) {
        viewModel.selectCurrency(currency)
    }
}
