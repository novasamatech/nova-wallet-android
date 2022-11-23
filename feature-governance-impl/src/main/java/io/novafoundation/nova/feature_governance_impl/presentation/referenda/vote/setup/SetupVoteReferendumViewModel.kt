package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.formatting.toAmountInput
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.ProgressConsumer
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.view.input.seekbar.SeekbarValue
import io.novafoundation.nova.common.view.input.seekbar.SeekbarValues
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.amountMultiplier
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.votesFor
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.GovernanceVoteAssistant
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.GovernanceVoteAssistant.ReusableLock.Type.ALL
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.GovernanceVoteAssistant.ReusableLock.Type.GOVERNANCE
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.VoteReferendumValidationPayload
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.VoteReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.handleVoteReferendumValidationFailure
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.AccountVoteParcelModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.ConfirmVoteReferendumPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.model.AmountChipModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.requireFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class SetupVoteReferendumViewModel(
    private val feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    private val assetUseCase: AssetUseCase,
    private val amountChooserMixinFactory: AmountChooserMixin.Factory,
    private val interactor: VoteReferendumInteractor,
    private val payload: SetupVoteReferendumPayload,
    private val resourceManager: ResourceManager,
    private val router: GovernanceRouter,
    private val validationSystem: VoteReferendumValidationSystem,
    private val validationExecutor: ValidationExecutor,
    private val referendumFormatter: ReferendumFormatter,
    private val locksChangeFormatter: LocksChangeFormatter,
) : BaseViewModel(),
    WithFeeLoaderMixin,
    Validatable by validationExecutor {

    private val selectedAsset = assetUseCase.currentAssetFlow()
        .shareInBackground()

    private val voteAssistantFlow = interactor.voteAssistantFlow(payload.referendumId, viewModelScope)

    override val originFeeMixin = feeLoaderMixinFactory.create(selectedAsset)

    private val validatingVoteType = MutableStateFlow<VoteType?>(null)

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = selectedAsset,
        balanceField = Asset::free,
        balanceLabel = R.string.wallet_balance_available
    )

    val convictionValues = convictionValues()

    val selectedConvictionIndex = MutableStateFlow(0)

    val title = flowOf {
        val formattedNumber = referendumFormatter.formatId(payload.referendumId)
        resourceManager.getString(R.string.referendum_vote_setup_title, formattedNumber)
    }.shareInBackground()

    private val selectedConvictionFlow = selectedConvictionIndex.mapNotNull(convictionValues::valueAt)

    private val locksChangeFlow = combine(
        amountChooserMixin.amount,
        selectedConvictionFlow,
        voteAssistantFlow
    ) { amount, conviction, voteAssistant ->
        val amountPlanks = selectedAsset.first().token.planksFromAmount(amount)

        voteAssistant.estimateLocksAfterVoting(amountPlanks, conviction, selectedAsset.first())
    }
        .inBackground()
        .shareWhileSubscribed()

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

    val ayeButtonStateFlow = buttonStateFlow(VoteType.AYE, R.string.referendum_vote_aye)
    val nayButtonStateFlow = buttonStateFlow(VoteType.NAY, R.string.referendum_vote_nay)

    val amountChips = voteAssistantFlow.map { voteAssistant ->
        val asset = selectedAsset.first()

        voteAssistant.reusableLocks().map { mapReusableLockToAmountChipModel(it, asset) }
    }
        .shareInBackground()

    init {
        originFeeMixin.connectWith(
            inputSource1 = amountChooserMixin.amount,
            inputSource2 = selectedConvictionFlow,
            scope = this,
            feeConstructor = { amount, conviction ->
                interactor.estimateFee(
                    amount = amount.toPlanks(),
                    conviction = conviction,
                    referendumId = payload.referendumId
                )
            }
        )
    }

    fun ayeClicked() {
        openConfirmIfValid(VoteType.AYE)
    }

    fun nayClicked() {
        openConfirmIfValid(VoteType.NAY)
    }

    fun backClicked() {
        router.back()
    }

    fun amountChipClicked(chipModel: AmountChipModel) {
        amountChooserMixin.amountInput.value = chipModel.amountInput
    }

    private fun openConfirmIfValid(voteType: VoteType) = originFeeMixin.requireFee(this) { fee ->
        launch {
            val voteAssistant = voteAssistantFlow.first()

            val payload = VoteReferendumValidationPayload(
                onChainReferendum = voteAssistant.onChainReferendum,
                asset = selectedAsset.first(),
                trackVoting = voteAssistant.trackVoting,
                voteAmount = amountChooserMixin.amount.first(),
                fee = fee
            )

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                validationFailureTransformer = { handleVoteReferendumValidationFailure(it, resourceManager) },
                progressConsumer = validatingVoteType.progressConsumer(voteType),
            ) {
                validatingVoteType.value = null

                openConfirm(it, voteType)
            }
        }
    }

    private fun openConfirm(validationPayload: VoteReferendumValidationPayload, voteType: VoteType) = launch {
        val conviction = selectedConvictionFlow.first()

        val confirmPayload = ConfirmVoteReferendumPayload(
            _referendumId = payload._referendumId,
            fee = validationPayload.fee,
            vote = AccountVoteParcelModel(
                amount = validationPayload.voteAmount,
                conviction = conviction,
                aye = voteType == VoteType.AYE
            )
        )

        router.openConfirmVoteReferendum(confirmPayload)
    }

    private fun convictionValues(): SeekbarValues<Conviction> {
        val colors = listOf(
            R.color.conviction_slider_text_01x,
            R.color.conviction_slider_text_1x,
            R.color.conviction_slider_text_2x,
            R.color.conviction_slider_text_3x,
            R.color.conviction_slider_text_4x,
            R.color.conviction_slider_text_5x,
            R.color.conviction_slider_text_6x
        )

        val values = Conviction.values().mapIndexed { index, conviction ->
            SeekbarValue(
                value = conviction,
                label = "${conviction.amountMultiplier().format()}x",
                labelColorRes = colors[index]
            )
        }

        return SeekbarValues(values)
    }

    private fun buttonStateFlow(forType: VoteType, @StringRes labelRes: Int) = validatingVoteType.map { validationType ->
        when (validationType) {
            null -> DescriptiveButtonState.Enabled(resourceManager.getString(labelRes))
            forType -> DescriptiveButtonState.Loading
            else -> DescriptiveButtonState.Disabled(resourceManager.getString(labelRes))
        }
    }

    private fun MutableStateFlow<VoteType?>.progressConsumer(voteType: VoteType): ProgressConsumer = { inProgress ->
        value = voteType.takeIf { inProgress }
    }

    private fun mapReusableLockToAmountChipModel(
        reusableLock: GovernanceVoteAssistant.ReusableLock,
        asset: Asset
    ): AmountChipModel {
        val labelFormat = when (reusableLock.type) {
            GOVERNANCE -> R.string.referendum_vote_chip_governance_lock
            ALL -> R.string.referendum_vote_chip_all_locks
        }

        val amount = asset.token.amountFromPlanks(reusableLock.amount)
        val amountModel = mapAmountToAmountModel(amount, asset)

        return AmountChipModel(
            amountInput = amount.toAmountInput(),
            label = resourceManager.getString(labelFormat, amountModel.token)
        )
    }
}
