package io.novafoundation.nova.feature_multisig_operations.presentation.enterCall

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.callHashString
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_multisig_operations.R
import io.novafoundation.nova.feature_multisig_operations.domain.details.MultisigOperationDetailsInteractor
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class MultisigOperationEnterCallViewModel(
    private val router: MultisigOperationsRouter,
    private val interactor: MultisigOperationDetailsInteractor,
    private val multisigOperationsService: MultisigPendingOperationsService,
    private val payload: MultisigOperationEnterCallPayload,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    val enteredCall = MutableStateFlow("")

    val buttonState = enteredCall.map {
        when {
            it.isBlank() -> DescriptiveButtonState.Disabled(reason = resourceManager.getString(R.string.enter_call_data_title))
            else -> DescriptiveButtonState.Enabled(action = resourceManager.getString(R.string.common_save))
        }
    }

    fun approve() = launchUnit {
        val operation = multisigOperationsService.pendingOperation(payload.operationId) ?: return@launchUnit
        if (interactor.isCallValid(operation, enteredCall.value)) {
            interactor.setCall(operation, enteredCall.value)
            router.back()
        } else {
            onCallInvalid(enteredCall.value)
        }
    }

    private fun onCallInvalid(enteredCall: String) = try {
        val callHash = enteredCall.callHashString()
        showError(
            resourceManager.getString(R.string.invalid_call_data_title),
            resourceManager.getString(R.string.invalid_call_data_message, callHash)
        )
    } catch (e: Exception) {
        showError(resourceManager.getString(R.string.invalid_call_data_title))
    }
}
