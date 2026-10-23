package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.formatAsPercentage
import io.novafoundation.nova.common.utils.formatting.toStripTrailingZerosString
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorCoverage
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorCoverageInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flow
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig

class BittensorValidatorInfoViewModel(
    private val payload: BittensorValidatorInfoPayload,
    private val router: StakingRouter,
    private val validatorInteractor: BittensorValidatorInteractor,
    private val coverageInteractor: BittensorValidatorCoverageInteractor,
    private val subnetInteractor: BittensorSubnetInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val amountFormatter: BittensorAmountFormatter,
    private val tokenFormatter: TokenFormatter,
    private val externalActions: ExternalActions.Presentation,
    private val retryController: BittensorRetryController,
    private val resources: ResourceManager,
    private val logger: Logger
) : BaseViewModel(), ExternalActions by externalActions {

    private val netuid = BittensorNetuid.from(payload.subnetId)

    val retryEvents = retryController.events

    // Without the list row the listing names the validator and the chain reports its take and status.
    private val detail: Flow<ExtendedLoadingState<BittensorValidatorInfoPayload.Detail>> = when (val known = payload.detail) {
        null -> retryController.observe(
            step = "validator detail",
            transform = { it.withSafeLoading() }
        ) {
            flowOf { validatorInteractor.chainValidator(netuid, hotkey()).toValidatorInfoDetail() }
        }
        else -> flowOf { ExtendedLoadingState.Loaded(known) }
    }
        .shareInBackground()

    private val loadedDetail = detail.filterLoaded()
        .shareInBackground()

    val account = detail.map { it.dataOrNull?.name }
        .distinctUntilChanged()
        .map { name -> addressIconGenerator.createAccountAddressModel(validatorInteractor.chain(), payload.address, name) }
        .shareInBackground()

    val status = loadedDetail.map { detail ->
        BittensorValidatorStatusUi(
            text = resources.getString(if (detail.inactive) R.string.staking_nominator_status_inactive else R.string.common_active),
            style = if (detail.inactive) TableCellView.FieldStyle.SECONDARY else TableCellView.FieldStyle.POSITIVE
        )
    }
        .shareInBackground()

    /** Null means no reported stake or no FRESH subnet price to value it at. Fiat follows the native price. */
    val totalStake: Flow<AmountModel?> = loadedDetail.flatMapLatest { stakeAmounts(it.reportedStake) }
        .shareInBackground()

    // Root stake is TAO itself; subnet stake is alpha valued in TAO at today's price (as on iOS)
    val totalStakeDescription: String = resources.getString(
        if (netuid == BittensorNetuid.Root) {
            R.string.bittensor_validator_info_total_staked_root
        } else {
            R.string.bittensor_validator_info_total_staked_subnet
        }
    )

    val takeDescription: String = resources.getString(R.string.bittensor_validator_info_take_description)

    val take = loadedDetail.map { detail ->
        resources.getString(
            R.string.bittensor_validator_info_take_value,
            detail.commissionRate.movePointRight(2).setScale(2, RoundingMode.HALF_EVEN).toStripTrailingZerosString()
        )
    }
        .shareInBackground()

    /** The reliable rate only, whatever the list filter shows; a failed read shows no rate. */
    private val rewardRate = flowOf<BigDecimal?> { validatorInteractor.yields(netuid, includeUnreliable = false)[hotkey()] }
        .catch { cause ->
            if (cause is CancellationException) throw cause
            logger.log("[BittensorValidatorInfo] Validator reward rate unavailable")
            logger.log(cause)
            emit(null)
        }

    // The reward shows with the rest of the validator's staking details.
    val reward: Flow<BittensorValidatorRewardUi> = combine(loadedDetail, rewardRate) { _, rate -> formatReward(rate) }
        .shareInBackground()

    val coverage: Flow<ExtendedLoadingState<CharSequence?>> = flowOf { coverageInteractor.coverage(hotkey()) }
        .withSafeLoading()
        .map { state ->
            when (state) {
                is ExtendedLoadingState.Loading -> ExtendedLoadingState.Loading
                is ExtendedLoadingState.Loaded -> ExtendedLoadingState.Loaded(formatCoverage(state.data))
                is ExtendedLoadingState.Error -> {
                    // Coverage is supplementary: a failed chain read reads as unknown rather than blocking the screen.
                    logger.log("[BittensorValidatorInfo] Validator coverage unavailable")
                    logger.log(state.exception)
                    ExtendedLoadingState.Loaded("—")
                }
            }
        }
        .shareInBackground()

    fun accountClicked() = launchUnit {
        try {
            externalActions.showAddressActions(payload.address, validatorInteractor.chain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log("[BittensorValidatorInfo] Address actions unavailable")
            logger.log(e)
            showError(resources.getString(R.string.common_retry_message))
        }
    }

    fun back() {
        router.back()
    }

    private suspend fun hotkey() = AccountIdKey(validatorInteractor.chain().accountIdOf(payload.address))

    private fun stakeAmounts(reportedStake: BigDecimal?): Flow<AmountModel?> = flow {
        var emitted = false
        val amounts = flow<AmountModel?> {
            val stake = reportedStake?.let { validatorInteractor.stakeInNative(netuid, it) }
            if (stake == null) {
                emit(null)
                return@flow
            }
            val symbol = validatorInteractor.chain().utilityAsset.symbol
            emit(AmountModel(tokenFormatter.formatToken(stake, symbol, TokenConfig(useCompactAbbreviation = true)), null))
            emitAll(
                subnetInteractor.observeAsset().map { asset ->
                    amountFormatter.compactAmount(stake, asset.token.configuration.symbol, asset.token.toFiatOrNull(stake), asset.token.currency)
                }
            )
        }
        emitAll(
            amounts
                .onEach { emitted = true }
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    logger.log("[BittensorValidatorInfo] Validator stake value unavailable")
                    logger.log(cause)
                    // Keep the amount already shown; otherwise resolve the cell instead of leaving it loading.
                    if (!emitted) emit(null)
                }
        )
    }

    private fun formatReward(rate: BigDecimal?) = BittensorValidatorRewardUi(
        value = rate?.let { resources.getString(R.string.staking_apy, it.formatAsPercentage()) } ?: "—",
        detail = rate?.let { resources.getString(R.string.bittensor_validator_info_reward_average) },
        style = if (rate != null) TableCellView.FieldStyle.POSITIVE else TableCellView.FieldStyle.SECONDARY
    )

    private fun formatCoverage(coverage: BittensorValidatorCoverage?): CharSequence = coverage?.let {
        resources.getString(R.string.bittensor_validator_info_coverage_value, it.permitted, it.total)
    } ?: "—"
}

data class BittensorValidatorStatusUi(val text: String, val style: TableCellView.FieldStyle)

data class BittensorValidatorRewardUi(val value: String, val detail: String?, val style: TableCellView.FieldStyle)
