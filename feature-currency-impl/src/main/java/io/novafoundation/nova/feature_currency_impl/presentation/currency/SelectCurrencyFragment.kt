package io.novafoundation.nova.feature_currency_impl.presentation.currency

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_currency_impl.databinding.FragmentSelectCurrencyBinding
import io.novafoundation.nova.feature_currency_impl.di.CurrencyFeatureComponent

class SelectCurrencyFragment : BaseFragment<SelectCurrencyViewModel, FragmentSelectCurrencyBinding>(), CurrencyAdapter.Handler {

    override fun createBinding() = FragmentSelectCurrencyBinding.inflate(layoutInflater)

    private val adapter = CurrencyAdapter(this)

    override fun initViews() {
        binder.currencyList.adapter = adapter

        binder.currencyToolbar.setHomeButtonListener {
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
