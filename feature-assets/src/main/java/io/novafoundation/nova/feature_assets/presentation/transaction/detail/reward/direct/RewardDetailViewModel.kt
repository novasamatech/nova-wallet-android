package io.novafoundation.nova.feature_assets.presentation.transaction.detail.reward.direct

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryTokenUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.launch

class RewardDetailViewModel(
    val operation: OperationParcelizeModel.Reward,
    private val addressIconGenerator: AddressIconGenerator,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val router: AssetsRouter,
    private val chainRegistry: ChainRegistry,
    private val externalActions: ExternalActions.Presentation,
) : BaseViewModel(),
    ExternalActions by externalActions {

    val chain by lazyAsync {
        chainRegistry.getChain(operation.chainId)
    }

    val validatorAddressModelFlow = flowOf {
        operation.validator?.let { getIcon(it) }
    }
        .inBackground()
        .share()

    val chainUi = flowOf {
        mapChainToUi(chain())
    }
        .inBackground()
        .share()

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

    private suspend fun getIcon(address: String) = addressIconGenerator.createAddressModel(
        chain = chain(),
        address = address,
        sizeInDp = AddressIconGenerator.SIZE_BIG,
        addressDisplayUseCase = addressDisplayUseCase,
        background = AddressIconGenerator.BACKGROUND_TRANSPARENT
    )
}
