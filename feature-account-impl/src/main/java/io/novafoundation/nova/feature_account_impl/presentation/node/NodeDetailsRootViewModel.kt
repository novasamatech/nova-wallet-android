package io.novafoundation.nova.feature_account_impl.presentation.node

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.base.errors.NovaException
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.core.model.Node
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.errors.NodeAlreadyExistsException
import io.novafoundation.nova.feature_account_impl.domain.errors.UnsupportedNetworkException

abstract class NodeDetailsRootViewModel(
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    protected open fun handleNodeException(throwable: Throwable) {
        when (throwable) {
            is NodeAlreadyExistsException -> showError(resourceManager.getString(R.string.connection_add_already_exists_error))
            is UnsupportedNetworkException -> showError(getUnsupportedNodeError())
            is NovaException -> {
                if (NovaException.Kind.NETWORK == throwable.kind) {
                    showError(resourceManager.getString(R.string.connection_add_invalid_error))
                } else {
                    throwable.message?.let(::showError)
                }
            }
            else -> throwable.message?.let(::showError)
        }
    }

    protected open fun getUnsupportedNodeError(): String {
        val supportedNodes = Node.NetworkType.values().joinToString(", ") { it.readableName }
        val unsupportedNodeErrorMsg = resourceManager.getString(R.string.connection_add_unsupported_error)
        return unsupportedNodeErrorMsg.format(supportedNodes)
    }
}
