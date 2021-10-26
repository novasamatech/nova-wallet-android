package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam

import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer.Payload.DialogAction
import io.novafoundation.nova.common.mixin.api.displayError
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam.MoonbeamCrowdloanInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam.MoonbeamFlowStatus
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.StartFlowInterceptor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.mapParachainMetadataFromParcel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MoonbeamStartFlowInterceptor(
    private val crowdloanRouter: CrowdloanRouter,
    private val resourceManager: ResourceManager,
    private val moonbeamInteractor: MoonbeamCrowdloanInteractor,
    private val customDialogDisplayer: CustomDialogDisplayer.Presentation,
) : StartFlowInterceptor {

    override suspend fun startFlow(payload: ContributePayload) {
        withContext(Dispatchers.Default) {
            moonbeamInteractor.flowStatus(mapParachainMetadataFromParcel(payload.parachainMetadata!!))
        }
            .onSuccess { handleMoonbeamStatus(it, payload) }
            .onFailure { customDialogDisplayer.displayError(resourceManager, it) }
    }

    private fun handleMoonbeamStatus(status: MoonbeamFlowStatus, payload: ContributePayload) {
        when (status) {
            MoonbeamFlowStatus.Completed -> crowdloanRouter.openContribute(payload)

            is MoonbeamFlowStatus.NeedsChainAccount -> {
                customDialogDisplayer.displayDialog(
                    CustomDialogDisplayer.Payload(
                        title = resourceManager.getString(R.string.crowdloan_moonbeam_missing_account_title),
                        message = resourceManager.getString(R.string.crowdloan_moonbeam_missing_account_message),
                        okAction = DialogAction(
                            title = resourceManager.getString(R.string.common_add),
                            action = { crowdloanRouter.openAddAccount(AddAccountPayload.ChainAccount(status.chainId, status.metaId)) }
                        ),
                        cancelAction = DialogAction.noOp(resourceManager.getString(R.string.common_cancel))
                    )
                )
            }

            MoonbeamFlowStatus.ReadyToComplete -> crowdloanRouter.openMoonbeamFlow(payload)

            MoonbeamFlowStatus.RegionNotSupported -> {
                customDialogDisplayer.displayDialog(
                    CustomDialogDisplayer.Payload(
                        title = resourceManager.getString(R.string.crowdloan_moonbeam_region_restriction_title),
                        message = resourceManager.getString(R.string.crowdloan_moonbeam_region_restriction_message),
                        okAction = DialogAction.noOp(resourceManager.getString(R.string.common_ok)),
                        cancelAction = null
                    )
                )
            }
            MoonbeamFlowStatus.UnsupportedAccountEncryption -> customDialogDisplayer.displayDialog(
                CustomDialogDisplayer.Payload(
                    title = resourceManager.getString(R.string.crowdloan_moonbeam_encryption_not_supported_title),
                    message = resourceManager.getString(R.string.crowdloan_moonbeam_encryption_not_supported_message),
                    okAction = DialogAction.noOp(resourceManager.getString(R.string.common_ok)),
                    cancelAction = null
                )
            )
        }
    }
}
