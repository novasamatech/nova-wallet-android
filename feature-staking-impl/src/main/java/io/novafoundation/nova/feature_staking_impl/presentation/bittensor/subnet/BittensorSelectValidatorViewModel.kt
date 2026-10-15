package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.navigation.requireLastInput
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class BittensorSelectValidatorViewModel(
    private val communicator: BittensorSelectValidatorCommunicator,
    private val bittensorValidatorInteractor: BittensorValidatorInteractor,
    private val icons: AddressIconGenerator,
    private val router: StakingRouter,
    private val externalActions: ExternalActions.Presentation,
    private val resources: ResourceManager,
    private val logger: Logger,
    private val retryController: BittensorRetryController
) : BaseViewModel(), ExternalActions by externalActions {

    val retryEvents = retryController.events

    private val request = communicator.requireLastInput()

    private val validatorsState = retryController.observe("validators") {
        flowOf {
            val netuid = BittensorNetuid.from(request.subnetId)
            val validators = bittensorValidatorInteractor.validators(netuid)
            validators.map { mapValidator(it) }
        }
    }
        .shareInBackground()

    val rows = validatorsState.map { it.dataOrNull.orEmpty() }
        .shareInBackground()

    private val selectedValidatorAddress = MutableStateFlow(request.selectedAddress)

    val selectedAddress = selectedValidatorAddress.asStateFlow()

    val isLoading = validatorsState.map { it.isLoading() }
        .shareInBackground()

    val isEmpty = validatorsState.map { it.dataOrNull?.isEmpty() == true }
        .shareInBackground()

    private val selectedValidator = combine(rows, selectedAddress) { rows, address ->
        rows.firstOrNull { it.eligibility.address == address && it.eligibility.selectable }
    }
        .shareInBackground()

    val buttonState = selectedValidator.map { if (it != null) ButtonState.NORMAL else ButtonState.DISABLED }
        .shareInBackground()

    val buttonLabel = selectedValidator.map { row ->
        if (row == null) return@map resources.getString(R.string.bittensor_select_validator)

        val name = row.eligibility.validator.name.ifBlank { row.eligibility.address }
        resources.getString(R.string.bittensor_select_named_validator, name)
    }
        .shareInBackground()

    fun select(row: BittensorValidatorUi) = launchUnit {
        if (row.eligibility.selectable && row in rows.first()) selectedValidatorAddress.value = row.eligibility.address
    }

    fun infoClicked(row: BittensorValidatorUi) = launchUnit {
        try {
            externalActions.showAddressActions(row.eligibility.address, bittensorValidatorInteractor.chain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reportFailure(e)
        }
    }

    fun confirmClicked() = launchUnit {
        val row = selectedValidator.first() ?: return@launchUnit
        communicator.respond(BittensorSelectValidatorCommunicator.Response(request.subnetId, row.eligibility.address))
        router.back()
    }

    fun back() = router.back()

    private suspend fun mapValidator(validator: BittensorSubnetValidator): BittensorValidatorUi {
        val icon = icons.createAddressIcon(
            validator.validator.hotkey.value,
            AddressIconGenerator.SIZE_MEDIUM,
            AddressIconGenerator.BACKGROUND_TRANSPARENT
        )
        return BittensorValidatorUi(validator, icon)
    }

    private fun reportFailure(cause: Throwable) {
        logger.log("[BittensorSelectValidator] Failed to load validators or address actions")
        logger.log(cause)
        showError(resources.getString(R.string.common_retry_message))
    }
}
