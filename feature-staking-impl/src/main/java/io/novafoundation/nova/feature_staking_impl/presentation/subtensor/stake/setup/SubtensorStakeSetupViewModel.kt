package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.setup

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.ColoredText
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.subtensor.extrinsic.addStakeLimit
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.confirm.SubtensorStakeConfirmPayload
import io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.validatorPicker.SubtensorPickedValidator
import io.novafoundation.nova.feature_staking_impl.presentation.view.stakingTarget.StakingTargetModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.withAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeToParcel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitOptionalFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger

class SubtensorStakeSetupViewModel(
    private val router: StakingRouter,
    private val dashboardRouter: StakingDashboardRouter,
    private val resourceManager: ResourceManager,
    private val stakingSharedState: StakingSharedState,
    private val extrinsicService: ExtrinsicService,
    private val assetUseCase: AssetUseCase,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    maxActionProviderFactory: MaxActionProviderFactory,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
    val netuid: Int,
    private val subnetName: String?,
) : BaseViewModel() {

    val isRoot: Boolean = netuid == SubtensorStakingConstants.ROOT_NETUID

    val titleText: StateFlow<String> = MutableStateFlow(resolveTitle()).asStateFlow()

    private val _selectedValidator = MutableStateFlow<SubtensorPickedValidator?>(null)
    val selectedValidator: StateFlow<SubtensorPickedValidator?> = _selectedValidator.asStateFlow()

    val validatorTargetModel: StateFlow<StakingTargetModel> = _selectedValidator
        .map(::buildTargetModel)
        .stateIn(viewModelScope, SharingStarted.Eagerly, buildTargetModel(null))

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    val toastEvents = androidx.lifecycle.MutableLiveData<Event<String>>()

    private val assetFlow = assetUseCase.currentAssetFlow().shareInBackground()

    private val selectedChainAsset = assetFlow
        .map { it.token.configuration }
        .shareInBackground()

    private val stakeableBalance = assetFlow
        .map { it.token.configuration.withAmount(it.transferableInPlanks) }
        .shareInBackground()

    /** Fee row mixin — wired to amount + hotkey via [connectWith] in init. */
    val originFeeMixin = feeLoaderMixinFactory.createDefault(this, selectedChainAsset)

    private val maxActionProvider = maxActionProviderFactory.createCustom(viewModelScope) {
        stakeableBalance.asMaxAmountProvider().deductFee(originFeeMixin)
    }

    val amountChooserMixin: AmountChooserMixin.Presentation = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = assetFlow,
        maxActionProvider = maxActionProvider,
    )

    val canContinue: StateFlow<Boolean> = combine(
        _selectedValidator,
        amountChooserMixin.amount,
        _submitting,
    ) { picked, amount, submitting ->
        !submitting && picked != null && amount > BigDecimal.ZERO
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        router.subtensorSelectedValidatorFlow
            .filterNotNull()
            .onEach { picked -> _selectedValidator.value = picked }
            .launchIn(viewModelScope)

        listenFee()
    }

    fun selectValidatorClicked() {
        router.openSubtensorValidatorPicker(netuid)
    }

    fun continueClicked() {
        if (_submitting.value) return
        viewModelScope.launch {
            val picked = _selectedValidator.value ?: return@launch
            val amountTao = amountChooserMixin.amount.first()
            if (amountTao <= BigDecimal.ZERO) return@launch
            openConfirm(picked, amountTao)
        }
    }

    fun backClicked() {
        if (_submitting.value) return
        router.back()
    }

    private fun listenFee() {
        originFeeMixin.connectWith(
            inputSource1 = amountChooserMixin.backPressuredPlanks,
            inputSource2 = _selectedValidator,
        ) { _, amountPlanks, picked ->
            val realAmount = amountPlanks.takeIf { it > BigInteger.ZERO } ?: BigInteger.valueOf(1_000_000_000L)
            val realHotkey = picked?.hotkeyAddress?.let { runCatching { it.toAccountId() }.getOrNull() } ?: ByteArray(32)
            extrinsicService.estimateFee(
                chain = stakingSharedState.chain(),
                origin = TransactionOrigin.SelectedWallet,
            ) {
                addStakeLimit(hotkey = realHotkey, netuid = netuid, amount = realAmount)
            }
        }
    }

    private fun openConfirm(picked: SubtensorPickedValidator, amountTao: BigDecimal) {
        _submitting.value = true
        viewModelScope.launch {
            val fee = originFeeMixin.awaitOptionalFee()

            val asset = assetFlow.first()
            val precision = asset.token.configuration.precision.value.toInt()
            val planks = amountTao.multiply(BigDecimal.TEN.pow(precision)).toBigInteger()

            // Min-stake guard: chain rejects anything below MIN_NOMINATOR_STAKE_RAO
            // (0.01 TAO = 10_000_000 RAO). iOS leaves this to the chain, but
            // toasting upfront avoids the wasted-gas round-trip and spares the
            // user a confusing extrinsic-rejection error.
            if (planks < SubtensorStakingConstants.MIN_NOMINATOR_STAKE_RAO) {
                _submitting.value = false
                toastEvents.value = Event(resourceManager.getString(R.string.subtensor_stake_below_min))
                return@launch
            }

            val hotkeyBytes = runCatching { picked.hotkeyAddress.toAccountId() }.getOrNull()
            if (hotkeyBytes == null) {
                _submitting.value = false
                toastEvents.value = Event(resourceManager.getString(R.string.subtensor_setup_invalid_validator))
                return@launch
            }

            if (fee == null) {
                _submitting.value = false
                toastEvents.value = Event(resourceManager.getString(R.string.subtensor_setup_submit_failed))
                return@launch
            }

            _submitting.value = false
            router.openSubtensorStakeConfirm(
                SubtensorStakeConfirmPayload(
                    netuid = netuid,
                    hotkeyAddress = picked.hotkeyAddress,
                    validatorIdentity = picked.identity,
                    amountInPlanks = planks,
                    fee = mapFeeToParcel(fee),
                    subnetName = subnetName,
                )
            )
        }
    }

    private fun resolveTitle(): String = when {
        isRoot -> resourceManager.getString(R.string.subtensor_stake_more)
        !subnetName.isNullOrBlank() -> subnetName
        else -> "Subnet $netuid"
    }

    private fun buildTargetModel(picked: SubtensorPickedValidator?): StakingTargetModel {
        if (picked == null) {
            return StakingTargetModel(
                title = resourceManager.getString(R.string.subtensor_setup_select_validator),
                subtitle = null,
                icon = null,
            )
        }
        val short = shorten(picked.hotkeyAddress)
        // Mirror iOS StackAccountSelectionCell: identity as the bold title,
        // truncated hotkey as the gray subtitle. When identity is unknown
        // the hotkey takes the title slot.
        val identity = picked.identity?.takeIf { it.isNotBlank() }
        return if (identity != null) {
            StakingTargetModel(
                title = identity,
                subtitle = ColoredText(text = short, colorRes = R.color.text_secondary),
                icon = null,
            )
        } else {
            StakingTargetModel(
                title = short,
                subtitle = null,
                icon = null,
            )
        }
    }

    private fun shorten(address: String): String =
        if (address.length <= 10) address else "${address.take(6)}...${address.takeLast(4)}"
}
