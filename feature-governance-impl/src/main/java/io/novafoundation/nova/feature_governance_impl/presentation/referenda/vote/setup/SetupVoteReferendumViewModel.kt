package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.view.input.seekbar.SeekbarValue
import io.novafoundation.nova.common.view.input.seekbar.SeekbarValues
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.amountMultiplier
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.votesFor
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.GovernanceVoteAssistant.Change
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.GovernanceVoteAssistant.LocksChange
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.model.AmountChangeModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.model.LocksChangeModel
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlin.time.Duration

class SetupVoteReferendumViewModel(
    private val feeLoaderMixinFactory: FeeLoaderMixin.Factory,
    private val assetUseCase: AssetUseCase,
    private val amountChooserMixinFactory: AmountChooserMixin.Factory,
    private val interactor: VoteReferendumInteractor,
    private val payload: SetupVoteReferendumPayload,
    private val resourceManager: ResourceManager,
    private val router: GovernanceRouter
) : BaseViewModel(), WithFeeLoaderMixin {

    private val selectedAsset = assetUseCase.currentAssetFlow()
        .shareInBackground()

    private val voteAssistantFlow = interactor.voteAssistantFlow(payload.referendumId)
        .shareInBackground()

    override val originFeeMixin = feeLoaderMixinFactory.create(selectedAsset)

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = this,
        assetFlow = selectedAsset,
        balanceField = Asset::free,
        balanceLabel = R.string.wallet_balance_available
    )

    val convictionValues = convictionValues()

    val selectedConvictionIndex = MutableStateFlow(0)

    val title = flowOf {
        resourceManager.getString(R.string.referendum_vote_setup_title, payload.referendumId.value.format())
    }.shareInBackground()

    private val selectedConvictionFlow = selectedConvictionIndex.mapNotNull(convictionValues::valueAt)

    private val locksChangeFlow = combine(
        amountChooserMixin.amount,
        selectedConvictionFlow,
        voteAssistantFlow
    ) { amount, conviction, voteAssistant ->
        val amountPlanks = selectedAsset.first().token.planksFromAmount(amount)

        voteAssistant.estimateLocksAfterVoting(amountPlanks, conviction)
    }
        .shareInBackground()

    val votesFormattedFlow = combine(
        amountChooserMixin.amount,
        selectedConvictionFlow
    ) { amount, conviction ->
        val votes = conviction.votesFor(amount)

        resourceManager.getString(R.string.referendum_votes_format, votes.format())
    }.shareInBackground()

    val locksChangeUiFlow = locksChangeFlow.map(::mapLocksChangeToUi)
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
        showMessage("TODO aye clicked")
    }

    fun nayClicked() {
        showMessage("TODO nay clicked")
    }

    fun backClicked() {
        router.back()
    }

    private suspend fun mapLocksChangeToUi(locksChange: LocksChange): LocksChangeModel {
        return LocksChangeModel(
            amountChange = mapLockedChangeToUi(locksChange.amountChange),
            periodChange = mapLockedPeriodChangeToUi(locksChange.periodChange)
        )
    }

    private suspend fun mapLockedChangeToUi(lockedChange: Change<Balance>): AmountChangeModel {
        val asset = selectedAsset.first()

        val fromFormatted = mapAmountToAmountModel(lockedChange.previousValue, asset, includeAssetTicker = false).token
        val toFormatted = mapAmountToAmountModel(lockedChange.newValue, asset).token

        return when (lockedChange) {
            is Change.Changed -> AmountChangeModel(
                from = fromFormatted,
                to = toFormatted,
                difference = mapAmountToAmountModel(lockedChange.absoluteDifference, asset).token,
                positive = lockedChange.positive
            )
            is Change.Same -> AmountChangeModel(
                from = fromFormatted,
                to = toFormatted,
                difference = null,
                positive = null
            )
        }
    }

    private fun mapLockedPeriodChangeToUi(periodChange: Change<Duration>): AmountChangeModel {
        val from = resourceManager.formatDuration(periodChange.previousValue, estimated = false)
        val to = resourceManager.formatDuration(periodChange.newValue, estimated = false)

        return when (periodChange) {
            is Change.Changed -> {
                val difference = resourceManager.formatDuration(periodChange.absoluteDifference, estimated = false)

                AmountChangeModel(
                    from = from,
                    to = to,
                    difference = resourceManager.getString(R.string.common_maximum_format, difference),
                    positive = periodChange.positive
                )
            }
            is Change.Same -> AmountChangeModel(
                from = from,
                to = to,
                difference = null,
                positive = null
            )
        }
    }

    private fun convictionValues(): SeekbarValues<Conviction> {
        val colors = listOf(
            R.color.multicolor_cyan_light,
            R.color.multicolor_cyan_medium,
            R.color.multicolor_cyan_dark,
            R.color.multicolor_blue_100,
            R.color.multicolor_indigo,
            R.color.multicolor_violet,
            R.color.multicolor_pink_polkadot
        )

        val values = Conviction.values().mapIndexed { index, conviction ->
            SeekbarValue(
                value = conviction,
                label = "${conviction.amountMultiplier()}x",
                labelColorRes = colors.getOrNull(index) ?: R.color.multicolor_pink_polkadot
            )
        }

        return SeekbarValues(values)
    }
}
