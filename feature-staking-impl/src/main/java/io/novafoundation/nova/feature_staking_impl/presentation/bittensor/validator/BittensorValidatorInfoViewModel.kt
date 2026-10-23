package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.validator

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
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
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorSubnetCatalogue
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorCoverage
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorCoverageInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorAmountFormatter
import io.novafoundation.nova.feature_wallet_api.domain.model.toFiatOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flow
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.runtime.ext.utilityAsset

class BittensorValidatorInfoViewModel(
    private val payload: BittensorValidatorInfoPayload,
    private val router: StakingRouter,
    private val validatorInteractor: BittensorValidatorInteractor,
    private val coverageInteractor: BittensorValidatorCoverageInteractor,
    private val subnetInteractor: BittensorSubnetInteractor,
    private val subnetCatalogue: BittensorValidatorSubnetCatalogue,
    private val addressIconGenerator: AddressIconGenerator,
    private val amountFormatter: BittensorAmountFormatter,
    private val tokenFormatter: TokenFormatter,
    private val externalActions: ExternalActions.Presentation,
    private val resources: ResourceManager,
    private val logger: Logger
) : BaseViewModel(), ExternalActions by externalActions {

    private val netuid = BittensorNetuid.from(payload.subnetId)

    val account = flowOf {
        addressIconGenerator.createAccountAddressModel(validatorInteractor.chain(), payload.address, payload.name)
    }
        .shareInBackground()

    val status = BittensorValidatorStatusUi(
        text = resources.getString(if (payload.inactive) R.string.staking_nominator_status_inactive else R.string.common_active),
        style = if (payload.inactive) TableCellView.FieldStyle.SECONDARY else TableCellView.FieldStyle.POSITIVE
    )

    /** Null means no reported stake or no FRESH subnet price to value it at. Fiat follows the native price. */
    val totalStake: Flow<AmountModel?> = flow {
        var emitted = false
        val amounts = flow<AmountModel?> {
            val stake = payload.reportedStake?.let { nativeStake(it) }
            if (stake == null) {
                emit(null)
                return@flow
            }
            val symbol = validatorInteractor.chain().utilityAsset.symbol
            emit(AmountModel(tokenFormatter.formatToken(stake, symbol), null))
            emitAll(
                subnetInteractor.observeAsset().map { asset ->
                    amountFormatter.amount(stake, asset.token.configuration.symbol, asset.token.toFiatOrNull(stake), asset.token.currency)
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
        .shareInBackground()

    val take: String = resources.getString(
        R.string.bittensor_validator_info_take_value,
        payload.commissionRate.movePointRight(2).setScale(2, RoundingMode.HALF_EVEN).toStripTrailingZerosString()
    )

    /** The reliable rate only, whatever the list filter shows; a failed read shows no rate. */
    val reward: Flow<BittensorValidatorRewardUi> = flowOf<BigDecimal?> { validatorInteractor.yields(netuid, includeUnreliable = false)[hotkey()] }
        .catch { cause ->
            if (cause is CancellationException) throw cause
            logger.log("[BittensorValidatorInfo] Validator reward rate unavailable")
            logger.log(cause)
            emit(null)
        }
        .map(::formatReward)
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

    /** Root stake is TAO; subnet stake is valued at the FRESH backend price, as iOS values it on this screen. */
    private suspend fun nativeStake(reportedStake: BigDecimal): BigDecimal? = when (val netuid = netuid) {
        BittensorNetuid.Root -> reportedStake
        is BittensorNetuid.Subnet -> subnetCatalogue.freshPrice(netuid)?.let { price ->
            reportedStake * price.toBigDecimal(validatorInteractor.chain().utilityAsset.precision.value)
        }
    }

    private fun formatReward(rate: BigDecimal?) = BittensorValidatorRewardUi(
        value = rate?.formatAsPercentage() ?: "—",
        detail = rate?.let { resources.getString(R.string.bittensor_validator_info_reward_average) },
        style = if (rate != null) TableCellView.FieldStyle.POSITIVE else TableCellView.FieldStyle.SECONDARY
    )

    private fun formatCoverage(coverage: BittensorValidatorCoverage?): CharSequence = coverage?.let {
        resources.getString(R.string.bittensor_validator_info_coverage_value, it.permitted, it.total)
    } ?: "—"
}

data class BittensorValidatorStatusUi(val text: String, val style: TableCellView.FieldStyle)

data class BittensorValidatorRewardUi(val value: String, val detail: String?, val style: TableCellView.FieldStyle)
