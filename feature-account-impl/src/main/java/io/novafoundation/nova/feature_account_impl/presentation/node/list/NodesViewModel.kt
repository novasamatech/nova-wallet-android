package io.novafoundation.nova.feature_account_impl.presentation.node.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.createSubstrateAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.map
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet.Payload
import io.novafoundation.nova.core.model.Node
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.model.Account
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.node.list.accounts.model.AccountByNetworkModel
import io.novafoundation.nova.feature_account_impl.presentation.node.mixin.api.NodeListingMixin
import io.novafoundation.nova.feature_account_impl.presentation.node.model.NodeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ICON_IN_DP = 24

class NodesViewModel(
    private val interactor: AccountInteractor,
    private val router: AccountRouter,
    private val nodeListingMixin: NodeListingMixin,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager
) : BaseViewModel(), NodeListingMixin by nodeListingMixin {

    private val _noAccountsEvent = MutableLiveData<Event<Node.NetworkType>>()
    val noAccountsEvent: LiveData<Event<Node.NetworkType>> = _noAccountsEvent

    private val _showAccountChooserLiveData = MutableLiveData<Event<Payload<AccountByNetworkModel>>>()
    val showAccountChooserLiveData: LiveData<Event<Payload<AccountByNetworkModel>>> = _showAccountChooserLiveData

    private val _editMode = MutableLiveData<Boolean>()
    val editMode: LiveData<Boolean> = _editMode

    private val _deleteNodeEvent = MutableLiveData<Event<NodeModel>>()
    val deleteNodeEvent: LiveData<Event<NodeModel>> = _deleteNodeEvent

    val toolbarAction = editMode.map {
        if (it) {
            resourceManager.getString(R.string.common_done)
        } else {
            resourceManager.getString(R.string.common_edit)
        }
    }

    fun editClicked() {
        val edit = editMode.value ?: false
        _editMode.value = !edit
    }

    fun backClicked() {
        router.back()
    }

    fun infoClicked(nodeModel: NodeModel) {
        router.openNodeDetails(nodeModel.id)
    }

    fun selectNodeClicked(nodeModel: NodeModel) {
        viewModelScope.launch {
            val (accounts, selectedNode) = interactor.getAccountsByNetworkTypeWithSelectedNode(nodeModel.networkModelType.networkType)

            handleAccountsForNetwork(nodeModel, selectedNode, accounts)
        }
    }

    fun accountSelected(accountModel: AccountByNetworkModel) {
        selectAccountForNode(accountModel.nodeId, accountModel.accountAddress)
    }

    fun addNodeClicked() {
        router.openAddNode()
    }

    fun createAccountForNetworkType(networkType: Node.NetworkType) {
//        router.createAccountForNetworkType(networkType)
    }

    fun deleteNodeClicked(nodeModel: NodeModel) {
        _deleteNodeEvent.value = Event(nodeModel)
    }

    fun confirmNodeDeletion(nodeModel: NodeModel) {
        viewModelScope.launch {
            interactor.deleteNode(nodeModel.id)
        }
    }

    private suspend fun generateIconForAddress(account: Account): AddressModel {
        return addressIconGenerator.createSubstrateAddressModel(account.address, ICON_IN_DP)
    }

    private fun handleAccountsForNetwork(nodeModel: NodeModel, selectedNode: Node, accounts: List<Account>) {
        when {
            accounts.isEmpty() -> _noAccountsEvent.value = Event(nodeModel.networkModelType.networkType)
            accounts.size == 1 -> selectAccountForNode(nodeModel.id, accounts.first().address)
            selectedNode.networkType == nodeModel.networkModelType.networkType -> selectNodeWithCurrentAccount(nodeModel.id)
            else -> showAccountChooser(nodeModel, accounts)
        }
    }

    private fun showAccountChooser(nodeModel: NodeModel, accounts: List<Account>) {
        viewModelScope.launch {
            val accountModels = generateAccountModels(nodeModel, accounts)

            _showAccountChooserLiveData.value = Event(Payload(accountModels))
        }
    }

    private suspend fun generateAccountModels(nodeModel: NodeModel, accounts: List<Account>): List<AccountByNetworkModel> {
        return withContext(Dispatchers.Default) {
            accounts.map { mapAccountToAccountModel(nodeModel.id, it) }
        }
    }

    private suspend fun mapAccountToAccountModel(nodeId: Int, account: Account): AccountByNetworkModel {
        val addressModel = generateIconForAddress(account)

        return AccountByNetworkModel(nodeId, account.address, account.name, addressModel)
    }

    private fun selectAccountForNode(nodeId: Int, accountAddress: String) {
        viewModelScope.launch {
            interactor.selectNodeAndAccount(nodeId, accountAddress)

            router.returnToWallet()
        }
    }

    private fun selectNodeWithCurrentAccount(nodeId: Int) {
        viewModelScope.launch {
            interactor.selectNode(nodeId)

            router.returnToWallet()
        }
    }
}
