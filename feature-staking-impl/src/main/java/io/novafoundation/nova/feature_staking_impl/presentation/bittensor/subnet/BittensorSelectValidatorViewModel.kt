package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.onError
import io.novafoundation.nova.common.navigation.requireLastInput
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetValidatorRepository
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class BittensorSelectValidatorViewModel(
    private val communicator: BittensorSelectValidatorCommunicator,
    private val validatorsRepository: BittensorSubnetValidatorRepository,
    private val icons: AddressIconGenerator,
    private val router: StakingRouter,
    private val externalActions: ExternalActions.Presentation,
    private val chainRegistry: ChainRegistry,
    private val resources: ResourceManager,
    private val logger: Logger
) : BaseViewModel(), ExternalActions by externalActions {

    private val request = communicator.requireLastInput()

    private val validatorsState = flowOf {
        val chain = chainRegistry.getChain(ChainGeneses.BITTENSOR)
        val validators = if (request.subnetId == 0) {
            validatorsRepository.rootValidators(chain)
        } else {
            validatorsRepository.validators(
                chain,
                BittensorSubnetId(request.subnetId)
            )
        }
        validators.map { validator ->
            val icon = icons.createAddressIcon(
                validator.validator.hotkey.value,
                AddressIconGenerator.SIZE_MEDIUM,
                AddressIconGenerator.BACKGROUND_TRANSPARENT
            )
            BittensorValidatorUi(validator, icon)
        }
    }.withSafeLoading()
        .onError { reportFailure(it) }
        .shareInBackground()

    val rows = validatorsState.map { it.dataOrNull.orEmpty() }.shareInBackground()

    private val selectedValidatorAddress = MutableStateFlow(request.selectedAddress)

    val selectedAddress = selectedValidatorAddress.asStateFlow()

    val isLoading = validatorsState.map { it.isLoading() }.shareInBackground()

    val isEmpty = validatorsState.map { !it.isLoading() && it.dataOrNull.isNullOrEmpty() }.shareInBackground()

    private val selectedValidator = combine(rows, selectedAddress) { rows, address ->
        rows.firstOrNull { it.validator.address == address && it.validator.selectable }
    }.shareInBackground()

    val buttonState = selectedValidator.map { if (it != null) ButtonState.NORMAL else ButtonState.DISABLED }.shareInBackground()

    val buttonLabel = selectedValidator.map { row ->
        row?.let { resources.getString(R.string.bittensor_select_named_validator, it.validator.validator.name.ifBlank { it.validator.address }) }
            ?: resources.getString(R.string.bittensor_select_validator)
    }.shareInBackground()

    fun select(row: BittensorValidatorUi) = launchUnit {
        if (row.validator.selectable && row in rows.first()) selectedValidatorAddress.value = row.validator.address
    }

    fun infoClicked(row: BittensorValidatorUi) = launchUnit {
        try {
            externalActions.showAddressActions(row.validator.address, chainRegistry.getChain(ChainGeneses.BITTENSOR))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportFailure(e)
        }
    }

    fun confirmClicked() = launchUnit {
        val row = selectedValidator.first() ?: return@launchUnit
        communicator.respond(BittensorSelectValidatorCommunicator.Response(request.subnetId, row.validator.address))
        router.back()
    }

    fun back() = router.back()

    private fun reportFailure(cause: Throwable) {
        logger.log("[BittensorSelectValidator] Failed to load validators or address actions")
        logger.log(cause)
        showError(resources.getString(R.string.common_retry_message))
    }
}
