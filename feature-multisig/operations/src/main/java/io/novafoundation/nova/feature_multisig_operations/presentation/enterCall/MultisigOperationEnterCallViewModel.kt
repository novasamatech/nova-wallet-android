package io.novafoundation.nova.feature_multisig_operations.presentation.enterCall

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.extrinsicHash
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperation
import io.novafoundation.nova.feature_multisig_operations.R
import io.novafoundation.nova.feature_multisig_operations.domain.details.MultisigOperationDetailsInteractor
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
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
        if (isValid(operation)) {
            interactor.setCall(operation, enteredCall.value)
            router.back()
        } else {
            onCallInvalid(enteredCall.value)
        }
    }

    private fun onCallInvalid(enteredCall: String) = try {
        val callHash = enteredCall.extrinsicHash()
        showError(
            resourceManager.getString(R.string.invalid_call_data_title),
            resourceManager.getString(R.string.invalid_call_data_message, callHash)
        )
    } catch (e: Exception) {
        showError(resourceManager.getString(R.string.invalid_call_data_title))
    }

    private fun isValid(operation: PendingMultisigOperation): Boolean = try {
        val enteredCall = enteredCall.value.fromHex()
        val operationHash = operation.callHash.value
        val enteredHash = enteredCall.blake2b256()

        operationHash.contentEquals(enteredHash)
    } catch (e: Exception) {
        false
    }
}
