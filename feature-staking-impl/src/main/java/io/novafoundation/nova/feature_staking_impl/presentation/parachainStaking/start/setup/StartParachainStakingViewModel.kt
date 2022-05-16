package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.StartParachainStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenRequester
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.openRequest
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.mapCollatorParcelModelToCollator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.model.ConfirmStartParachainStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.model.SelectCollatorModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards.RealParachainStakingRewardsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards.connectWith
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.startParachainStakingValidationFailure
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.math.BigDecimal

class StartParachainStakingViewModel(
    private val router: ParachainStakingRouter,
    private val selectCollatorInterScreenRequester: SelectCollatorInterScreenRequester,
    private val interactor: StartParachainStakingInteractor,
    private val rewardsComponentFactory: RealParachainStakingRewardsComponentFactory,
    private val singleAssetSharedState: SingleAssetSharedState,
    private val addressIconGenerator: AddressIconGenerator,
    private val assetUseCase: AssetUseCase,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: StartParachainStakingValidationSystem,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
) : BaseViewModel(),
    Retriable,
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin {

    private val validationInProgress = MutableStateFlow(false)

    private val assetFlow = assetUseCase.currentAssetFlow()
        .share()

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        balanceField = Asset::transferable,
        balanceLabel = R.string.wallet_balance_transferable
    )

    private val selectedCollator: Flow<Collator?> = selectCollatorInterScreenRequester.responseFlow.map { response ->
        mapCollatorParcelModelToCollator(response.collator)
    }
        .onStart<Collator?> { emit(null) }
        .shareInBackground()

    val selectedCollatorModel = selectedCollator.map { collator ->
        collator?.let {
            val chain = singleAssetSharedState.chain()

            val addressModel = addressIconGenerator.createAccountAddressModel(
                chain = chain,
                address = chain.addressOf(it.accountIdHex.fromHex()),
                name = it.identity?.display
            )

            SelectCollatorModel(
                addressModel = addressModel
            )
        }
    }.shareInBackground()

    val minimumStake = selectedCollator.map {
        val minimumStake = it?.minimumStakeToGetRewards ?: interactor.defaultMinimumStake()
        val asset = assetFlow.first()

        mapAmountToAmountModel(minimumStake, asset)
    }.shareInBackground()

    val rewardsComponent = rewardsComponentFactory.create(
        parentScope = this,
        assetFlow = assetFlow
    )

    val title = assetFlow.map { resourceManager.getString(R.string.staking_stake_format, it.token.configuration.symbol) }
        .inBackground()
        .share()

    val buttonState = combine(
        validationInProgress,
        selectedCollator,
        amountChooserMixin.amountInput
    ) { validationInProgress, collator, amountInput ->
        when {
            validationInProgress -> DescriptiveButtonState.Loading
            collator == null -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.staking_parachain_select_collator_hint))
            amountInput.isEmpty() -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_enter_amount))
            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        }
    }

    init {
        rewardsComponent connectWith amountChooserMixin
        rewardsComponent connectWith selectedCollator.map { it?.accountIdHex?.fromHex() }

        feeLoaderMixin.connectWith(
            amountMixin = amountChooserMixin,
            scope = this,
            feeConstructor = interactor::estimateFee,
            onRetryCancelled = ::backClicked
        )
    }

    fun selectCollatorClicked() = launch {
        selectCollatorInterScreenRequester.openRequest()
    }

    fun nextClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    private fun maybeGoToNext() = requireFee { fee ->
        launch {
            val collator = selectedCollator.first() ?: return@launch
            val amount = amountChooserMixin.amount.first()

            val payload = StartParachainStakingValidationPayload(
                amount = amount,
                fee = fee,
                asset = assetFlow.first(),
                collator = collator
            )

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                validationFailureTransformer = { startParachainStakingValidationFailure(it, resourceManager) },
                progressConsumer = validationInProgress.progressConsumer()
            ) {
                validationInProgress.value = false

                goToNextStep(fee = fee, amount = amount, collator = collator)
            }
        }
    }

    private fun goToNextStep(
        fee: BigDecimal,
        amount: BigDecimal,
        collator: Collator,
    ) {
        val payload = ConfirmStartParachainStakingPayload(
            collatorId = collator.accountIdHex.fromHex(),
            amount = amount,
            fee = fee
        )

        router.openConfirmStartStaking(payload)
    }

    private fun requireFee(block: (BigDecimal) -> Unit) = feeLoaderMixin.requireFee(
        block,
        onError = { title, message -> showError(title, message) }
    )
}
