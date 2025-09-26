package io.novafoundation.nova.feature_governance_impl.presentation.unlock.confirm

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.utils.multiResult.PartialRetriableMixin
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch.submissionHierarchy
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper

import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.Change
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.absoluteDifference
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.GovernanceUnlockInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.validations.UnlockReferendumValidationPayload
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.validations.UnlockReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.validations.handleUnlockReferendumValidationFailure
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.unlock.confirm.hints.ConfirmGovernanceUnlockHintsMixinFactory
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConfirmGovernanceUnlockViewModel(
    private val router: GovernanceRouter,
    private val externalActions: ExternalActions.Presentation,
    private val governanceSharedState: GovernanceSharedState,
    private val validationExecutor: ValidationExecutor,
    private val interactor: GovernanceUnlockInteractor,
    feeMixinFactory: FeeLoaderMixin.Factory,
    private val assetUseCase: AssetUseCase,
    private val walletUiUseCase: WalletUiUseCase,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val locksChangeFormatter: LocksChangeFormatter,
    private val validationSystem: UnlockReferendumValidationSystem,
    private val hintsMixinFactory: ConfirmGovernanceUnlockHintsMixinFactory,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
    partialRetriableMixinFactory: PartialRetriableMixin.Factory,
) : BaseViewModel(),
    WithFeeLoaderMixin,
    Validatable by validationExecutor,
    ExternalActions by externalActions,
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    private val unlockAffectsFlow = interactor.unlockAffectsFlow(
        scope = viewModelScope,
        assetFlow = assetFlow
    ).shareInBackground()

    override val originFeeMixin = feeMixinFactory.create(assetFlow)

    val hintsMixin = hintsMixinFactory.create(
        scope = viewModelScope,
        assetFlow = assetFlow,
        remainsLockedInfoFlow = unlockAffectsFlow.map { it.remainsLockedInfo }
    )

    val walletModel: Flow<WalletModel> = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val amountModelFlow = unlockAffectsFlow.map {
        val asset = assetFlow.first()
        val amount = it.governanceLockChange.absoluteDifference()

        mapAmountToAmountModel(amount, asset)
    }.shareInBackground()

    val currentAddressModelFlow = selectedAccountUseCase.selectedMetaAccountFlow().map { metaAccount ->
        val chain = governanceSharedState.chain()

        addressIconGenerator.createAccountAddressModel(chain, metaAccount)
    }.shareInBackground()

    val transferableChange = unlockAffectsFlow.map {
        val asset = assetFlow.first()

        locksChangeFormatter.mapAmountChangeToUi(it.transferableChange, asset)
    }.shareInBackground()

    val governanceLockChange = unlockAffectsFlow.map {
        val asset = assetFlow.first()

        locksChangeFormatter.mapAmountChangeToUi(it.governanceLockChange, asset)
    }.shareInBackground()

    private val submissionInProgress = MutableStateFlow(false)

    val confirmButtonState = submissionInProgress.map { inProgress ->
        if (inProgress) {
            DescriptiveButtonState.Loading
        } else {
            DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_confirm))
        }
    }

    val partialRetriableMixin = partialRetriableMixinFactory.create(scope = this)

    init {
        originFeeMixin.connectWith(
            inputSource = unlockAffectsFlow,
            scope = this,
            feeConstructor = { interactor.calculateFee(it.claimableChunk) }
        )
    }

    fun accountClicked() = launch {
        val chain = governanceSharedState.chain()
        val addressModel = currentAddressModelFlow.first()

        externalActions.showAddressActions(addressModel.address, chain)
    }

    fun backClicked() {
        router.back()
    }

    fun confirmClicked() = launch {
        submissionInProgress.value = true

        val claimable = unlockAffectsFlow.first().claimableChunk
        val locksChange = unlockAffectsFlow.first().governanceLockChange

        val validationPayload = UnlockReferendumValidationPayload(
            asset = assetFlow.first(),
            fee = originFeeMixin.awaitFee()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = validationPayload,
            validationFailureTransformer = { handleUnlockReferendumValidationFailure(it, resourceManager) },
            progressConsumer = submissionInProgress.progressConsumer(),
        ) {
            executeUnlock(claimable, locksChange)
        }
    }

    private fun executeUnlock(
        claimable: ClaimSchedule.UnlockChunk.Claimable?,
        lockChange: Change<Balance>
    ) = launch {
        val result = interactor.unlock(claimable)

        partialRetriableMixin.handleMultiResult(
            multiResult = result,
            onSuccess = {
                startNavigation(it.submissionHierarchy()) {
                    router.finishUnlockFlow(shouldCloseLocksScreen = lockChange.newValue.isZero)
                }
            },
            progressConsumer = submissionInProgress.progressConsumer(),
            onRetryCancelled = { router.back() }
        )
    }
}
