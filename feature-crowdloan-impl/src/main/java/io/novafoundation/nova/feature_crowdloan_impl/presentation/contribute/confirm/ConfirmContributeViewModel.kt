package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.validation.CompositeValidation
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeManager
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.CrowdloanContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.ContributeValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.ContributeValidationPayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm.model.LeasePeriodModel
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm.parcel.ConfirmContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.contributeValidationFailure
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.ConfirmContributeCustomization
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.mapParachainMetadataFromParcel
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapFeeToFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeFromParcel
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConfirmContributeViewModel(
    private val assetIconProvider: AssetIconProvider,
    private val router: CrowdloanRouter,
    private val contributionInteractor: CrowdloanContributeInteractor,
    private val resourceManager: ResourceManager,
    assetUseCase: AssetUseCase,
    accountUseCase: SelectedAccountUseCase,
    addressModelGenerator: AddressIconGenerator,
    private val validationExecutor: ValidationExecutor,
    private val payload: ConfirmContributePayload,
    private val validations: Collection<ContributeValidation>,
    private val customContributeManager: CustomContributeManager,
    private val externalActions: ExternalActions.Presentation,
    private val assetSharedState: SingleAssetSharedState,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
) : BaseViewModel(),
    Validatable by validationExecutor,
    ExternalActions by externalActions,
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

    private val decimalFee = mapFeeFromParcel(payload.fee)

    private val chain by lazyAsync { assetSharedState.chain() }

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    private val assetFlow = assetUseCase.currentAssetFlow()
        .share()

    val assetModelFlow = assetFlow
        .map { mapAssetToAssetModel(assetIconProvider, it, resourceManager, it.transferableInPlanks) }
        .inBackground()
        .share()

    val selectedAddressModelFlow = accountUseCase.selectedMetaAccountFlow()
        .map { metaAccount ->
            addressModelGenerator.createAccountAddressModel(chain.await(), metaAccount)
        }
        .shareInBackground()

    val selectedAmount = payload.amount.toString()

    val feeFlow = assetFlow.map { asset ->
        val feeModel = mapFeeToFeeModel(decimalFee, asset.token)

        FeeStatus.Loaded(feeModel)
    }
        .inBackground()
        .share()

    val enteredFiatAmountFlow = assetFlow.map { asset ->
        asset.token.amountToFiat(payload.amount).formatAsCurrency(asset.token.currency)
    }
        .inBackground()
        .share()

    private val parachainMetadata = payload.metadata?.let(::mapParachainMetadataFromParcel)

    private val relevantCustomFlowFactory = parachainMetadata?.customFlow?.let {
        customContributeManager.getFactoryOrNull(it)
    }

    val customizationConfiguration: Flow<Pair<ConfirmContributeCustomization, ConfirmContributeCustomization.ViewState>?> = flowOf {
        relevantCustomFlowFactory?.confirmContributeCustomization?.let {
            it to it.createViewState(coroutineScope = this, parachainMetadata!!, payload.customizationPayload)
        }
    }
        .inBackground()
        .share()

    val estimatedReward = payload.estimatedRewardDisplay

    private val crowdloanFlow = contributionInteractor.crowdloanStateFlow(payload.paraId, parachainMetadata)
        .inBackground()
        .share()

    val crowdloanInfoFlow = crowdloanFlow.map { crowdloan ->
        LeasePeriodModel(
            leasePeriod = resourceManager.formatDuration(crowdloan.leasePeriodInMillis),
            leasedUntil = resourceManager.formatDateTime(crowdloan.leasedUntilInMillis)
        )
    }
        .inBackground()
        .share()

    val bonusFlow = flow {
        val bonusDisplay = payload.bonusPayload?.bonusText(payload.amount)

        emit(bonusDisplay)
    }
        .inBackground()
        .share()

    private val customizedValidationSystem = flowOf {
        val validations = relevantCustomFlowFactory?.confirmContributeCustomization?.modifyValidations(validations)
            ?: validations

        ValidationSystem(CompositeValidation(validations))
    }
        .inBackground()
        .share()

    fun nextClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() {
        launch {
            val accountAddress = selectedAddressModelFlow.first().address
            val chain = assetSharedState.chain()

            externalActions.showAddressActions(accountAddress, chain)
        }
    }

    private fun maybeGoToNext() = launch {
        val validationPayload = ContributeValidationPayload(
            crowdloan = crowdloanFlow.first(),
            fee = decimalFee,
            asset = assetFlow.first(),
            customizationPayload = payload.customizationPayload,
            bonusPayload = payload.bonusPayload,
            contributionAmount = payload.amount
        )

        validationExecutor.requireValid(
            validationSystem = customizedValidationSystem.first(),
            payload = validationPayload,
            progressConsumer = _showNextProgress.progressConsumer(),
            validationFailureTransformerCustom = { status, actions ->
                contributeValidationFailure(
                    reason = status.reason,
                    validationFlowActions = actions,
                    resourceManager = resourceManager,
                    onOpenCustomContribute = null
                )
            }
        ) {
            sendTransaction()
        }
    }

    private fun sendTransaction() {
        launch {
            val crowdloan = crowdloanFlow.first()

            contributionInteractor.contribute(
                crowdloan = crowdloan,
                contribution = payload.amount,
                bonusPayload = payload.bonusPayload,
                customizationPayload = payload.customizationPayload
            )
                .onFailure(::showError)
                .onSuccess {
                    showToast(resourceManager.getString(R.string.common_transaction_submitted))

                    startNavigation(it.submissionHierarchy) { router.returnToMain() }
                }

            _showNextProgress.value = false
        }
    }
}
