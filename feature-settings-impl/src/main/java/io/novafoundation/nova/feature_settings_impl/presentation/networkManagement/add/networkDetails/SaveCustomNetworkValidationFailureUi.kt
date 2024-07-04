package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.add.networkDetails

import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.TransformedFailure.Default
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.CustomNetworkFailure

fun mapSaveCustomNetworkFailureToUI(
    resourceManager: ResourceManager,
    status: ValidationStatus.NotValid<CustomNetworkFailure>,
    actions: ValidationFlowActions<*>,
): TransformedFailure {
    return when (val reason = status.reason) {
        is CustomNetworkFailure.DefaultNetworkAlreadyAdded -> Default(
            resourceManager.getString(R.string.network_already_exist_failure_title) to
                resourceManager.getString(R.string.default_network_already_exist_failure_message, reason.networkName)
        )

        is CustomNetworkFailure.CustomNetworkAlreadyAdded -> {
            TransformedFailure.Custom(
                CustomDialogDisplayer.Payload(
                    title = resourceManager.getString(R.string.network_already_exist_failure_title),
                    message = resourceManager.getString(R.string.custom_network_already_exist_failure_message, reason.networkName),
                    okAction = CustomDialogDisplayer.Payload.DialogAction(
                        title = resourceManager.getString(R.string.common_close),
                        action = { }
                    ),
                    cancelAction = CustomDialogDisplayer.Payload.DialogAction(
                        title = resourceManager.getString(R.string.common_modify),
                        action = { actions.resumeFlow() }
                    ),
                    customStyle = R.style.AccentDialogNeutralButtonStyle
                )
            )
        }

        CustomNetworkFailure.NodeIsNotAlive -> Default(
            resourceManager.getString(R.string.node_not_alive_title) to
                resourceManager.getString(R.string.node_not_alive_message)
        )

        is CustomNetworkFailure.WrongNetwork -> Default(
            resourceManager.getString(R.string.create_network_invalid_chain_id_title) to
                resourceManager.getString(R.string.create_network_invalid_chain_id_message)
        )

        CustomNetworkFailure.CoingeckoLinkBadFormat -> Default(
            resourceManager.getString(R.string.asset_add_evm_token_invalid_coin_gecko_link_title) to
                resourceManager.getString(R.string.asset_add_evm_token_invalid_coin_gecko_link_message)
        )
    }
}
