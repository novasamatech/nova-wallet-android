package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.preConfiguredNetworks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.ConcatAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.mixin.impl.observeRetries
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.progress.observeProgressDialog
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.databinding.FragmentPreConfiguredNetworkListBinding
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.common.adapter.NetworkManagementListAdapter
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.networkList.preConfiguredNetworks.adapter.AddCustomNetworkAdapter
import javax.inject.Inject

class PreConfiguredNetworksFragment :
    BaseFragment<PreConfiguredNetworksViewModel, FragmentPreConfiguredNetworkListBinding>(),
    NetworkManagementListAdapter.ItemHandler,
    AddCustomNetworkAdapter.ItemHandler {

    override val binder by viewBinding(FragmentPreConfiguredNetworkListBinding::bind)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val addCustomNetworkAdapter = AddCustomNetworkAdapter(this)

    private val networksAdapter by lazy(LazyThreadSafetyMode.NONE) { NetworkManagementListAdapter(imageLoader, this) }

    private val adapter by lazy(LazyThreadSafetyMode.NONE) { ConcatAdapter(addCustomNetworkAdapter, networksAdapter) }

    override fun initViews() {
        binder.preConfiguredNetworksToolbar.applyStatusBarInsets()
        binder.preConfiguredNetworksToolbar.setHomeButtonListener { viewModel.backClicked() }
        binder.preConfiguredNetworkList.adapter = adapter
    }

    override fun inject() {
        FeatureUtils.getFeature<SettingsFeatureComponent>(
            requireContext(),
            SettingsFeatureApi::class.java
        )
            .preConfiguredNetworks()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: PreConfiguredNetworksViewModel) {
        observeRetries(viewModel)
        observeProgressDialog(viewModel.progressDialogMixin)

        binder.preConfiguredNetworksSearch.content.bindTo(viewModel.searchQuery, viewModel.viewModelScope)
        viewModel.networkList.observe {
            binder.preConfiguredNetworkProgress.isVisible = it.isLoading()
            if (it is ExtendedLoadingState.Loaded) {
                networksAdapter.submitList(it.data)
            }
        }
    }

    override fun onNetworkClicked(chainId: String) {
        viewModel.networkClicked(chainId)
    }

    override fun onAddNetworkClicked() {
        viewModel.addNetworkClicked()
    }
}
