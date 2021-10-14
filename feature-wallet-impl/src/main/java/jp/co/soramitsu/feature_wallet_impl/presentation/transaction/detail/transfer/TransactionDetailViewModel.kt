package jp.co.soramitsu.feature_wallet_impl.presentation.transaction.detail.transfer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import jp.co.soramitsu.common.address.AddressIconGenerator
import jp.co.soramitsu.common.address.AddressModel
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.utils.Event
import jp.co.soramitsu.common.utils.invoke
import jp.co.soramitsu.common.utils.lazyAsync
import jp.co.soramitsu.feature_account_api.presenatation.account.AddressDisplayUseCase
import jp.co.soramitsu.feature_account_api.presenatation.account.icon.createAddressModel
import jp.co.soramitsu.feature_account_api.presenatation.actions.ExternalActions
import jp.co.soramitsu.feature_wallet_impl.presentation.AssetPayload
import jp.co.soramitsu.feature_wallet_impl.presentation.WalletRouter
import jp.co.soramitsu.feature_wallet_impl.presentation.model.OperationParcelizeModel
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.launch

class TransactionDetailViewModel(
    private val router: WalletRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val chainRegistry: ChainRegistry,
    val operation: OperationParcelizeModel.Transfer,
    private val externalActions: ExternalActions.Presentation,
) : BaseViewModel(),
    ExternalActions by externalActions {

    private val chain by lazyAsync {
        chainRegistry.getChain(operation.chainId)
    }

    override val openBrowserEvent: MutableLiveData<Event<String>> = MutableLiveData()

    val recipientAddressModelLiveData = liveData {
        emit(getIcon(operation.receiver))
    }

    val senderAddressModelLiveData = liveData {
        emit(getIcon(operation.sender))
    }

    val retryAddressModelLiveData = if (operation.isIncome) senderAddressModelLiveData else recipientAddressModelLiveData

    fun backClicked() {
        router.back()
    }

    fun repeatTransaction() {
        val retryAddress = retryAddressModelLiveData.value?.address ?: return

        router.openRepeatTransaction(retryAddress, AssetPayload(operation.chainId, operation.assetId))
    }

    private suspend fun getIcon(address: String): AddressModel {
        return addressIconGenerator.createAddressModel(chain(), address, AddressIconGenerator.SIZE_BIG, addressDisplayUseCase(address))
    }

    fun transactionHashClicked() = operation.hash?.let {
        showExternalActions(ExternalActions.Type.Extrinsic(it))
    }

    fun fromAddressClicked() {
        showExternalActions(ExternalActions.Type.Address(operation.sender))
    }

    fun toAddressClicked() {
        showExternalActions(ExternalActions.Type.Address(operation.receiver))
    }

    private fun showExternalActions(type: ExternalActions.Type) = launch {
        externalActions.showExternalActions(type, chain())
    }
}
