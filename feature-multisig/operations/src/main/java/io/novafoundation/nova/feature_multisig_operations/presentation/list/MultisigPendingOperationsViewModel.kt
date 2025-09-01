package io.novafoundation.nova.feature_multisig_operations.presentation.list

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.presentation.ColoredDrawable
import io.novafoundation.nova.common.presentation.ColoredText
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_account_api.data.multisig.model.MultisigAction
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperation
import io.novafoundation.nova.feature_account_api.data.multisig.model.userAction
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdKeyIn
import io.novafoundation.nova.feature_multisig_operations.R
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.common.MultisigOperationPayload
import io.novafoundation.nova.feature_multisig_operations.presentation.common.fromOperationId
import io.novafoundation.nova.feature_multisig_operations.presentation.details.general.MultisigOperationDetailsPayload
import io.novafoundation.nova.feature_multisig_operations.presentation.list.model.PendingMultisigOperationHeaderModel
import io.novafoundation.nova.feature_multisig_operations.presentation.list.model.PendingMultisigOperationModel
import io.novafoundation.nova.feature_multisig_operations.presentation.list.model.PendingMultisigOperationModel.SigningAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MultisigPendingOperationsViewModel(
    discoveryService: MultisigPendingOperationsService,
    private val router: MultisigOperationsRouter,
    private val resourceManager: ResourceManager,
    private val multisigCallFormatter: MultisigCallFormatter,
    private val selectedAccountUseCase: SelectedAccountUseCase,
) : BaseViewModel() {

    val account = selectedAccountUseCase.selectedMetaAccountFlow()

    val pendingOperationsFlow = discoveryService.pendingOperations()
        .map { operations ->
            val account = account.first()

            operations
                .sortedByDescending { it.timestamp }
                .groupBy { it.timestamp.inWholeDays }
                .toSortedMap(Comparator.reverseOrder())
                .toListWithHeaders(
                    keyMapper = { day, _ -> PendingMultisigOperationHeaderModel(day) },
                    valueMapper = { operation -> operation.toUi(account) }
                )
        }
        .withSafeLoading()
        .shareInBackground()

    fun backClicked() {
        router.back()
    }

    fun operationClicked(model: PendingMultisigOperationModel) {
        val operationPayload = MultisigOperationPayload.fromOperationId(model.id)
        router.openMultisigOperationDetails(MultisigOperationDetailsPayload(operationPayload))
    }

    private suspend fun PendingMultisigOperation.toUi(selectedAccount: MetaAccount): PendingMultisigOperationModel {
        val initialOrigin = selectedAccount.requireAccountIdKeyIn(chain)
        val formattedCall = multisigCallFormatter.formatPreview(call, initialOrigin, chain)

        return PendingMultisigOperationModel(
            id = operationId,
            chain = mapChainToUi(chain),
            action = formatAction(),
            call = formattedCall,
            progress = formatProgress(),
            time = resourceManager.formatTime(timestamp.inWholeMilliseconds)
        )
    }

    private fun PendingMultisigOperation.formatProgress(): String {
        return resourceManager.getString(R.string.multisig_operations_progress, approvals.size.format(), threshold.format())
    }

    private fun PendingMultisigOperation.formatAction(): SigningAction? {
        return when (userAction()) {
            is MultisigAction.CanApprove -> null

            is MultisigAction.CanReject -> SigningAction(
                text = ColoredText(
                    text = resourceManager.getText(R.string.multisig_operations_created),
                    colorRes = R.color.text_secondary
                ),
                icon = null
            )

            MultisigAction.Signed -> SigningAction(
                text = ColoredText(
                    text = resourceManager.getText(R.string.multisig_operations_signed),
                    colorRes = R.color.text_positive
                ),
                icon = ColoredDrawable(
                    drawableRes = R.drawable.ic_checkmark_circle_16,
                    iconColor = R.color.icon_positive
                )
            )
        }
    }
}
