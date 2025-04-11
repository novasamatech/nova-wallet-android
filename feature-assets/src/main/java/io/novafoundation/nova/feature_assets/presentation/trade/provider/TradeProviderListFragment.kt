package io.novafoundation.nova.feature_assets.presentation.trade.provider

import androidx.recyclerview.widget.ConcatAdapter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.ViewSpace
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.common.view.recyclerview.adapter.text.TextAdapter
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.FragmentTradeProviderListBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent

class TradeProviderListFragment : BaseFragment<TradeProviderListViewModel, FragmentTradeProviderListBinding>(), TradeProviderAdapter.ItemHandler {

    companion object : PayloadCreator<TradeProviderListPayload> by FragmentPayloadCreator()

    override fun createBinding() = FragmentTradeProviderListBinding.inflate(layoutInflater)

    private val titleAdapter by lazy(LazyThreadSafetyMode.NONE) {
        TextAdapter(styleRes = R.style.TextAppearance_NovaFoundation_Bold_Title3)
    }

    private val providersAdapter by lazy(LazyThreadSafetyMode.NONE) {
        TradeProviderAdapter(this)
    }

    private val footerAdapter by lazy(LazyThreadSafetyMode.NONE) {
        TextAdapter(
            text = getString(R.string.trade_provider_list_footer),
            R.style.TextAppearance_NovaFoundation_Regular_Caption1,
            textColor = R.color.text_secondary,
            paddingInDp = ViewSpace(top = 12)
        )
    }

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        ConcatAdapter(titleAdapter, providersAdapter, footerAdapter)
    }

    override fun initViews() {
        binder.tradeProviderListToolbar.applyStatusBarInsets()
        binder.tradeProviderListToolbar.setHomeButtonListener { viewModel.back() }
        binder.tradeProviderList.adapter = adapter
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .tradeProviderListComponent()
            .create(this, payload())
            .inject(this)
    }

    override fun subscribe(viewModel: TradeProviderListViewModel) {
        viewModel.titleFlow.observe { titleAdapter.setText(it) }
        viewModel.providerModels.observe { providersAdapter.submitList(it) }
    }

    override fun providerClicked(item: TradeProviderRvItem) {
        viewModel.onProviderClicked(item)
    }
}
