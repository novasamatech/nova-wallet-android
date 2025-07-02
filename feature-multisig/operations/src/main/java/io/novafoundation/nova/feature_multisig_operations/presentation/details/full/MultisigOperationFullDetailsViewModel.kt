package io.novafoundation.nova.feature_multisig_operations.presentation.details.full

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.onLoaded
import io.novafoundation.nova.common.mixin.copy.CopyTextMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.ellipsizeMiddle
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.view.bottomSheet.description.DescriptionBottomSheetLauncher
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountUIUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_multisig_operations.R
import io.novafoundation.nova.feature_multisig_operations.domain.details.MultisigOperationDetailsInteractor
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.details.common.MultisigOperationDetailsPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.utilityAsset
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MultisigOperationFullDetailsViewModel(
    private val router: MultisigOperationsRouter,
    private val resourceManager: ResourceManager,
    private val interactor: MultisigOperationDetailsInteractor,
    private val multisigOperationsService: MultisigPendingOperationsService,
    private val externalActions: ExternalActions.Presentation,
    private val payload: MultisigOperationDetailsPayload,
    private val accountUIUseCase: AccountUIUseCase,
    private val descriptionBottomSheetLauncher: DescriptionBottomSheetLauncher,
    private val copyTextMixin: CopyTextMixin.Presentation
) : BaseViewModel(),
    ExternalActions by externalActions,
    CopyTextMixin by copyTextMixin,
    DescriptionBottomSheetLauncher by descriptionBottomSheetLauncher {

    fun backClicked() {
        router.back()
    }

    private val operationFlow = multisigOperationsService.pendingOperationFlow(payload.operationId)
        .filterNotNull()
        .shareInBackground()

    private val tokenFlow = flowOfAll {
        interactor.tokenFlow(operationFlow.first().chain.utilityAsset)
    }.shareInBackground()

    val depositorAccountModel = operationFlow.map {
        accountUIUseCase.getAccountModel(it.depositor.value, it.chain)
    }.withSafeLoading()
        .shareInBackground()

    val depositAmount = combine(operationFlow, tokenFlow) { operation, token ->
        mapAmountToAmountModel(operation.deposit, token)
    }.shareInBackground()

    private val callHash = operationFlow.map { operation ->
        operation.call?.let { interactor.callHash(it, operation.chain.id) }
    }.shareInBackground()

    val ellipsizedCallHash = callHash.map { it?.ellipsizeMiddle(9) }

    val formattedCall = operationFlow.map { operation ->
        operation.call?.let { interactor.callDetails(it) }
    }.shareInBackground()

    fun onDepositorClicked() = launchUnit {
        val chain = operationFlow.first().chain
        depositorAccountModel.first().onLoaded {
            externalActions.showAddressActions(it.address(), chain)
        }
    }

    fun callHashClicked() = launchUnit {
        val callHash = callHash.first() ?: return@launchUnit
        copyTextMixin.showCopyTextDialog(callHash)
    }

    fun depositClicked() {
        launchDescriptionBottomSheet(
            titleRes = R.string.multisig_deposit,
            descriptionRes = R.string.multisig_deposit_description
        )
    }
}
