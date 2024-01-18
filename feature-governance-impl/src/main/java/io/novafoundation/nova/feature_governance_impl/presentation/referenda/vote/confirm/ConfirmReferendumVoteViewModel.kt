package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumVote
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.VoteReferendumValidationPayload
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.VoteReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.handleVoteReferendumValidationFailure
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.hints.ReferendumVoteHintsMixinFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeFromParcel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Vote
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAndAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val localIdentityProvider: IdentityProvider,
    private val interactor: VoteReferendumInteractor,
    private val assetUseCase: AssetUseCase,
    private val payload: ConfirmVoteReferendumPayload,
    private val validationSystem: VoteReferendumValidationSystem,
    private val validationExecutor: ValidationExecutor,
    private val resourceManager: ResourceManager,
    private val referendumFormatter: ReferendumFormatter,
    private val locksChangeFormatter: LocksChangeFormatter,
) : BaseViewModel(),
    Validatable by validationExecutor,
    WithFeeLoaderMixin,
    ExternalActions by externalActions {

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    override val originFeeMixin: FeeLoaderMixin.Presentation = feeLoaderMixinFactory.create(assetFlow)

    val hintsMixin = hintsMixinFactory.create(scope = this)

    val walletModel: Flow<WalletModel> = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val amountModelFlow = assetFlow.map {
        mapAmountToAmountModel(payload.vote.amount, it)
    }.shareInBackground()

    val currentAddressModelFlow = selectedAccountUseCase.selectedMetaAccountFlow().map { metaAccount ->
        val chain = governanceSharedState.chain()

        addressIconGenerator.createAccountAddressModel(chain, metaAccount)
    }.shareInBackground()

    private val accountVoteFlow = assetFlow.map(::constructAccountVote)
        .shareInBackground()

    private val _showNextProgress = MutableStateFlow(false)

    val showNextProgress: Flow<Boolean> = _showNextProgress

    private val voteAssistantFlow = interactor.voteAssistantFlow(payload.referendumId, viewModelScope)

    val accountVoteUi = accountVoteFlow.map {
        val referendumVote = ReferendumVote.UserDirect(it)
        val (chain, chainAsset) = governanceSharedState.chainAndAsset()

        referendumFormatter.formatUserVote(referendumVote, chain, chainAsset)
    }.shareInBackground()

    val title = flowOf {
        val formattedNumber = referendumFormatter.formatId(payload.referendumId)
        resourceManager.getString(R.string.referendum_vote_setup_title, formattedNumber)
    }.shareInBackground()

    private val locksChangeFlow = voteAssistantFlow.map { voteAssistant ->
        val asset = assetFlow.first()
        val amountPlanks = asset.token.planksFromAmount(payload.vote.amount)

        voteAssistant.estimateLocksAfterVoting(amountPlanks, payload.vote.conviction, asset)
    }

    val locksChangeUiFlow = locksChangeFlow.map {
        locksChangeFormatter.mapLocksChangeToUi(it, assetFlow.first())
    }
        .shareInBackground()

    private val decimalFee = mapFeeFromParcel(payload.fee)

    init {
        setFee()
    }

    fun accountClicked() = launch {
        val addressModel = currentAddressModelFlow.first()
        val type = ExternalActions.Type.Address(addressModel.address)

        externalActions.showExternalActions(type, governanceSharedState.chain())
    }

    fun confirmClicked() = launch {
        val voteAssistant = voteAssistantFlow.first()

        val validationPayload = VoteReferendumValidationPayload(
            onChainReferendum = voteAssistant.onChainReferendum,
            asset = assetFlow.first(),
            trackVoting = voteAssistant.trackVoting,
            voteAmount = payload.vote.amount,
            fee = decimalFee
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = validationPayload,
            validationFailureTransformer = { handleVoteReferendumValidationFailure(it, resourceManager) },
            progressConsumer = _showNextProgress.progressConsumer(),
        ) {
            performVote()
        }
    }

    fun backClicked() {
        router.back()
    }

    private fun setFee() = launch {
        originFeeMixin.setFee(decimalFee.genericFee)
    }

    private fun performVote() = launch {
        val accountVote = accountVoteFlow.first()

        val result = withContext(Dispatchers.Default) {
            interactor.vote(accountVote, payload.referendumId)
        }

        result.onSuccess {
            showMessage(resourceManager.getString(R.string.common_transaction_submitted))
            router.backToReferendumDetails()
        }
            .onFailure(::showError)

        _showNextProgress.value = false
    }

    private fun constructAccountVote(asset: Asset): AccountVote.Standard {
        val planks = asset.token.planksFromAmount(payload.vote.amount)

        return AccountVote.Standard(
            vote = Vote(
                aye = payload.vote.aye,
                conviction = payload.vote.conviction
            ),
            balance = planks
        )
    }
}
