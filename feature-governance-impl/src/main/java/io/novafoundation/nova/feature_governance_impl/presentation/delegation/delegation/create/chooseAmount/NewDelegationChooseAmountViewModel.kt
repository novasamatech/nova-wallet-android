package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseAmount

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.votesFor
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseAmount.NewDelegationChooseAmountInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.conviction.ConvictionValuesProvider
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.AmountChipModel
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.LocksFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull


class NewDelegationChooseAmountViewModel(
    private val feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    private val assetUseCase: AssetUseCase,
    private val amountChooserMixinFactory: AmountChooserMixin.Factory,
    private val interactor: NewDelegationChooseAmountInteractor,
    private val payload: NewDelegationChooseAmountPayload,
    private val resourceManager: ResourceManager,
    private val router: GovernanceRouter,
//    private val validationSystem: VoteReferendumValidationSystem,
    private val validationExecutor: ValidationExecutor,
    private val locksChangeFormatter: LocksChangeFormatter,
    private val convictionValuesProvider: ConvictionValuesProvider,
    private val locksFormatter: LocksFormatter,
) : BaseViewModel(),
    WithFeeLoaderMixin,
    Validatable by validationExecutor {

    private val selectedAsset = assetUseCase.currentAssetFlow()
        .shareInBackground()

    private val delegateAssistantFlow = interactor.delegateAssistantFlow(viewModelScope)

    override val originFeeMixin = feeLoaderMixinFactory.create(selectedAsset)

    private val validatingVoteType = MutableStateFlow<VoteType?>(null)

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = selectedAsset,
        balanceField = Asset::free,
        balanceLabel = R.string.wallet_balance_available
    )

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
        .inBackground()
        .shareInBackground()

    val votesFormattedFlow = combine(
        amountChooserMixin.amount,
        selectedConvictionFlow
    ) { amount, conviction ->
        val votes = conviction.votesFor(amount)

        resourceManager.getString(R.string.referendum_votes_format, votes.format())
    }.shareInBackground()

    val locksChangeUiFlow = locksChangeFlow.map {
        locksChangeFormatter.mapLocksChangeToUi(it, selectedAsset.first())
    }
        .shareInBackground()

    val amountChips = delegateAssistantFlow.map { voteAssistant ->
        val asset = selectedAsset.first()

        voteAssistant.reusableLocks().map { locksFormatter.formatReusableLock(it, asset) }
    }
        .shareInBackground()

    init {
        originFeeMixin.connectWith(
            inputSource1 = amountChooserMixin.backPressuredAmount,
            inputSource2 = selectedConvictionFlow,
            scope = this,
            feeConstructor = { amount, conviction ->
                interactor.estimateFee(
                    amount = amount.toPlanks(),
                    conviction = conviction,
                    delegate = payload.delegate,
                    tracks = payload.trackIds
                )
            }
        )
    }

   fun continueClicked() {

   }

    fun backClicked() {
        router.back()
    }

    fun amountChipClicked(chipModel: AmountChipModel) {
        amountChooserMixin.amountInput.value = chipModel.amountInput
    }

//    private fun openConfirmIfValid(voteType: VoteType) = originFeeMixin.requireFee(this) { fee ->
//        launch {
//            val voteAssistant = delegateAssistantFlow.first()
//
//            val payload = VoteReferendumValidationPayload(
//                onChainReferendum = voteAssistant.onChainReferendum,
//                asset = selectedAsset.first(),
//                trackVoting = voteAssistant.trackVoting,
//                voteAmount = amountChooserMixin.amount.first(),
//                fee = fee
//            )
//
//            validationExecutor.requireValid(
//                validationSystem = validationSystem,
//                payload = payload,
//                validationFailureTransformer = { handleVoteReferendumValidationFailure(it, resourceManager) },
//                progressConsumer = validatingVoteType.progressConsumer(voteType),
//            ) {
//                validatingVoteType.value = null
//
//                openConfirm(it, voteType)
//            }
//        }
//    }
//
//    private fun openConfirm(validationPayload: VoteReferendumValidationPayload, voteType: VoteType) = launch {
//        val conviction = selectedConvictionFlow.first()
//
//        val confirmPayload = ConfirmVoteReferendumPayload(
//            _referendumId = payload._referendumId,
//            fee = validationPayload.fee,
//            vote = AccountVoteParcelModel(
//                amount = validationPayload.voteAmount,
//                conviction = conviction,
//                aye = voteType == VoteType.AYE
//            )
//        )
//
//        router.openConfirmVoteReferendum(confirmPayload)
//    }

//    private fun buttonStateFlow(forType: VoteType, @StringRes labelRes: Int) = validatingVoteType.map { validationType ->
//        when (validationType) {
//            null -> DescriptiveButtonState.Enabled(resourceManager.getString(labelRes))
//            forType -> DescriptiveButtonState.Loading
//            else -> DescriptiveButtonState.Disabled(resourceManager.getString(labelRes))
//        }
//    }
}
