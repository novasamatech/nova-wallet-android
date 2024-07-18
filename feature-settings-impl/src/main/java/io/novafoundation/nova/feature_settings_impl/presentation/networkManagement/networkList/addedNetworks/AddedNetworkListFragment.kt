package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.addedNetworks

import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.list.EditablePlaceholderAdapter
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.ViewSpace
import io.novafoundation.nova.common.view.PlaceholderModel
import io.novafoundation.nova.common.view.PlaceholderView
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.addedNetworks.adapter.NetworksBannerAdapter
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.NetworkListFragment

class AddedNetworkListFragment : NetworkListFragment<AddedNetworkListViewModel>(), NetworksBannerAdapter.ItemHandler {

    private val bannerAdapter = NetworksBannerAdapter(this)

    private val placeholderAdapter = EditablePlaceholderAdapter()

    override val adapter: RecyclerView.Adapter<*> by lazy(LazyThreadSafetyMode.NONE) {
        ConcatAdapter(bannerAdapter, placeholderAdapter, networksAdapter)
    }

    override fun inject() {
        FeatureUtils.getFeature<SettingsFeatureComponent>(
            requireContext(),
            SettingsFeatureApi::class.java
        )
            .addedNetworkListFactory()
            .create(this)
            .inject(this)
    }

    override fun initViews() {
        super.initViews()
        placeholderAdapter.setPadding(ViewSpace(top = 64.dp))
        placeholderAdapter.setPlaceholderData(
            PlaceholderModel(
                requireContext().getString(R.string.added_networks_empty_placeholder),
                R.drawable.ic_no_added_networks,
                requireContext().getString(R.string.common_add_network),
                PlaceholderView.Style.NO_BACKGROUND,
                imageTint = null
            )
        )
        placeholderAdapter.setButtonClickListener { viewModel.addNetworkClicked() }
    }

    override fun subscribe(viewModel: AddedNetworkListViewModel) {
        super.subscribe(viewModel)

        observeBrowserEvents(viewModel)

        viewModel.showBanner.observe { bannerAdapter.show(it) }
        viewModel.networkList.observe { placeholderAdapter.show(it.isEmpty()) }
    }

    override fun closeBannerClicked() {
        viewModel.closeBannerClicked()
    }

    override fun bannerWikiLinkClicked() {
        viewModel.bannerWikiClicked()
    }
}
