package io.novafoundation.nova.feature_settings_impl.presentation.networkManagement.node

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.TransformedFailure.Default
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.domain.validation.NetworkNodeFailure

fun mapSaveCustomNodeFailureToUI(
    resourceManager: ResourceManager,
    status: ValidationStatus.NotValid<NetworkNodeFailure>
): TransformedFailure {
    return when (val reason = status.reason) {
        is NetworkNodeFailure.NodeAlreadyExists -> Default(
            resourceManager.getString(R.string.node_already_exist_failure_title) to
                resourceManager.getString(R.string.node_already_exist_failure_message, reason.node.name)
        )

        NetworkNodeFailure.NodeIsNotAlive -> Default(
            resourceManager.getString(R.string.node_not_alive_title) to
                resourceManager.getString(R.string.node_not_alive_message)
        )

        is NetworkNodeFailure.WrongNetwork -> Default(
            resourceManager.getString(R.string.node_not_supported_by_network_title) to
                resourceManager.getString(R.string.node_not_supported_by_network_message, reason.chain.name)
        )
    }
}
