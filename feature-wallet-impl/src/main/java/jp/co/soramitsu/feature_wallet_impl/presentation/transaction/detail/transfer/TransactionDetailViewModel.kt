package jp.co.soramitsu.feature_wallet_impl.presentation.transaction.detail.transfer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import jp.co.soramitsu.common.address.AddressIconGenerator
import jp.co.soramitsu.common.address.AddressModel
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.data.network.AppLinksProvider
import jp.co.soramitsu.common.data.network.ExternalAnalyzer
import jp.co.soramitsu.common.mixin.api.Browserable
import jp.co.soramitsu.common.resources.ClipboardManager
import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.common.utils.Event
import jp.co.soramitsu.common.utils.invoke
import jp.co.soramitsu.common.utils.lazyAsync
import jp.co.soramitsu.core.model.Node
import jp.co.soramitsu.feature_account_api.presenatation.account.AddressDisplayUseCase
import jp.co.soramitsu.feature_account_api.presenatation.account.icon.createAddressModel
import jp.co.soramitsu.feature_wallet_impl.R
import jp.co.soramitsu.feature_wallet_impl.presentation.AssetPayload
import jp.co.soramitsu.feature_wallet_impl.presentation.WalletRouter
import jp.co.soramitsu.feature_wallet_impl.presentation.model.OperationParcelizeModel
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry

enum class ExternalActionsSource {
    TRANSACTION_HASH, FROM_ADDRESS, TO_ADDRESS
}

class TransactionDetailViewModel(
    private val router: WalletRouter,
    private val resourceManager: ResourceManager,
    private val addressIconGenerator: AddressIconGenerator,
    private val clipboardManager: ClipboardManager,
    private val appLinksProvider: AppLinksProvider,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val chainRegistry: ChainRegistry,
    val operation: OperationParcelizeModel.Transfer
) : BaseViewModel(), Browserable {

    private val _showExternalViewEvent = MutableLiveData<Event<ExternalActionsSource>>()
    val showExternalTransactionActionsEvent: LiveData<Event<ExternalActionsSource>> = _showExternalViewEvent

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

    fun copyStringClicked(address: String) {
        clipboardManager.addToClipboard(address)

        showMessage(resourceManager.getString(R.string.common_copied))
    }

    fun backClicked() {
        router.back()
    }

    fun repeatTransaction() {
        val retryAddress = retryAddressModelLiveData.value?.address ?: return

        router.openRepeatTransaction(retryAddress, AssetPayload(operation.chainId, operation.assetId))
    }

    private suspend fun getIcon(address: String): AddressModel {
        return addressIconGenerator.createAddressModel(chain(),  address, AddressIconGenerator.SIZE_BIG, addressDisplayUseCase(address))
    }

    fun showExternalActionsClicked(externalActionsSource: ExternalActionsSource) {
        _showExternalViewEvent.value = Event(externalActionsSource)
    }

    fun viewTransactionExternalClicked(analyzer: ExternalAnalyzer, hash: String, networkType: Node.NetworkType) {
        val url = appLinksProvider.getExternalTransactionUrl(analyzer, hash, networkType)

        openBrowserEvent.value = Event(url)
    }

    fun viewAccountExternalClicked(analyzer: ExternalAnalyzer, address: String, networkType: Node.NetworkType) {
        val url = appLinksProvider.getExternalAddressUrl(analyzer, address, networkType)

        openBrowserEvent.value = Event(url)
    }
}
