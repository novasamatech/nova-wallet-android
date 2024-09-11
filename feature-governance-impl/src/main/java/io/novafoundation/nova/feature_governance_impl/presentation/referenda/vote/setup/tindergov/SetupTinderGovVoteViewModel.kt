package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.model.VotingPower
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_api.domain.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.VoteReferendumValidationPayload
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.VoteReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.conviction.ConvictionValuesProvider
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.LocksFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.SetupVotePayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.SetupVoteViewModel
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.setAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SetupTinderGovVoteViewModel(
    private val payload: SetupVotePayload,
    private val router: GovernanceRouter,
    private val tinderGovInteractor: TinderGovInteractor,
    private val tinderGovVoteResponder: TinderGovVoteResponder,
    private val accountRepository: AccountRepository,
    feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    assetUseCase: AssetUseCase,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
    interactor: VoteReferendumInteractor,
    resourceManager: ResourceManager,
    validationSystem: VoteReferendumValidationSystem,
    validationExecutor: ValidationExecutor,
    locksChangeFormatter: LocksChangeFormatter,
    convictionValuesProvider: ConvictionValuesProvider,
    locksFormatter: LocksFormatter,
) : SetupVoteViewModel(
    feeLoaderMixinFactory,
    assetUseCase,
    amountChooserMixinFactory,
    interactor,
    payload,
    resourceManager,
    router,
    validationSystem,
    validationExecutor,
    locksChangeFormatter,
    convictionValuesProvider,
    locksFormatter
) {
    override val title: Flow<String> = flowOf {
        resourceManager.getString(R.string.tinder_gov_vote_title)
    }

    init {
        launch {
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val asset = selectedAsset.first()
            val chainAsset = asset.token.configuration
            val votingPower = tinderGovInteractor.getVotingPower(metaAccount.id, asset.token.configuration.chainId) ?: return@launch
            val amount = chainAsset.amountFromPlanks(votingPower.amount)
            amountChooserMixin.setAmount(amount, false)
            selectedConvictionIndex.value = votingPower.conviction.ordinal
        }
    }

    override fun backClicked() {
        tinderGovVoteResponder.respond(TinderGovVoteResponder.Response(success = false))
        super.backClicked()
    }

    fun continueClicked() {
        validateVote(voteType = null)
    }

    override fun onFinish(validationPayload: VoteReferendumValidationPayload) {
        launch {
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val chainAsset = validationPayload.asset.token.configuration
            val amount = chainAsset.planksFromAmount(validationPayload.voteAmount)
            val votingPower = VotingPower(metaAccount.id, chainAsset.chainId, amount, validationPayload.conviction)
            tinderGovInteractor.setVotingPower(votingPower)
            tinderGovVoteResponder.respond(TinderGovVoteResponder.Response(success = true))
            router.back()
        }
    }
}
