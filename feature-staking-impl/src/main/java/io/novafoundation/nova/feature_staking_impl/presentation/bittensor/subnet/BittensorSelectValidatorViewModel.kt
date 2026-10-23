package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.analytics.BittensorDataEndpoint
import io.novafoundation.nova.analytics.BittensorScreen
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorReadAnalytics
import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.StakingAnalyticsTarget
import io.novafoundation.nova.analytics.ValidatorChangeSource
import io.novafoundation.nova.analytics.AnalyticsService

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.address.AccountIdKey
import java.math.BigDecimal
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.formatAsPercentage
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.bittensorYieldColor
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorSorting
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.sortBittensorValidators
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class BittensorSelectValidatorViewModel(
    private val analytics: AnalyticsService,
    private val request: BittensorSelectValidatorCommunicator.Request,
    private val communicator: BittensorSelectValidatorCommunicator,
    private val bittensorValidatorInteractor: BittensorValidatorInteractor,
    private val icons: AddressIconGenerator,
    private val router: StakingRouter,
    private val externalActions: ExternalActions.Presentation,
    private val resources: ResourceManager,
    private val logger: Logger,
    private val retryController: BittensorRetryController,
    private val tokenFormatter: TokenFormatter,
    private val subnetInteractor: BittensorSubnetInteractor
) : BaseViewModel(), ExternalActions by externalActions {

    private val validatorsReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.VALIDATORS, BittensorScreen.VALIDATOR_SELECT)
    private val chainRpcReads = BittensorReadAnalytics(analytics, BittensorDataEndpoint.CHAIN_RPC, BittensorScreen.VALIDATOR_SELECT)

    val retryEvents = retryController.events

    val filters = MutableStateFlow(request.selectionState.filtersFor(BittensorNetuid.from(request.subnetId)))

    private val query = MutableStateFlow("")

    private val netuid = BittensorNetuid.from(request.subnetId)

    private val yields = retryController.observe(
        step = "validator yields",
        analytics = validatorsReads,
        transform = { source ->
            source.onStart { emit(emptyMap()) }.catch { cause ->
                if (cause is CancellationException) throw cause
                emit(emptyMap())
            }
        }
    ) {
        filters.flatMapLatest { filter ->
            flowOf { bittensorValidatorInteractor.yields(netuid, includeUnreliable = !filter.hideUnusualApy) }
        }
    }
        .shareInBackground()

    private val validatorsState = observeBittensorValidatorDirectory(
        retryController = retryController,
        loadDirectory = { bittensorValidatorInteractor.directory(netuid) },
        rankDirectory = { bittensorValidatorInteractor.rankDirectory(netuid, it) }
    )
        .mapLoading { directory -> directory.validators.map { mapValidator(it) } }
        .shareInBackground()

    private val selectedValidatorAddress = MutableStateFlow(request.selectedAddress)

    val selectedAddress = selectedValidatorAddress.asStateFlow()

    private val stakeDisplay = retryController.observe(
        step = "validator reported stake price",
        analytics = chainRpcReads,
        transform = { source ->
            source.onStart { emit(null) }.catch { cause ->
                if (cause is CancellationException) throw cause
                emit(null)
            }
        }
    ) {
        flowOf<BittensorValidatorStakeDisplay?> {
            val asset = bittensorValidatorInteractor.chain().utilityAsset
            val price = when (val netuid = netuid) {
                BittensorNetuid.Root -> BigDecimal.ONE
                is BittensorNetuid.Subnet -> subnetInteractor.subnet(netuid).priceInRao?.toBigDecimal(asset.precision.value)
            }
            BittensorValidatorStakeDisplay(asset.symbol, price)
        }
    }
        .shareInBackground()

    private val filteredList = combine(validatorsState, filters, selectedAddress, yields, stakeDisplay) { state, filters, address, yields, stake ->
        BittensorValidatorRows(
            rows = mapValidatorRows(state, filters.sorting, address, yields, filters.hideUnusualApy, stake),
            total = state.dataOrNull?.size
        )
    }
        .shareInBackground()

    private val filteredRows = filteredList.map { it.rows }
        .shareInBackground()

    // Shown and total counts come from one directory snapshot, so the header can never read "more of fewer".
    private val visibleList = combine(filteredList, query) { list, text ->
        list.copy(rows = list.rows.filter { matchesBittensorValidator(it.eligibility, text) })
    }
        .shareInBackground()

    val rows = visibleList.map { it.rows }
        .shareInBackground()

    val header = combine(visibleList, filters) { list, filters ->
        val total = list.total
        BittensorValidatorListHeader(
            if (total == null) {
                resources.getString(R.string.bittensor_validator_list_count_loading)
            } else {
                resources.getString(R.string.bittensor_validator_list_count, list.rows.size, total)
            },
            resources.getString(R.string.bittensor_validator_list_sort, sortingDisplay(filters.sorting).uppercase(Locale.getDefault()))
        )
    }
        .shareInBackground()

    private val _inactiveValidatorEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val inactiveValidatorEvents: SharedFlow<Unit> = _inactiveValidatorEvents

    val isLoading = validatorsState.map { it.isLoading() }
        .shareInBackground()

    val isEmpty = combine(rows, isLoading) { rows, loading -> rows.isEmpty() && !loading }
        .shareInBackground()

    val canRemoveFilters = combine(filteredRows, validatorsState, filters) { rows, state, filters ->
        !state.isLoading() && !state.dataOrNull.isNullOrEmpty() && rows.isEmpty() && filters.hideUnusualApy
    }.shareInBackground()

    val emptyMessage = combine(canRemoveFilters, query) { filtered, query ->
        when {
            filtered -> resources.getString(R.string.bittensor_validators_filtered_empty)
            query.isNotBlank() -> resources.getString(R.string.bittensor_validators_search_empty, query.trim())
            else -> resources.getString(R.string.bittensor_no_validators)
        }
    }.shareInBackground()

    private val selectedValidator = combine(filteredRows, selectedAddress) { rows, address ->
        rows.firstOrNull { it.eligibility.address == address && it.eligibility.canSelect }
    }
        .shareInBackground()

    val buttonState = selectedValidator.map { if (it != null) ButtonState.NORMAL else ButtonState.DISABLED }
        .shareInBackground()

    val buttonLabel = selectedValidator.map { row ->
        if (row == null) return@map resources.getString(R.string.bittensor_select_validator)

        val name = bittensorValidatorDisplayName(row.eligibility)
        resources.getString(R.string.bittensor_select_named_validator, name)
    }
        .shareInBackground()

    private fun mapValidatorRows(
        state: ExtendedLoadingState<List<BittensorValidatorUi>>,
        sorting: BittensorValidatorSorting,
        address: String?,
        rates: Map<AccountIdKey, BigDecimal>,
        hideUnusual: Boolean,
        stake: BittensorValidatorStakeDisplay?
    ): List<BittensorValidatorUi> {
        val rows = state.dataOrNull.orEmpty()
        val rowsByAddress = rows.associateBy { it.eligibility.address }
        return sortBittensorValidators(
            validators = rows.map { it.eligibility },
            sorting = sorting,
            selectedAddress = address,
            locale = Locale.getDefault(),
            yields = rates,
            hideUnusualApy = hideUnusual,
            preselectedAddress = request.selectedAddress,
            includeUnavailable = true
        ).map { validator ->
            val rate = rates[validator.validator.hotkey]
            val nativeStake = validator.validator.reportedStake?.let { amount -> stake?.nativePrice?.let { amount * it } }
            val amount = nativeStake?.let { tokenFormatter.formatToken(it, requireNotNull(stake).symbol).toString() } ?: "—"
            val take = validator.commissionRate.movePointRight(2).setScale(2, java.math.RoundingMode.HALF_EVEN).toStripTrailingZerosString()
            val subtitle = resources.getString(R.string.bittensor_validator_list_subtitle, amount, take)
            rowsByAddress.getValue(validator.address).copy(apy = rate?.formatAsPercentage(), apyColorRes = bittensorYieldColor(rate), subtitle = subtitle)
        }
    }

    fun search(text: String) {
        query.value = text
    }

    fun applyFilters(value: BittensorValidatorFilters) {
        filters.value = value
    }

    fun removeFilters() {
        filters.value = filters.value.copy(hideUnusualApy = false)
    }

    fun select(row: BittensorValidatorUi) = launchUnit {
        if (row !in rows.first()) return@launchUnit
        when {
            row.eligibility.inactive -> _inactiveValidatorEvents.tryEmit(Unit)
            row.eligibility.canSelect -> selectedValidatorAddress.value = row.eligibility.address
        }
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
        analytics.track(
            BittensorAnalyticsEvent.ValidatorChanged(
                if (request.subnetId == 0) StakingAnalyticsTarget.ROOT else StakingAnalyticsTarget.SUBNET,
                if (query.value.isNotBlank()) ValidatorChangeSource.SEARCH else ValidatorChangeSource.LIST,
                filters.value.hideUnusualApy
            )
        )
        communicator.respond(BittensorSelectValidatorCommunicator.Response(request.subnetId, row.eligibility.address, selectionState()))
        router.back()
    }

    fun back() {
        communicator.respond(BittensorSelectValidatorCommunicator.Response(null, null, selectionState()))
        router.back()
    }

    private fun selectionState(): BittensorSelectionState = request.selectionState.withFilters(netuid, filters.value)

    private fun sortingDisplay(sorting: BittensorValidatorSorting): String = resources.getString(
        when (sorting) {
            BittensorValidatorSorting.APY -> R.string.bittensor_sort_apy
            BittensorValidatorSorting.TOTAL_STAKE -> R.string.staking_validator_total_stake
            BittensorValidatorSorting.NAME -> R.string.common_name
        }
    )

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
