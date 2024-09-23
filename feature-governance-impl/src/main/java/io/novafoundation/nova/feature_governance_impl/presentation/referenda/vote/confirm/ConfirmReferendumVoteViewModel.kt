package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.constructAccountVote
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumVote
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.estimateLocksAfterVoting
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.referendum.VoteReferendaValidationPayload
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.referendum.VoteReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.referendum.handleVoteReferendumValidationFailure
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.confirmVote.ConfirmVoteViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.hints.ReferendumVoteHintsMixinFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeFromParcel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.chainAndAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConfirmReferendumVoteViewModel(
    private val router: GovernanceRouter,
    private val feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    private val externalActions: ExternalActions.Presentation,
    private val governanceSharedState: GovernanceSharedState,
    private val hintsMixinFactory: ReferendumVoteHintsMixinFactory,
    private val walletUiUseCase: WalletUiUseCase,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val addressIconGenerator: AddressIconGenerator,
    private val interactor: VoteReferendumInteractor,
    private val assetUseCase: AssetUseCase,
    private val payload: ConfirmVoteReferendumPayload,
    private val validationSystem: VoteReferendumValidationSystem,
    private val validationExecutor: ValidationExecutor,
    private val resourceManager: ResourceManager,
    private val referendumFormatter: ReferendumFormatter,
    private val locksChangeFormatter: LocksChangeFormatter,
) : ConfirmVoteViewModel(
    router,
    feeLoaderMixinFactory,
    externalActions,
    governanceSharedState,
    hintsMixinFactory,
    walletUiUseCase,
    selectedAccountUseCase,
    addressIconGenerator,
    assetUseCase,
    validationExecutor
) {

    override val titleFlow: Flow<String> = flowOf {
        val formattedNumber = referendumFormatter.formatId(payload.referendumId)
        resourceManager.getString(R.string.referendum_vote_setup_title, formattedNumber)
    }.shareInBackground()

    override val amountModelFlow: Flow<AmountModel> = assetFlow.map {
        mapAmountToAmountModel(payload.vote.amount, it)
    }.shareInBackground()

    private val accountVoteFlow = assetFlow.map(::constructAccountVote)
        .shareInBackground()

    private val voteAssistantFlow = interactor.voteAssistantFlow(payload.referendumId, viewModelScope)

    override val accountVoteUi = accountVoteFlow.map {
        val referendumVote = ReferendumVote.UserDirect(it)
        val (chain, chainAsset) = governanceSharedState.chainAndAsset()

        referendumFormatter.formatUserVote(referendumVote, chain, chainAsset)
    }.shareInBackground()

    private val locksChangeFlow = voteAssistantFlow.map { voteAssistant ->
        val asset = assetFlow.first()
        val accountVote = constructAccountVote(asset)

        voteAssistant.estimateLocksAfterVoting(payload.referendumId, accountVote, asset)
    }

    init {
        setFee()
    }

    override val locksChangeUiFlow = locksChangeFlow.map {
        locksChangeFormatter.mapLocksChangeToUi(it, assetFlow.first())
    }
        .shareInBackground()

    override fun confirmClicked() {
        launch {
            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = getValidationPayload(),
                validationFailureTransformerCustom = { status, actions ->
                    handleVoteReferendumValidationFailure(status.reason, actions, resourceManager)
                },
                progressConsumer = _showNextProgress.progressConsumer(),
            ) {
                performVote()
            }
        }
    }

    private fun performVote() = launch {
        val accountVote = accountVoteFlow.first()

        val result = withContext(Dispatchers.Default) {
            interactor.voteReferendum(payload.referendumId, accountVote)
        }

        result.onSuccess {
            showMessage(resourceManager.getString(R.string.common_transaction_submitted))
            router.backToReferendumDetails()
        }
            .onFailure(::showError)

        _showNextProgress.value = false
    }

    private fun constructAccountVote(asset: Asset): AccountVote {
        val planks = asset.token.planksFromAmount(payload.vote.amount)

        return AccountVote.constructAccountVote(planks, payload.vote.conviction, payload.vote.voteType)
    }

    private fun setFee() = launch {
        originFeeMixin.setFee(mapFeeFromParcel(payload.fee).genericFee)
    }

    private suspend fun getValidationPayload(): VoteReferendaValidationPayload {
        val voteAssistant = voteAssistantFlow.first()

        return VoteReferendaValidationPayload(
            onChainReferenda = voteAssistant.onChainReferenda,
            asset = assetFlow.first(),
            trackVoting = voteAssistant.trackVoting,
            maxAmount = payload.vote.amount,
            conviction = payload.vote.conviction,
            voteType = payload.vote.voteType,
            fee = originFeeMixin.awaitDecimalFee()
        )
    }
}
