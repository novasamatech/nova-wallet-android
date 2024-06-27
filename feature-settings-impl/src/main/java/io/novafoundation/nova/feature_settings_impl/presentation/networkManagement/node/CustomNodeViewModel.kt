package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.node

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.domain.CustomNodeInteractor
import io.novafoundation.nova.feature_settings_impl.domain.validation.NetworkNodePayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CustomNodeViewModel(
    private val router: SettingsRouter,
    private val resourceManager: ResourceManager,
    private val payload: CustomNodePayload,
    private val customNodeInteractor: CustomNodeInteractor,
    private val validationExecutor: ValidationExecutor,
    private val chainRegistry: ChainRegistry
) : BaseViewModel(), Validatable by validationExecutor {

    val chainModel = flowOf { chainRegistry.getChain(payload.chainId) }
        .map { mapChainToUi(it) }

    val nodeUrlInput = MutableStateFlow("")
    val nodeNameInput = MutableStateFlow("")

    private val saveProgressFlow = MutableStateFlow(false)

    val buttonState = combine(saveProgressFlow, nodeUrlInput, nodeNameInput) { validationProgress, url, name ->
        when {
            validationProgress -> DescriptiveButtonState.Loading

            url.isBlank() || name.isBlank() -> {
                DescriptiveButtonState.Disabled(resourceManager.getString(R.string.custom_node_disabled_button_state))
            }

            else -> getEnabledButtonState()
        }
    }

    init {
        launch {
            if (payload.mode is CustomNodePayload.Mode.Edit) {
                customNodeInteractor.getNodeDetails(payload.chainId, payload.mode.url)
                    .onSuccess { node ->
                        nodeUrlInput.value = node.unformattedUrl
                        nodeNameInput.value = node.name
                    }.onFailure {
                        router.back()
                    }
            }
        }
    }

    fun backClicked() {
        router.back()
    }

    fun getTitle(): String {
        return when (payload.mode) {
            CustomNodePayload.Mode.Add -> resourceManager.getString(R.string.custom_node_add_title)

            is CustomNodePayload.Mode.Edit -> resourceManager.getString(R.string.custom_node_edit_title)
        }
    }

    fun saveNodeClicked() {
        launch {
            saveProgressFlow.value = true
            val payload = NetworkNodePayload(
                chain = chainRegistry.getChain(payload.chainId),
                nodeUrl = nodeUrlInput.value
            )

            validationExecutor.requireValid(
                validationSystem = customNodeInteractor.getValidationSystem(viewModelScope),
                payload = payload,
                progressConsumer = saveProgressFlow.progressConsumer(),
                validationFailureTransformerCustom = { status, actions -> mapSaveCustomNodeFailureToUI(resourceManager, status) }
            ) {
                saveProgressFlow.value = false

                executeSaving()
            }
        }
    }

    private fun executeSaving() {
        launch {
            when (payload.mode) {
                CustomNodePayload.Mode.Add -> customNodeInteractor.createNode(payload.chainId, nodeUrlInput.value, nodeNameInput.value)

                is CustomNodePayload.Mode.Edit -> customNodeInteractor.updateNode(payload.chainId, payload.mode.url, nodeUrlInput.value, nodeNameInput.value)
            }

            router.back()
        }
    }

    private fun getEnabledButtonState(): DescriptiveButtonState.Enabled {
        return when (payload.mode) {
            CustomNodePayload.Mode.Add -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.custom_node_add_button_state))

            is CustomNodePayload.Mode.Edit -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.custom_node_edit_button_state))
        }
    }
}
