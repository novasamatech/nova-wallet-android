package io.novafoundation.nova.common.base.errors

import io.novafoundation.nova.common.resources.ResourceManager

class FearlessException(
    val kind: Kind,
    message: String?,
    exception: Throwable? = null
) : RuntimeException(message, exception) {

    enum class Kind {
        NETWORK,
        UNEXPECTED
    }

    companion object {

        fun networkError(resourceManager: ResourceManager, throwable: Throwable): FearlessException {
            return FearlessException(Kind.NETWORK, "", throwable) // TODO: add common error text to resources
        }

        fun unexpectedError(exception: Throwable): FearlessException {
            return FearlessException(Kind.UNEXPECTED, exception.message ?: "", exception) // TODO: add common error text to resources
        }
    }
}
