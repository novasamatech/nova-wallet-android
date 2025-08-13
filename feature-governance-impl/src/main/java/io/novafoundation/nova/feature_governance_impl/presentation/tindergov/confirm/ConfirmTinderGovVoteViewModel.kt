package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.confirm

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.multiResult.PartialRetriableMixin
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch.submissionHierarchy
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper

import io.novafoundation.nova.feature_governance_api.data.model.TinderGovBasketItem
import io.novafoundation.nova.feature_governance_api.data.model.accountVote
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.TinderGovBasketInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.tindergov.VoteTinderGovValidationPayload
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.tindergov.VoteTinderGovValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.tindergov.handleVoteTinderGovValidationFailure
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.confirmVote.ConfirmVoteViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.hints.ReferendumVoteHintsMixinFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger

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
    private val validationSystem: VoteTinderGovValidationSystem,
    private val validationExecutor: ValidationExecutor,
    private val resourceManager: ResourceManager,
    private val locksChangeFormatter: LocksChangeFormatter,
    private val tinderGovInteractor: TinderGovInteractor,
    private val tinderGovBasketInteractor: TinderGovBasketInteractor,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
    partialRetriableMixinFactory: PartialRetriableMixin.Factory,
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
),
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

    private val basketFlow = tinderGovBasketInteractor.observeTinderGovBasket()
        .map { it.associateBy { it.referendumId } }
        .shareInBackground()

    private val votesFlow = basketFlow.map { basket ->
        basket.mapValues { it.value.accountVote() }
    }.shareInBackground()

    override val titleFlow: Flow<String> = basketFlow.map {
        resourceManager.getString(R.string.swipe_gov_vote_setup_title, it.size)
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

    val partialRetriableMixin = partialRetriableMixinFactory.create(scope = this)

    init {
        launch {
            originFeeMixin.loadFeeSuspending(
                retryScope = this,
                feeConstructor = { interactor.estimateFee(votesFlow.first()) },
                onRetryCancelled = router::back
            )
        }
    }

    override fun confirmClicked() {
        launch {
            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = getValidationPayload(),
                validationFailureTransformerCustom = { status, actions ->
                    handleVoteTinderGovValidationFailure(status.reason, actions, resourceManager)
                },
                progressConsumer = _showNextProgress.progressConsumer(),
            ) {
                performVote(it)
            }
        }
    }

    private fun performVote(payload: VoteTinderGovValidationPayload) = launch {
        val accountVotes = payload.basket.associateBy { it.referendumId }
            .mapValues { (_, value) -> value.accountVote() }

        val result = withContext(Dispatchers.Default) {
            interactor.voteReferenda(accountVotes)
        }

        partialRetriableMixin.handleMultiResult(
            multiResult = result,
            onSuccess = {
                startNavigation(it.submissionHierarchy()) { onVoteSuccess(payload.basket) }
            },
            progressConsumer = _showNextProgress.progressConsumer(),
            onRetryCancelled = { router.back() }
        )
    }

    private suspend fun onVoteSuccess(basket: List<TinderGovBasketItem>) {
        awaitVotedReferendaStateUpdate(basket)

        showToast(resourceManager.getString(R.string.swipe_gov_convirm_votes_success_message, basket.size))
        tinderGovBasketInteractor.clearBasket()
        router.backToTinderGovCards()
    }

    private suspend fun awaitVotedReferendaStateUpdate(basket: List<TinderGovBasketItem>) {
        tinderGovInteractor.awaitAllItemsVoted(coroutineScope, basket)
    }

    private suspend fun getValidationPayload(): VoteTinderGovValidationPayload {
        val voteAssistant = voteAssistantFlow.first()
        val basket = basketFlow.first().values.toList()
        val asset = assetFlow.first()
        val maxAvailablePlanks = interactor.maxAvailableForVote(asset)
        val maxAvailableAmount = asset.token.amountFromPlanks(maxAvailablePlanks)

        return VoteTinderGovValidationPayload(
            onChainReferenda = voteAssistant.onChainReferenda,
            asset = asset,
            trackVoting = voteAssistant.trackVoting,
            fee = originFeeMixin.awaitFee(),
            basket = basket,
            maxAvailableAmount = maxAvailableAmount
        )
    }
}
