package io.novafoundation.nova.feature_multisig_operations.presentation.list

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
import io.novafoundation.nova.feature_multisig_operations.R
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.common.MultisigOperationFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.details.MultisigOperationDetailsPayload
import io.novafoundation.nova.feature_multisig_operations.presentation.list.model.PendingMultisigOperationModel

class MultisigPendingOperationsViewModel(
    discoveryService: MultisigPendingOperationsService,
    private val router: MultisigOperationsRouter,
    private val resourceManager: ResourceManager,
    private val operationFormatter: MultisigOperationFormatter,
) : BaseViewModel() {

    val pendingOperationsFlow = discoveryService.pendingOperations()
        .mapList { it.toUi() }
        .withSafeLoading()
        .shareInBackground()


    fun backClicked() {
        router.back()
    }

    fun operationClicked(model: PendingMultisigOperationModel) {
        val payload = MultisigOperationDetailsPayload(model.id)
        router.openMultisigOperationDetails(payload)
    }

    private fun PendingMultisigOperation.toUi(): PendingMultisigOperationModel {
        return PendingMultisigOperationModel(
            id = identifier,
            chain = mapChainToUi(chain),
            action = formatAction(),
            operationTitle = operationFormatter.formatTitle(this),
            progress = formatProgress()
        )
    }

    private fun PendingMultisigOperation.formatProgress(): String {
        return resourceManager.getString(R.string.multisig_operations_progress, approvals.size.format(), threshold.format())
    }

    private fun PendingMultisigOperation.formatAction(): ColoredText {
        return when (userAction()) {
            is MultisigAction.CanApprove -> ColoredText(
                text = resourceManager.getText(R.string.multisig_operations_pending_signature),
                colorRes = R.color.button_text_accent
            )

            is MultisigAction.CanReject, MultisigAction.Signed -> ColoredText(
                text = resourceManager.getText(R.string.multisig_operations_signed),
                colorRes = R.color.text_secondary
            )
        }
    }
}
