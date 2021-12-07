package io.novafoundation.nova.feature_account_impl.presentation.exporting.json.password

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.withFlagSet
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.export.json.ExportJsonInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.confirm.ExportJsonConfirmPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ExportJsonPasswordViewModel(
    private val router: AccountRouter,
    private val interactor: ExportJsonInteractor,
    private val resourceManager: ResourceManager,
    private val chainRegistry: ChainRegistry,
    private val payload: ExportPayload,
) : BaseViewModel() {

    private val chain by lazyAsync { chainRegistry.getChain(payload.chainId) }

    val passwordFlow = MutableStateFlow("")
    val passwordConfirmationFlow = MutableStateFlow("")

    private val jsonGenerationInProgressFlow = MutableStateFlow(false)

    val chainFlow = flowOf { mapChainToUi(chain()) }
        .inBackground()
        .share()

    val nextButtonState: Flow<DescriptiveButtonState> = combine(
        passwordFlow,
        passwordConfirmationFlow,
        jsonGenerationInProgressFlow
    ) { password, confirmation, jsonGenerationInProgress ->
        when {
            jsonGenerationInProgress -> DescriptiveButtonState.Loading
            password.isBlank() || confirmation.isBlank() -> DescriptiveButtonState.Disabled(
                resourceManager.getString(R.string.common_input_error_set_password)
            )
            password != confirmation -> DescriptiveButtonState.Disabled(
                resourceManager.getString(R.string.export_json_password_match_error)
            )
            else -> DescriptiveButtonState.Enabled(
                resourceManager.getString(R.string.common_continue)
            )
        }
    }

    fun back() {
        router.back()
    }

    fun nextClicked() = viewModelScope.launch {
        jsonGenerationInProgressFlow.withFlagSet {
            interactor.generateRestoreJson(payload.metaId, payload.chainId, passwordFlow.value)
                .onSuccess {
                    val confirmPayload = ExportJsonConfirmPayload(payload, it)

                    router.openExportJsonConfirm(confirmPayload)
                }
                .onFailure { it.message?.let(::showError) }
        }
    }
}
