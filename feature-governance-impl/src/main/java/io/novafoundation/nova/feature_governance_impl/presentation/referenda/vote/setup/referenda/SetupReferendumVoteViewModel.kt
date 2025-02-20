package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.referenda

import androidx.annotation.StringRes
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.referendum.VoteReferendaValidationPayload
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.referendum.VoteReferendumValidationSystem
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.conviction.ConvictionValuesProvider
import io.novafoundation.nova.feature_governance_impl.presentation.common.locks.LocksFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.common.LocksChangeFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.AccountVoteParcelModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.ConfirmVoteReferendumPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.SetupVotePayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.SetupVoteViewModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common.referendumId
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeToParcel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.maxAction.MaxActionProviderFactory
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SetupReferendumVoteViewModel(
    private val payload: SetupVotePayload,
    private val router: GovernanceRouter,
    private val resourceManager: ResourceManager,
    feeLoaderMixinFactory: FeeLoaderMixinV2.Factory,
    assetUseCase: AssetUseCase,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
    interactor: VoteReferendumInteractor,
    validationSystem: VoteReferendumValidationSystem,
    validationExecutor: ValidationExecutor,
    referendumFormatter: ReferendumFormatter,
    locksChangeFormatter: LocksChangeFormatter,
    convictionValuesProvider: ConvictionValuesProvider,
    maxActionProviderFactory: MaxActionProviderFactory,
    locksFormatter: LocksFormatter,
) : SetupVoteViewModel(
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
    locksFormatter,
    maxActionProviderFactory,
    feeLoaderMixinFactory
) {

    override val title: Flow<String> = flowOf {
        val formattedNumber = referendumFormatter.formatId(payload.referendumId)
        resourceManager.getString(R.string.referendum_vote_setup_title, formattedNumber)
    }.shareInBackground()

    val ayeButtonStateFlow = validatingVoteType.map { buttonStateFlow(VoteType.AYE, validationVoteType = it, R.string.referendum_vote_aye) }
    val abstainButtonStateFlow = combine(validatingVoteType, abstainVotingSupported) { type, isAbstainSupported ->
        if (isAbstainSupported) {
            buttonStateFlow(VoteType.ABSTAIN, validationVoteType = type, R.string.referendum_vote_abstain)
        } else {
            DescriptiveButtonState.Invisible
        }
    }
    val nayButtonStateFlow = validatingVoteType.map { buttonStateFlow(VoteType.NAY, validationVoteType = it, R.string.referendum_vote_nay) }

    fun ayeClicked() {
        validateVote(VoteType.AYE)
    }

    fun abstainClicked() {
        validateVote(VoteType.ABSTAIN)
    }

    fun nayClicked() {
        validateVote(VoteType.NAY)
    }

    override fun onFinish(
        amount: BigDecimal,
        conviction: Conviction,
        voteType: VoteType,
        validationPayload: VoteReferendaValidationPayload
    ) {
        launch {
            val confirmPayload = ConfirmVoteReferendumPayload(
                _referendumId = payload._referendumId,
                fee = mapFeeToParcel(validationPayload.fee),
                vote = AccountVoteParcelModel(
                    amount = amount,
                    conviction = conviction,
                    voteType = voteType
                )
            )

            router.openConfirmVoteReferendum(confirmPayload)
        }
    }

    private fun buttonStateFlow(targetVoteType: VoteType, validationVoteType: VoteType?, @StringRes labelRes: Int): DescriptiveButtonState {
        return when (validationVoteType) {
            null -> DescriptiveButtonState.Enabled(resourceManager.getString(labelRes))
            targetVoteType -> DescriptiveButtonState.Loading
            else -> DescriptiveButtonState.Disabled(resourceManager.getString(labelRes))
        }
    }
}
