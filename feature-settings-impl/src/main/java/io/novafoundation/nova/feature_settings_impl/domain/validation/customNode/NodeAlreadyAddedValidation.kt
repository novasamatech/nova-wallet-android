package io.novafoundation.nova.feature_settings_impl.domain.validation.customNode

import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError

class NodeAlreadyAddedValidation : Validation<NetworkNodePayload, NetworkNodeFailure> {

    override suspend fun validate(value: NetworkNodePayload): ValidationStatus<NetworkNodeFailure> {
        try {
            val node = value.chain.nodes
                .nodes
                .firstOrNull { it.unformattedUrl == value.nodeUrl.normalize() }

            if (node != null) {
                return validationError(NetworkNodeFailure.NodeAlreadyExists(node))
            }

            return valid()
        } catch (e: Exception) {
            return validationError(NetworkNodeFailure.NodeIsNotAlive)
        }
    }

    private fun String.normalize(): String {
        return Urls.normalizePath(this)
    }
}

fun ValidationSystemBuilder<NetworkNodePayload, NetworkNodeFailure>.validateNodeNotAdded() = validate(
    NodeAlreadyAddedValidation()
)
