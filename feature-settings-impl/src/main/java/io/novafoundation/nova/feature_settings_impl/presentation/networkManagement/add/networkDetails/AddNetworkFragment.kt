package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main.AddNetworkPayload
import kotlinx.android.synthetic.main.fragment_add_network.addNetworkApplyButton
import kotlinx.android.synthetic.main.fragment_add_network.addNetworkBlockExplorer
import kotlinx.android.synthetic.main.fragment_add_network.addNetworkChainId
import kotlinx.android.synthetic.main.fragment_add_network.addNetworkChainIdContainer
import kotlinx.android.synthetic.main.fragment_add_network.addNetworkCurrency
import kotlinx.android.synthetic.main.fragment_add_network.addNetworkName
import kotlinx.android.synthetic.main.fragment_add_network.addNetworkNodeUrl
import kotlinx.android.synthetic.main.fragment_add_network.addNetworkPriceInfoProvider

class AddNetworkFragment : BaseFragment<AddNetworkViewModel>() {

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
        return inflater.inflate(R.layout.fragment_add_network, container, false)
    }

    override fun inject() {
        FeatureUtils.getFeature<SettingsFeatureComponent>(
            requireContext(),
            SettingsFeatureApi::class.java
        )
            .addNetworkFactory()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }

    override fun initViews() {
        addNetworkApplyButton.prepareForProgress(lifecycleOwner)

        addNetworkApplyButton.setOnClickListener { viewModel.addNetworkClicked() }
    }

    override fun subscribe(viewModel: AddNetworkViewModel) {
        observeValidations(viewModel)
        viewModel.isNodeEditable.observe { addNetworkNodeUrl.isEnabled = it }
        addNetworkNodeUrl.bindTo(viewModel.nodeUrlFlow, viewModel)
        addNetworkName.bindTo(viewModel.networkNameFlow, viewModel)
        addNetworkCurrency.bindTo(viewModel.tokenSymbolFlow, viewModel)
        viewModel.isChainIdVisibleFlow.observe { addNetworkChainIdContainer.isVisible = it }
        addNetworkChainId.bindTo(viewModel.evmChainIdFlow, viewModel)
        addNetworkBlockExplorer.bindTo(viewModel.blockExplorerFlow, viewModel)
        addNetworkPriceInfoProvider.bindTo(viewModel.priceProviderFlow, viewModel)
        viewModel.buttonState.observe { addNetworkApplyButton.setState(it) }
    }
}
