package io.novafoundation.nova.feature_account_impl.presentation.multisig.operations

import io.novafoundation.nova.common.address.toHex
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.ColoredText
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_account_api.data.multisig.model.MultisigAction
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperation
import io.novafoundation.nova.feature_account_api.data.multisig.model.userAction
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.multisig.operations.model.PendingMultisigOperationModel
import kotlinx.coroutines.flow.map

class MultisigPendingOperationsViewModel(
    discoveryService: MultisigPendingOperationsService,
    private val router: AccountRouter,
    private val resourceManager: ResourceManager,
) : BaseViewModel() {

    val pendingOperationsFlow = discoveryService.pendingOperations()
        .mapList { it.toUi() }
        .withSafeLoading()
        .shareInBackground()


    fun backClicked() {
        router.back()
    }

    fun operationClicked(model: PendingMultisigOperationModel) {
        showMessage("Todo")
    }

    private fun PendingMultisigOperation.toUi(): PendingMultisigOperationModel {
        return PendingMultisigOperationModel(
            id = id(),
            chain = mapChainToUi(chain),
            action = formatAction(),
            operationTitle = formatTitle(),
            progress = formatProgress()
        )
    }

    private fun PendingMultisigOperation.formatProgress(): String {
        return resourceManager.getString(R.string.multisig_operations_progress, approvals.size.format(), threshold.format())
    }

    private fun PendingMultisigOperation.formatTitle(): String {
        val call = call

        return if (call != null) {
            "${call.module.name}.${call.function.name}"
        } else {
            val hexHash = callHash.toHex()
            val trimmedHash = hexHash.substring(0, 4) + "..." + hexHash.substring(hexHash.length - 4, hexHash.length)

            resourceManager.getString(R.string.multisig_operations_unknown_calldata, trimmedHash)
        }
    }


    private fun PendingMultisigOperation.formatAction(): ColoredText {
        return when (userAction()) {
            MultisigAction.CAN_APPROVE -> ColoredText(
                text = resourceManager.getText(R.string.multisig_operations_pending_signature),
                colorRes = R.color.button_text_accent
            )

            MultisigAction.SIGNED, MultisigAction.CAN_REJECT -> ColoredText(
                text = resourceManager.getText(R.string.multisig_operations_signed),
                colorRes = R.color.text_secondary
            )
        }
    }

    private fun PendingMultisigOperation.id(): String {
        return "${chain.id}.${callHash.toHex()}.${timePoint.height}.${timePoint.extrinsicIndex}"
    }
}
