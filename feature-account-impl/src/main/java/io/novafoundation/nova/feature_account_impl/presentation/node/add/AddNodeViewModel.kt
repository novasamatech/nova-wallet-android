package io.novafoundation.nova.feature_account_impl.presentation.node.add

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.common.utils.requireException
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.NodeHostValidator
import io.novafoundation.nova.feature_account_api.presenatation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.node.NodeDetailsRootViewModel
import kotlinx.coroutines.launch

class AddNodeViewModel(
    private val interactor: AccountInteractor,
    private val router: AccountRouter,
    private val nodeHostValidator: NodeHostValidator,
    resourceManager: ResourceManager
) : NodeDetailsRootViewModel(resourceManager) {

    val nodeNameInputLiveData = MutableLiveData<String>()
    val nodeHostInputLiveData = MutableLiveData<String>()

    private val addingInProgressLiveData = MutableLiveData(false)

    val addButtonState = combine(
        nodeNameInputLiveData,
        nodeHostInputLiveData,
        addingInProgressLiveData
    ) { (name: String, host: String, addingInProgress: Boolean) ->
        when {
            addingInProgress -> ButtonState.PROGRESS
            name.isNotEmpty() && nodeHostValidator.hostIsValid(host) -> ButtonState.NORMAL
            else -> ButtonState.DISABLED
        }
    }

    fun backClicked() {
        router.back()
    }

    fun addNodeClicked() {
        val nodeName = nodeNameInputLiveData.value ?: return
        val nodeHost = nodeHostInputLiveData.value ?: return

        addingInProgressLiveData.value = true

        viewModelScope.launch {
            val result = interactor.addNode(nodeName, nodeHost)

            if (result.isSuccess) {
                router.back()
            } else {
                handleNodeException(result.requireException())
            }
        }
    }
}
