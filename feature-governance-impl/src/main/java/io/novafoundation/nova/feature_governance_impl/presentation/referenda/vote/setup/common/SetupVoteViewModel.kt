package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.ProgressConsumer
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.constructAccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.votesFor
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.estimateLocksAfterVoting
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.referendum.VoteReferendaValidationPayload
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.referendum.VoteReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.referendum.handleVoteReferendumValidationFailure
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.conviction.ConvictionValuesProvider
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.AmountChipModel
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.LocksFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setAmountInput
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxBalanceType
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.create
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.BigInteger

abstract class SetupVoteViewModel(
    private val assetUseCase: AssetUseCase,
    private val amountChooserMixinFactory: AmountChooserMixin.Factory,
    private val interactor: VoteReferendumInteractor,
    private val payload: SetupVotePayload,
    private val resourceManager: ResourceManager,
    private val router: GovernanceRouter,
    private val validationSystem: VoteReferendumValidationSystem,
    private val validationExecutor: ValidationExecutor,
    private val locksChangeFormatter: LocksChangeFormatter,
    private val convictionValuesProvider: ConvictionValuesProvider,
    private val locksFormatter: LocksFormatter,
    private val maxActionProviderFactory: MaxActionProviderFactory,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
) : BaseViewModel(),
    Validatable by validationExecutor {

    abstract val title: Flow<String>

    private val assetWithOption = assetUseCase.currentAssetAndOptionFlow()
        .shareInBackground()

    protected val selectedAsset = assetWithOption.map { it.asset }
        .shareInBackground()

    private val selectedChainAsset = selectedAsset.map { it.token.configuration }
        .shareInBackground()

    private val voteAssistantFlow = interactor.voteAssistantFlow(payload.referendumId, viewModelScope)

    private val originFeeMixin = feeLoaderMixinFactory.createDefault(this, selectedChainAsset)

    protected val validatingVoteType = MutableStateFlow<VoteType?>(null)

    private val maxActionProvider = maxActionProviderFactory.create(
        viewModelScope = viewModelScope,
        assetInFlow = selectedAsset,
        feeLoaderMixin = originFeeMixin,
        maxBalanceType = MaxBalanceType.FREE
    )

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = selectedAsset,
        maxActionProvider = maxActionProvider
    )

    val convictionValues = convictionValuesProvider.convictionValues()

    val selectedConvictionIndex = MutableStateFlow(0)

    private val selectedConvictionFlow = selectedConvictionIndex.mapNotNull(convictionValues::valueAt)

    private val locksChangeFlow = combine(
        amountChooserMixin.amount,
        selectedConvictionFlow,
        voteAssistantFlow
    ) { amount, conviction, voteAssistant ->
        val amountPlanks = selectedAsset.first().token.planksFromAmount(amount)
        val accountVote = constructAccountVote(amountPlanks, conviction)
        voteAssistant.estimateLocksAfterVoting(payload.referendumId, accountVote, selectedAsset.first())
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

    val abstainVotingSupported = flowOf { interactor.isAbstainSupported() }
        .shareInBackground()

    val amountChips = voteAssistantFlow.map { voteAssistant ->
        val asset = selectedAsset.first()

        voteAssistant.reusableLocks().map { locksFormatter.formatReusableLock(it, asset) }
    }
        .shareInBackground()

    init {
        originFeeMixin.connectWith(
            inputSource1 = amountChooserMixin.backPressuredPlanks,
            inputSource2 = selectedConvictionFlow,
            feeConstructor = { _, amount, conviction ->
                val accountVote = constructAccountVote(amount, conviction)
                interactor.estimateFee(
                    referendumId = payload.referendumId,
                    vote = accountVote
                )
            }
        )
    }

    open fun backClicked() {
        router.back()
    }

    fun amountChipClicked(chipModel: AmountChipModel) {
        amountChooserMixin.setAmountInput(chipModel.amountInput)
    }

    protected fun validateVote(voteType: VoteType) = launch {
        validatingVoteType.value = voteType
        val voteAssistant = voteAssistantFlow.first()
        val asset = selectedAsset.first()
        val amount = amountChooserMixin.amount.first()
        val conviction = selectedConvictionFlow.first()

        val payload = VoteReferendaValidationPayload(
            onChainReferenda = voteAssistant.onChainReferenda,
            asset = asset,
            trackVoting = voteAssistant.trackVoting,
            maxAmount = amount,
            voteType = voteType,
            conviction = conviction,
            fee = originFeeMixin.awaitFee()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformerCustom = { status, action ->
                handleVoteReferendumValidationFailure(status.reason, action, resourceManager)
            },
            progressConsumer = validatingVoteType.progressConsumer(voteType),
        ) {
            validatingVoteType.value = null

            onFinish(amount, conviction, voteType, it)
        }
    }

    abstract fun onFinish(amount: BigDecimal, conviction: Conviction, voteType: VoteType, validationPayload: VoteReferendaValidationPayload)

    private fun MutableStateFlow<VoteType?>.progressConsumer(voteType: VoteType?): ProgressConsumer = { inProgress ->
        value = voteType.takeIf { inProgress }
    }

    private fun constructAccountVote(
        amount: BigInteger,
        conviction: Conviction
    ): AccountVote {
        return AccountVote.constructAccountVote(amount, conviction, VoteType.AYE)
    }
}
