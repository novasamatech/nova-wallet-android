package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.presentation.mapLoading
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.isAye
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.votes
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.GovernanceLocksOverview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumGroup
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumProposal
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.dapp.GovernanceDAppsInteractor
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendaGroupModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.YourVotePreviewModel
import io.novafoundation.nova.feature_governance_impl.presentation.view.GovernanceLocksModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.WithAssetSelector
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn

class ReferendaListViewModel(
    assetSelectorFactory: AssetSelectorFactory,
    private val referendaListInteractor: ReferendaListInteractor,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val selectedAssetSharedState: GovernanceSharedState,
    private val resourceManager: ResourceManager,
    private val updateSystem: UpdateSystem,
    private val governanceRouter: GovernanceRouter,
    private val referendumFormatter: ReferendumFormatter,
    private val governanceDAppsInteractor: GovernanceDAppsInteractor
) : BaseViewModel(), WithAssetSelector {

    override val assetSelectorMixin = assetSelectorFactory.create(
        scope = this,
        amountProvider = Asset::free
    )

    private val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
    private val selectedChainAndAssetFlow = selectedAssetSharedState.selectedOption

    private val accountAndChainFlow = combineToPair(selectedAccount, selectedChainAndAssetFlow)

    private val referendaListStateFlow = accountAndChainFlow.withLoading { (account, supportedOption) ->
        val chainAndAsset = supportedOption.assetWithChain
        val accountId = account.accountIdIn(chainAndAsset.chain)

        referendaListInteractor.referendaListStateFlow(accountId, supportedOption)
    }
        .inBackground()
        .shareWhileSubscribed()

    val governanceTotalLocks = referendaListStateFlow.mapLoading {
        val asset = assetSelectorMixin.selectedAssetFlow.first()

        mapLocksOverviewToUi(it.locksOverview, asset)
    }
        .inBackground()
        .shareWhileSubscribed()

    val referendaUiFlow = referendaListStateFlow.mapLoading { state ->
        val asset = assetSelectorMixin.selectedAssetFlow.first()

        state.groupedReferenda.toListWithHeaders(
            keyMapper = { group, referenda -> mapReferendumGroupToUi(group, referenda.size) },
            valueMapper = { mapReferendumPreviewToUi(it, asset.token) }
        )
    }
        .inBackground()
        .shareWhileSubscribed()

    init {
        governanceDAppsInteractor.syncGovernanceDapps()
            .launchIn(this)

        updateSystem.start()
            .launchIn(this)
    }

    fun openReferendum(referendum: ReferendumModel) {
        val payload = ReferendumDetailsPayload(referendum.id.value)
        governanceRouter.openReferendum(payload)
    }

    private fun mapLocksOverviewToUi(locksOverview: GovernanceLocksOverview?, asset: Asset): GovernanceLocksModel? {
        if (locksOverview == null) return null

        return GovernanceLocksModel(
            amount = mapAmountToAmountModel(locksOverview.locked, asset).token,
            hasUnlockableLocks = locksOverview.hasClaimableLocks
        )
    }

    private fun mapReferendumGroupToUi(referendumGroup: ReferendumGroup, groupSize: Int): ReferendaGroupModel {
        val nameRes = when (referendumGroup) {
            ReferendumGroup.ONGOING -> R.string.common_ongoing
            ReferendumGroup.COMPLETED -> R.string.common_completed
        }

        return ReferendaGroupModel(
            name = resourceManager.getString(nameRes),
            badge = groupSize.format()
        )
    }

    private fun mapReferendumPreviewToUi(referendum: ReferendumPreview, token: Token): ReferendumModel {
        return ReferendumModel(
            id = referendum.id,
            status = referendumFormatter.formatStatus(referendum.status),
            name = mapReferendumNameToUi(referendum),
            timeEstimation = referendumFormatter.formatTimeEstimation(referendum.status),
            track = referendum.track?.let { referendumFormatter.formatTrack(it, token.configuration) },
            number = referendumFormatter.formatId(referendum.id),
            voting = referendum.voting?.let { referendumFormatter.formatVoting(it, token) },
            yourVote = mapUserVoteToUi(referendum.userVote, token)
        )
    }

    private fun mapReferendumNameToUi(referendum: ReferendumPreview): String {
        return referendum.offChainMetadata?.title
            ?: mapReferendumOnChainNameToUi(referendum)
            ?: referendumFormatter.formatUnknownReferendumTitle(referendum.id)
    }

    private fun mapReferendumOnChainNameToUi(referendum: ReferendumPreview): String? {
        return when (val proposal = referendum.onChainMetadata?.proposal) {
            is ReferendumProposal.Call -> referendumFormatter.formatOnChainName(proposal.call)
            else -> null
        }
    }

    private fun mapUserVoteToUi(vote: AccountVote?, token: Token): YourVotePreviewModel? {
        val isAye = vote?.isAye() ?: return null
        val votes = vote.votes(token.configuration) ?: return null

        val voteTypeRes = if (isAye) R.string.referendum_vote_aye else R.string.referendum_vote_nay
        val colorRes = if (isAye) R.color.text_positive else R.color.text_negative

        return YourVotePreviewModel(
            voteType = resourceManager.getString(voteTypeRes),
            colorRes = colorRes,
            details = resourceManager.getString(R.string.referendum_your_vote_format, votes.total.format())
        )
    }

    fun governanceLocksClicked() {
        governanceRouter.openGovernanceLocksOverview()
    }
}
