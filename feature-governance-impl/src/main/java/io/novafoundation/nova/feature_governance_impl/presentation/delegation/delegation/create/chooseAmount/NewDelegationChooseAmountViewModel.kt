package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseAmount

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.model.toFeePaymentAsset
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.votesFor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseAmount.NewDelegationChooseAmountInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount.validation.ChooseDelegationAmountValidationPayload
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount.validation.ChooseDelegationAmountValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount.validation.chooseChooseDelegationAmountValidationFailure
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.conviction.ConvictionValuesProvider
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.AmountChipModel
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.LocksFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.common.newDelegationHints
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.common.newDelegationTitle
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.confirm.NewDelegationConfirmPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setAmountInput
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeToParcel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxBalanceType
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.create
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class NewDelegationChooseAmountViewModel(
    private val assetUseCase: AssetUseCase,
    private val amountChooserMixinFactory: AmountChooserMixin.Factory,
    private val interactor: NewDelegationChooseAmountInteractor,
    private val payload: NewDelegationChooseAmountPayload,
    private val resourceManager: ResourceManager,
    private val router: GovernanceRouter,
    private val validationSystem: ChooseDelegationAmountValidationSystem,
    private val validationExecutor: ValidationExecutor,
    private val locksChangeFormatter: LocksChangeFormatter,
    private val convictionValuesProvider: ConvictionValuesProvider,
    private val locksFormatter: LocksFormatter,
    private val resourcesHintsMixinFactory: ResourcesHintsMixinFactory,
    private val maxActionProviderFactory: MaxActionProviderFactory,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
) : BaseViewModel(),
    WithFeeLoaderMixinV2,
    Validatable by validationExecutor {

    val title = flowOf {
        resourceManager.newDelegationTitle(isEditMode = payload.isEditMode)
    }.shareInBackground()

    private val assetWithOption = assetUseCase.currentAssetAndOptionFlow()
        .shareInBackground()

    private val chainFlow = assetWithOption.map { it.option.assetWithChain.chain }

    private val selectedAsset = assetWithOption.map { it.asset }
        .shareInBackground()

    private val selectedChainAsset = selectedAsset.map { it.token.configuration }
        .shareInBackground()

    private val delegateAssistantFlow = interactor.delegateAssistantFlow(viewModelScope)

    override val originFeeMixin = feeLoaderMixinFactory.createDefault(this, selectedChainAsset)

    private val maxActionProvider = maxActionProviderFactory.create(
        viewModelScope = viewModelScope,
        assetInFlow = selectedAsset,
        feeLoaderMixin = originFeeMixin,
        maxBalanceType = MaxBalanceType.FREE
    )

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = selectedAsset,
        balanceField = Asset::free,
        maxActionProvider = maxActionProvider
    )

    val hintsMixin = resourcesHintsMixinFactory.newDelegationHints(viewModelScope)

    val convictionValues = convictionValuesProvider.convictionValues()
    val selectedConvictionIndex = MutableStateFlow(0)

    private val selectedConvictionFlow = selectedConvictionIndex.mapNotNull(convictionValues::valueAt)

    private val locksChangeFlow = combine(
        amountChooserMixin.amount,
        selectedConvictionFlow,
        delegateAssistantFlow
    ) { amount, conviction, delegateAssistant ->
        val amountPlanks = selectedAsset.first().token.planksFromAmount(amount)

        delegateAssistant.estimateLocksAfterDelegating(amountPlanks, conviction, selectedAsset.first())
    }
        .shareInBackground()

    val votesFormattedFlow = combine(
        amountChooserMixin.amount,
        selectedConvictionFlow
    ) { amount, conviction ->
        val votes = conviction.votesFor(amount)

        resourceManager.getString(R.string.referendum_votes_format, votes.format())
    }.shareInBackground()

    val locksChangeUiFlow = locksChangeFlow.map {
        locksChangeFormatter.mapLocksChangeToUi(it, selectedAsset.first(), displayPeriodFromWhenSame = false)
    }
        .shareInBackground()

    val amountChips = delegateAssistantFlow.map { voteAssistant ->
        val asset = selectedAsset.first()

        voteAssistant.reusableLocks().map { locksFormatter.formatReusableLock(it, asset) }
    }
        .shareInBackground()

    private val validationInProgressFlow = MutableStateFlow(false)

    val buttonState = combine(validationInProgressFlow, amountChooserMixin.amountInput) { inProgress, amountRaw ->
        when {
            inProgress -> DescriptiveButtonState.Loading
            amountRaw.isEmpty() -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_enter_amount))
            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        }
    }

    init {
        originFeeMixin.connectWith(
            inputSource1 = amountChooserMixin.backPressuredAmount,
            inputSource2 = selectedConvictionFlow,
            inputSource3 = chainFlow,
            feeConstructor = { feePaymentCurrency, amount, conviction, chain ->
                interactor.estimateFee(
                    amount = feePaymentCurrency.toFeePaymentAsset(chain).planksFromAmount(amount),
                    conviction = conviction,
                    delegate = payload.delegate,
                    tracks = payload.trackIds,
                    shouldRemoveOtherTracks = payload.isEditMode
                )
            }
        )
    }

    fun continueClicked() {
        openConfirmIfValid()
    }

    fun backClicked() {
        router.back()
    }

    fun amountChipClicked(chipModel: AmountChipModel) {
        amountChooserMixin.setAmountInput(chipModel.amountInput)
    }

    private fun openConfirmIfValid() = launch {
        validationInProgressFlow.value = true

        val payload = ChooseDelegationAmountValidationPayload(
            asset = selectedAsset.first(),
            fee = originFeeMixin.awaitFee(),
            amount = amountChooserMixin.amount.first(),
            delegate = payload.delegate
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { chooseChooseDelegationAmountValidationFailure(it, resourceManager) },
            progressConsumer = validationInProgressFlow.progressConsumer(),
        ) {
            validationInProgressFlow.value = false

            openConfirm(it)
        }
    }

    private fun openConfirm(validationPayload: ChooseDelegationAmountValidationPayload) = launch {
        val payload = NewDelegationConfirmPayload(
            delegate = validationPayload.delegate,
            trackIdsRaw = payload.trackIdsRaw,
            amount = validationPayload.amount,
            conviction = selectedConvictionFlow.first(),
            fee = mapFeeToParcel(validationPayload.fee),
            isEditMode = payload.isEditMode
        )

        router.openNewDelegationConfirm(payload)
    }
}
