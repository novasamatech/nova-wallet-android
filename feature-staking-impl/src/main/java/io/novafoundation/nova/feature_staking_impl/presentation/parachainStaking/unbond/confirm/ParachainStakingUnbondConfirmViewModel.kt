package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper

import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.ParachainStakingUnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.details.parachain
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.mapCollatorParcelModelToCollator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.collators.collatorAddressModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.mappers.mapCollatorToDetailsParcelModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.confirm.model.ParachainStakingUnbondConfirmPayload
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.hints.ParachainStakingUnbondHintsMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.parachainStakingUnbondPayloadAutoFix
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.parachainStakingUnbondValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeFromParcel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.extensions.fromHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParachainStakingUnbondConfirmViewModel(
    private val router: ParachainStakingRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val validationSystem: ParachainStakingUnbondValidationSystem,
    private val interactor: ParachainStakingUnbondInteractor,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val externalActions: ExternalActions.Presentation,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val validationExecutor: ValidationExecutor,
    private val collatorsUseCase: CollatorsUseCase,
    private val payload: ParachainStakingUnbondConfirmPayload,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
    selectedAccountUseCase: SelectedAccountUseCase,
    assetUseCase: AssetUseCase,
    walletUiUseCase: WalletUiUseCase,
    hintsMixinFactory: ParachainStakingUnbondHintsMixinFactory,
    private val amountFormatter: AmountFormatter
) : BaseViewModel(),
    Retriable,
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin,
    ExternalActions by externalActions,
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

    private val decimalFee = mapFeeFromParcel(payload.fee)

    val hintsMixin = hintsMixinFactory.create(coroutineScope = this)

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    private val collator by lazyAsync(Dispatchers.Default) {
        mapCollatorParcelModelToCollator(payload.collator)
    }

    val currentAccountModelFlow = selectedAccountUseCase.selectedMetaAccountFlow().map {
        addressIconGenerator.createAccountAddressModel(
            chain = selectedAssetState.chain(),
            account = it,
            name = null
        )
    }.shareInBackground()

    val amountModel = assetFlow.map { asset ->
        amountFormatter.formatAmountToAmountModel(payload.amount, asset)
    }
        .shareInBackground()

    val walletFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val collatorAddressModel = flowOf {
        addressIconGenerator.collatorAddressModel(collator(), selectedAssetState.chain())
    }.shareInBackground()

    private val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: StateFlow<Boolean> = _showNextProgress

    init {
        setInitialFee()
    }

    fun confirmClicked() {
        sendTransactionIfValid()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() = launch {
        val address = currentAccountModelFlow.first().address

        externalActions.showAddressActions(address, selectedAssetState.chain())
    }

    fun collatorClicked() = launch {
        val parcel = withContext(Dispatchers.Default) {
            mapCollatorToDetailsParcelModel(collator())
        }

        router.openCollatorDetails(StakeTargetDetailsPayload.parachain(parcel, collatorsUseCase))
    }

    private fun setInitialFee() = launch {
        feeLoaderMixin.setFee(decimalFee)
    }

    private fun sendTransactionIfValid() = launch {
        val payload = ParachainStakingUnbondValidationPayload(
            amount = payload.amount,
            fee = decimalFee,
            collator = collator(),
            asset = assetFlow.first()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { parachainStakingUnbondValidationFailure(it, resourceManager, amountFormatter) },
            autoFixPayload = ::parachainStakingUnbondPayloadAutoFix,
            progressConsumer = _showNextProgress.progressConsumer()
        ) {
            sendTransaction()
        }
    }

    private fun sendTransaction() = launch {
        val token = assetFlow.first().token
        val amountInPlanks = token.planksFromAmount(payload.amount)

        interactor.unbond(
            amount = amountInPlanks,
            collator = payload.collator.accountIdHex.fromHex()
        )
            .onFailure(::showError)
            .onSuccess {
                showToast(resourceManager.getString(R.string.common_transaction_submitted))

                startNavigation(it.submissionHierarchy) { router.returnToStakingMain() }
            }

        _showNextProgress.value = false
    }
}
