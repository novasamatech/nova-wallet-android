package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.confirm

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumVote
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_api.domain.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.VoteReferendumValidationPayload
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.VoteReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.confirmVote.ConfirmVoteViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.hints.ReferendumVoteHintsMixinFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeFromParcel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.DecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Vote
import io.novafoundation.nova.runtime.state.chainAndAsset
import java.math.BigInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private val referendumFormatter: ReferendumFormatter,
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
        .shareInBackground()

    override val titleFlow: Flow<String> = basketFlow.map {
        resourceManager.getString(R.string.tinder_gov_vote_setup_title, it.size)
    }.shareInBackground()

    override val amountModelFlow: Flow<AmountModel> = combine(assetFlow, basketFlow) { asset, basket ->
        val maxAmount = basket.maxOf { it.amount }
        mapAmountToAmountModel(maxAmount, asset)
    }.shareInBackground()

    private val accountVoteFlow = assetFlow.map(::constructAccountVote)
        .shareInBackground()

    override val accountVoteUi = accountVoteFlow.map {
        val referendumVote = ReferendumVote.UserDirect(it)
        val (chain, chainAsset) = governanceSharedState.chainAndAsset()

        referendumFormatter.formatUserVote(referendumVote, chain, chainAsset)
    }.shareInBackground()

    override val decimalFeeFlow: Flow<DecimalFee>
        get() = TODO("Not yet implemented")

    private val voteAssistantFlow = interactor.voteAssistantFlow(payload.referendumId, viewModelScope)

    private val locksChangeFlow = voteAssistantFlow.map { voteAssistant ->
        val asset = assetFlow.first()
        val amountPlanks = asset.token.planksFromAmount(payload.vote.amount)

        voteAssistant.estimateLocksAfterVoting(amountPlanks, payload.vote.conviction, asset)
    }

    override val locksChangeUiFlow = locksChangeFlow.map {
        locksChangeFormatter.mapLocksChangeToUi(it, assetFlow.first())
    }
        .shareInBackground()

    override suspend fun performVote() {
        val accountVote = accountVoteFlow.first()

        val result = withContext(Dispatchers.Default) {
            interactor.vote(accountVote, payload.referendumId)
        }

        result.onSuccess {
            showMessage(resourceManager.getString(R.string.common_transaction_submitted))
            router.backToReferendumDetails()
        }
            .onFailure(::showError)
    }

    override suspend fun getValidationPayload(): VoteReferendumValidationPayload {
        val voteAssistant = voteAssistantFlow.first()

        return VoteReferendumValidationPayload(
            onChainReferendum = voteAssistant.onChainReferendum,
            asset = assetFlow.first(),
            trackVoting = voteAssistant.trackVoting,
            voteAmount = payload.vote.amount,
            fee = decimalFeeFlow.first(),
            conviction = payload.vote.conviction,
            voteType = payload.vote.voteType
        )
    }

    private fun constructAccountVote(asset: Asset): AccountVote {
        val planks = asset.token.planksFromAmount(payload.vote.amount)

        return if (payload.vote.voteType == VoteType.ABSTAIN) {
            AccountVote.SplitAbstain(
                aye = BigInteger.ZERO,
                nay = BigInteger.ZERO,
                abstain = planks
            )
        } else {
            AccountVote.Standard(
                vote = Vote(
                    aye = payload.vote.voteType == VoteType.AYE,
                    conviction = payload.vote.conviction
                ),
                balance = planks
            )
        }
    }
}
