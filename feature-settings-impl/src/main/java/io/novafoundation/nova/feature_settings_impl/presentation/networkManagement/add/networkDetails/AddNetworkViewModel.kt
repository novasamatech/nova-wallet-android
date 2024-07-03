package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class AddNetworkViewModel(
    private val resourceManager: ResourceManager,
    private val router: SettingsRouter,
    private val payload: AddNetworkPayload
) : BaseViewModel() {

    val isChainIdVisibleFlow = flowOf { chainIdRequired() }
        .shareInBackground()

    val nodeUrlFlow = MutableStateFlow("")
    val networkNameFlow = MutableStateFlow("")
    val tokenNameFlow = MutableStateFlow("")
    val chainIdFlow = MutableStateFlow("")
    val blockExplorerFlow = MutableStateFlow("")
    val priceProviderFlow = MutableStateFlow("")

    private val loadingState = MutableStateFlow(false)

    val buttonState = combine(
        nodeUrlFlow,
        networkNameFlow,
        tokenNameFlow,
        chainIdFlow,
        loadingState
    ) { url, networkName, tokenName, chainId, isLoading ->
        val chainIdRequiredAndEmpty = chainIdRequired() && chainId.isBlank()
        when {
            isLoading -> DescriptiveButtonState.Loading
            url.isBlank() || networkName.isBlank() || tokenName.isBlank() || chainIdRequiredAndEmpty -> DescriptiveButtonState.Disabled(
                resourceManager.getString(R.string.common_enter_details)
            )

            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_add_network))
        }
    }

    init {
        val prefilledData = payload.prefilledData
        if (prefilledData != null) {
            nodeUrlFlow.value = prefilledData.rpcNodeUrl
            networkNameFlow.value = prefilledData.networkName
            tokenNameFlow.value = prefilledData.tokenName
            chainIdFlow.value = prefilledData.chainId ?: ""
            blockExplorerFlow.value = prefilledData.blockExplorerUrl ?: ""
            priceProviderFlow.value = prefilledData.coingeckoLink ?: ""
        }
    }

    private fun chainIdRequired(): Boolean {
        return payload.mode == AddNetworkPayload.Mode.EVM
    }

    fun addNetworkClicked() {

    }
}
