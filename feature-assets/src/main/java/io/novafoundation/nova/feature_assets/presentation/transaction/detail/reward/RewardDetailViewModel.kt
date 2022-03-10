package io.novafoundation.nova.feature_assets.presentation.transaction.detail.reward

import androidx.lifecycle.liveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.invoke
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_assets.presentation.WalletRouter
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.launch

class RewardDetailViewModel(
    val operation: OperationParcelizeModel.Reward,
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

    val validatorAddressModelFlow = liveData {
        val icon = operation.validator?.let { getIcon(it) }

        emit(icon)
    }

    val chainUi = flowOf {
        mapChainToUi(chain())
    }
        .inBackground()
        .share()

    private suspend fun getIcon(address: String) = addressIconGenerator.createAddressModel(
        address,
        AddressIconGenerator.SIZE_BIG,
        addressDisplayUseCase(chain(), address)
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
