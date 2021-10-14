package jp.co.soramitsu.feature_wallet_impl.presentation.transaction.detail.extrinsic

import androidx.lifecycle.liveData
import jp.co.soramitsu.common.address.AddressIconGenerator
import jp.co.soramitsu.common.address.createAddressModel
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.utils.invoke
import jp.co.soramitsu.common.utils.lazyAsync
import jp.co.soramitsu.feature_account_api.presenatation.account.AddressDisplayUseCase
import jp.co.soramitsu.feature_account_api.presenatation.actions.ExternalActions
import jp.co.soramitsu.feature_wallet_impl.presentation.WalletRouter
import jp.co.soramitsu.feature_wallet_impl.presentation.model.OperationParcelizeModel
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.launch

class ExtrinsicDetailViewModel(
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val addressIconGenerator: AddressIconGenerator,
    private val chainRegistry: ChainRegistry,
    private val router: WalletRouter,
    val operation: OperationParcelizeModel.Extrinsic,
    private val externalActions: ExternalActions.Presentation
) : BaseViewModel(),
    ExternalActions by externalActions {

    private val chain by lazyAsync {
        chainRegistry.getChain(operation.chainId)
    }

    val fromAddressModelLiveData = liveData {
        emit(getIcon(operation.originAddress))
    }

    private suspend fun getIcon(address: String) = addressIconGenerator.createAddressModel(
        address,
        AddressIconGenerator.SIZE_BIG,
        addressDisplayUseCase(address)
    )

    fun extrinsicClicked() = launch {
        externalActions.showExternalActions(ExternalActions.Type.Extrinsic(operation.hash), chain())
    }

    fun fromAddressClicked() = launch {
        externalActions.showExternalActions(ExternalActions.Type.Address(operation.originAddress), chain())
    }

    fun backClicked() {
        router.back()
    }
}
