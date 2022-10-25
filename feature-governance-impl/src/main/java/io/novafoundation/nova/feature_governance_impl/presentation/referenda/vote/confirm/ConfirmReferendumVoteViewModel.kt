package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createIdentityAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.VoteReferendumValidationPayload
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.VoteReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.handleVoteReferendumValidationFailure
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.model.ReviewReferendumModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.hints.ReferendumVoteHintsMixinFactory
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Vote
import io.novafoundation.nova.runtime.state.chain
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

    val currentAddressModelFlow = selectedAccountUseCase.selectedMetaAccountFlow().map {
        val chain = governanceSharedState.chain()

        addressIconGenerator.createIdentityAddressModel(
            chain = chain,
            accountId = it.accountIdIn(chain)!!,
            identityProvider = localIdentityProvider
        )
    }.shareInBackground()

    private val accountVoteFlow = assetFlow.map(::constructAccountVote)
        .shareInBackground()

    private val _showNextProgress = MutableStateFlow(false)

    val showNextProgress: Flow<Boolean> = _showNextProgress

    private val voteAssistantFlow = interactor.voteAssistantFlow(payload.referendumId, viewModelScope)

    val accountVoteUi = accountVoteFlow.map {
        referendumFormatter.formatUserVote(it, assetFlow.first().token)
    }.shareInBackground()

    val reviewReferendumModel = voteAssistantFlow.map { voteAssistant ->
        val asset = assetFlow.first()
            .token
            .configuration

        ReviewReferendumModel(
            number = referendumFormatter.formatId(payload.referendumId),
            trackModel = voteAssistant.track?.let { referendumFormatter.formatTrack(it, asset) },
        )
    }.shareInBackground()

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
            fee = payload.fee
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
        originFeeMixin.setFee(payload.fee)
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

    private fun constructAccountVote(asset: Asset): AccountVote {
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
