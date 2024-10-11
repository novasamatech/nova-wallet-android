package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails

import android.os.Bundle
import androidx.core.view.isVisible
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.databinding.FragmentAddNetworkBinding
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureComponent
import io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.main.AddNetworkPayload

class AddNetworkFragment : BaseFragment<AddNetworkViewModel, FragmentAddNetworkBinding>() {

    companion object {

        private const val KEY_PAYLOAD = "key_payload"

        fun getBundle(payload: AddNetworkPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    override val binder: FragmentAddNetworkBinding by viewBinding(FragmentAddNetworkBinding::bind)

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
        binder.addNetworkApplyButton.prepareForProgress(lifecycleOwner)

        binder.addNetworkApplyButton.setOnClickListener { viewModel.addNetworkClicked() }
    }

    override fun subscribe(viewModel: AddNetworkViewModel) {
        observeValidations(viewModel)
        viewModel.isNodeEditable.observe { binder.addNetworkNodeUrl.isEnabled = it }
        binder.addNetworkNodeUrl.bindTo(viewModel.nodeUrlFlow, viewModel)
        binder.addNetworkName.bindTo(viewModel.networkNameFlow, viewModel)
        binder.addNetworkCurrency.bindTo(viewModel.tokenSymbolFlow, viewModel)
        viewModel.isChainIdVisibleFlow.observe { binder.addNetworkChainIdContainer.isVisible = it }
        binder.addNetworkChainId.bindTo(viewModel.evmChainIdFlow, viewModel)
        binder.addNetworkBlockExplorer.bindTo(viewModel.blockExplorerFlow, viewModel)
        binder.addNetworkPriceInfoProvider.bindTo(viewModel.priceProviderFlow, viewModel)
        viewModel.buttonState.observe { binder.addNetworkApplyButton.setState(it) }
    }
}
