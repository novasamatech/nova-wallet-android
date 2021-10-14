package jp.co.soramitsu.feature_wallet_impl.presentation.transaction.detail.reward

import androidx.lifecycle.liveData
import jp.co.soramitsu.common.address.AddressIconGenerator
import jp.co.soramitsu.common.address.createAddressModel
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.common.utils.invoke
import jp.co.soramitsu.common.utils.lazyAsync
import jp.co.soramitsu.feature_account_api.presenatation.account.AddressDisplayUseCase
import jp.co.soramitsu.feature_account_api.presenatation.actions.ExternalActions
import jp.co.soramitsu.feature_wallet_impl.R
import jp.co.soramitsu.feature_wallet_impl.presentation.WalletRouter
import jp.co.soramitsu.feature_wallet_impl.presentation.model.OperationParcelizeModel
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.launch

class RewardDetailViewModel(
    val operation: OperationParcelizeModel.Reward,
    private val resourceManager: ResourceManager,
    private val addressIconGenerator: AddressIconGenerator,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val router: WalletRouter,
    private val chainRegistry: ChainRegistry,
    private val externalActions: ExternalActions.Presentation
) : BaseViewModel(),
    ExternalActions by externalActions {

    val chain by lazyAsync {
        chainRegistry.getChain(operation.chainId)
    }

    val validatorAddressModelLiveData = liveData {
        val icon = operation.validator?.let { getIcon(it) }

        emit(icon)
    }

    val eraLiveData = liveData {
        emit(resourceManager.getString(R.string.staking_era_index_no_prefix, operation.era))
    }

    private suspend fun getIcon(address: String) = addressIconGenerator.createAddressModel(
        address,
        AddressIconGenerator.SIZE_BIG,
        addressDisplayUseCase(address)
    )

    fun backClicked() {
        router.back()
    }

    fun eventIdClicked() {
        shoExternalActions(ExternalActions.Type.Event(operation.eventId))
    }

    fun validatorAddressClicked() {
        operation.validator?.let {
            shoExternalActions(ExternalActions.Type.Address(it))
        }
    }

    private fun shoExternalActions(type: ExternalActions.Type) = launch {
        externalActions.showExternalActions(type, chain())
    }
}
