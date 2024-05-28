package io.novafoundation.nova.feature_account_impl.presentation.exporting.json

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.export.json.ExportJsonInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.export.json.validations.ExportJsonPasswordValidationPayload
import io.novafoundation.nova.feature_account_impl.domain.account.export.json.validations.ExportJsonPasswordValidationSystem
import io.novafoundation.nova.feature_account_impl.domain.account.export.json.validations.mapExportJsonPasswordValidationFailureToUi
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportPayload
import io.novafoundation.nova.feature_account_impl.presentation.exporting.ExportViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ExportJsonViewModel(
    private val router: AccountRouter,
    private val interactor: ExportJsonInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: ExportJsonPasswordValidationSystem,
    private val payload: ExportPayload,
) : ExportViewModel(),
    Validatable by validationExecutor {

    val passwordFlow = MutableStateFlow("")
    val passwordConfirmationFlow = MutableStateFlow("")

    private val jsonGenerationInProgressFlow = MutableStateFlow(false)

    val nextButtonState: Flow<DescriptiveButtonState> = combine(
        passwordFlow,
        passwordConfirmationFlow,
        jsonGenerationInProgressFlow
    ) { password, confirmation, jsonGenerationInProgress ->
        when {
            jsonGenerationInProgress -> DescriptiveButtonState.Loading
            password.isBlank() || confirmation.isBlank() -> DescriptiveButtonState.Disabled(
                resourceManager.getString(R.string.common_enter_password)
            )

            else -> DescriptiveButtonState.Enabled(
                resourceManager.getString(R.string.export_json_download_btn)
            )
        }
    }

    fun back() {
        router.back()
    }

    fun nextClicked() = viewModelScope.launch {
        val password = passwordFlow.value

        val validationPayload = ExportJsonPasswordValidationPayload(
            password = password,
            passwordConfirmation = passwordConfirmationFlow.value
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = validationPayload,
            progressConsumer = jsonGenerationInProgressFlow.progressConsumer(),
            validationFailureTransformer = { mapExportJsonPasswordValidationFailureToUi(resourceManager, it) }
        ) {
            tryGenerateJson(password)
        }
    }

    private fun tryGenerateJson(password: String) = launch {
        val generateRestoreJsonResult = when (payload) {
            is ExportPayload.ChainAccount -> interactor.generateRestoreJson(payload.metaId, payload.chainId, password)
            is ExportPayload.MetaAccount -> interactor.generateRestoreJson(payload.metaId, password)
        }

        generateRestoreJsonResult
            .onSuccess { exportText(it) }
            .onFailure { it.message?.let(::showError) }

        jsonGenerationInProgressFlow.value = false
    }
}
