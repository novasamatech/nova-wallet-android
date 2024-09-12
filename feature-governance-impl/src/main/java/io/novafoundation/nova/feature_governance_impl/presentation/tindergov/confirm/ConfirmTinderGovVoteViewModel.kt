package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.confirm

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_api.data.model.accountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.amount
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_api.domain.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.VoteReferendaValidationPayload
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.VoteReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.confirmVote.ConfirmVoteViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.hints.ReferendumVoteHintsMixinFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.SimpleFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import java.math.BigInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConfirmTinderGovVoteViewModel(
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
    private val validationSystem: VoteReferendumValidationSystem,
    private val validationExecutor: ValidationExecutor,
    private val resourceManager: ResourceManager,
    private val locksChangeFormatter: LocksChangeFormatter,
    private val tinderGovInteractor: TinderGovInteractor
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
    validationSystem,
    validationExecutor,
    resourceManager
) {

    private val basketFlow = tinderGovInteractor.observeTinderGovBasket()
        .map { it.associateBy { it.referendumId } }
        .shareInBackground()

    private val votesFlow = basketFlow.map { basket ->
        basket.mapValues { it.value.accountVote() }
    }.shareInBackground()

    override val titleFlow: Flow<String> = basketFlow.map {
        resourceManager.getString(R.string.tinder_gov_vote_setup_title, it.size)
    }.shareInBackground()

    override val amountModelFlow: Flow<AmountModel> = combine(assetFlow, basketFlow) { asset, basket ->
        val maxAmount = basket.values.maxOfOrNull { it.amount } ?: BigInteger.ZERO
        mapAmountToAmountModel(maxAmount, asset)
    }.shareInBackground()

    override val accountVoteUi = flowOf { null }

    private val voteAssistantFlow = basketFlow.flatMapLatest { basketItems ->
        interactor.voteAssistantFlow(basketItems.keys.toList(), viewModelScope)
    }.shareInBackground()

    private val locksChangeFlow = combine(votesFlow, voteAssistantFlow) { accountVotes, voteAssistant ->
        val asset = assetFlow.first()

        voteAssistant.estimateLocksAfterVoting(accountVotes, asset)
    }

    override val locksChangeUiFlow = locksChangeFlow.map {
        locksChangeFormatter.mapLocksChangeToUi(it, assetFlow.first())
    }
        .shareInBackground()

    init {
        launch {
            originFeeMixin.loadFeeSuspending(
                retryScope = this,
                feeConstructor = {
                    val fee = interactor.estimateFee(votesFlow.first())
                    SimpleFee(fee)
                },
                onRetryCancelled = {
                    router.back()
                }
            )
        }
    }

    override suspend fun performVote() {
        val votes = votesFlow.first()

        val result = withContext(Dispatchers.Default) {
            interactor.vote(votes)
        }

        result.onSuccess {
            showMessage(resourceManager.getString(R.string.tinder_gov_convirm_votes_success_message, votes.size))
            tinderGovInteractor.clearBasket()
            router.backToTinderGovCards()
        }
            .onFailure(::showError)
    }

    override suspend fun getValidationPayload(): VoteReferendaValidationPayload {
        val voteAssistant = voteAssistantFlow.first()
        val asset = assetFlow.first()
        val votes = votesFlow.first().values
        val maxAmount = votes.maxOf { it.amount() }

        return VoteReferendaValidationPayload(
            onChainReferenda = voteAssistant.onChainReferenda,
            asset = assetFlow.first(),
            trackVoting = voteAssistant.trackVoting,
            fee = originFeeMixin.awaitDecimalFee(),
            maxAmount = asset.token.amountFromPlanks(maxAmount),
            conviction = null,
            voteType = null
        )
    }
}
