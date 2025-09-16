package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list

import android.util.Log
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.masking.unfoldHidden
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.firstLoaded
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.withItemScope
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.view.PlaceholderModel
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.DelegatedState
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.GovernanceLocksOverview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumGroup
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.dapp.GovernanceDAppsInteractor
import io.novafoundation.nova.feature_governance_impl.domain.filters.ReferendaFiltersInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.filters.ReferendumType
import io.novafoundation.nova.feature_governance_api.domain.referendum.filters.ReferendumTypeFilter
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListState
import io.novafoundation.nova.feature_governance_impl.domain.summary.ReferendaSummaryInteractor
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.list.ReferendaListStateModel
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.validation.StartSwipeGovValidationPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatterFactory
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendaGroupModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.TinderGovBannerModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.toReferendumDetailsPrefilledData
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.validation.handleStartSwipeGovValidationFailure
import io.novafoundation.nova.feature_governance_impl.presentation.view.GovernanceLocksModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.WithAssetSelector
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatToken
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.maskable.MaskableValueFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.maskable.MaskableValueFormatterProvider
import io.novafoundation.nova.feature_wallet_api.presentation.model.maskableToken
import io.novafoundation.nova.runtime.ext.supportTinderGov
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ReferendaListViewModel(
    assetSelectorFactory: AssetSelectorFactory,
    private val referendaListInteractor: ReferendaListInteractor,
    private val referendaFiltersInteractor: ReferendaFiltersInteractor,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val selectedAssetSharedState: GovernanceSharedState,
    private val resourceManager: ResourceManager,
    private val updateSystem: UpdateSystem,
    private val governanceRouter: GovernanceRouter,
    private val referendumFormatterFactory: ReferendumFormatterFactory,
    private val governanceDAppsInteractor: GovernanceDAppsInteractor,
    private val referendaSummaryInteractor: ReferendaSummaryInteractor,
    private val tinderGovInteractor: TinderGovInteractor,
    private val selectedMetaAccountUseCase: SelectedAccountUseCase,
    private val validationExecutor: ValidationExecutor,
    private val maskableValueFormatterProvider: MaskableValueFormatterProvider,
    private val tokenFormatter: TokenFormatter
) : BaseViewModel(),
    WithAssetSelector,
    Validatable by validationExecutor {

    private val maskableValueFormatterFlow = maskableValueFormatterProvider.provideFormatter()
        .shareInBackground()

    private val referendumFormatterFlow = maskableValueFormatterFlow.map { referendumFormatterFactory.create(it) }
        .shareInBackground()

    override val assetSelectorMixin = assetSelectorFactory.create(
        scope = this,
        amountProvider = referendaListInteractor::availableVoteAmount
    )

    private val selectedMetaAccount = selectedAccountUseCase.selectedMetaAccountFlow()
    private val selectedChainAndAssetFlow = selectedAssetSharedState.selectedOption

    private val accountAndChainFlow = combineToPair(selectedMetaAccount, selectedChainAndAssetFlow)

    private val referendaFilters = referendaFiltersInteractor.observeReferendumTypeFilter()

    private val referendaListStateFlow = accountAndChainFlow
        .withItemScope(parentScope = this)
        .flatMapLatest { (metaAccountWithOptions, scope) ->
            val (metaAccount, supportedOption) = metaAccountWithOptions
            val chainAndAsset = metaAccountWithOptions.second.assetWithChain
            val accountId = metaAccount.accountIdIn(chainAndAsset.chain)

            referendaListInteractor.referendaListStateFlow(metaAccount, accountId, supportedOption, scope, referendaFilters)
        }
        .catch { Log.e(LOG_TAG, it.message, it) }
        .inBackground()
        .share()

    val governanceTotalLocks = referendaListStateFlow
        .combine(maskableValueFormatterFlow) { referendaState, formatter ->
            val asset = assetSelectorMixin.selectedAssetFlow.first()
            referendaState.map { mapLocksOverviewToUi(formatter, it.locksOverview, asset) }
        }
        .inBackground()
        .shareWhileSubscribed()

    val governanceDelegated = referendaListStateFlow
        .combine(maskableValueFormatterFlow) { referendaState, formatter ->
            val asset = assetSelectorMixin.selectedAssetFlow.first()
            referendaState.map { mapDelegatedToUi(formatter, it.delegated, asset) }
        }
        .inBackground()
        .shareWhileSubscribed()

    private val referendaSummariesFlow = referendaListStateFlow.mapLoading { referenda ->
        val referendaIds = referenda.availableToVoteReferenda.map { it.id }
        referendaSummaryInteractor.getReferendaSummaries(referendaIds, viewModelScope)
    }.shareInBackground()

    val tinderGovBanner = referendaSummariesFlow
        .combine(maskableValueFormatterFlow) { summaries, formatter ->
            val chain = selectedAssetSharedState.chain()
            mapTinderGovToUi(formatter, chain, summaries)
        }
        .inBackground()
        .shareWhileSubscribed()

    val referendaFilterIcon = referendaFilters
        .map { mapFilterTypeToIconRes(it) }
        .inBackground()
        .shareWhileSubscribed()

    val referendaUiFlow = referendaListStateFlow
        .combine(maskableValueFormatterFlow) { summaries, formatter ->
            summaries.map { mapReferendaListToStateList(it, formatter) }
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
        val payload = ReferendumDetailsPayload(
            referendum.id.value,
            allowVoting = referendum.isOngoing,
            prefilledData = referendum.toReferendumDetailsPrefilledData()
        )
        governanceRouter.openReferendum(payload)
    }

    fun openTinderGovCards() = launch {
        val payload = StartSwipeGovValidationPayload(
            chain = selectedAssetSharedState.chain(),
            metaAccount = selectedMetaAccountUseCase.getSelectedMetaAccount()
        )

        validationExecutor.requireValid(
            validationSystem = tinderGovInteractor.startSwipeGovValidationSystem(),
            payload = payload,
            validationFailureTransformerCustom = { validationFailure, _ ->
                handleStartSwipeGovValidationFailure(
                    resourceManager,
                    validationFailure,
                    governanceRouter
                )
            }
        ) {
            governanceRouter.openTinderGovCards()
        }
    }

    private fun mapLocksOverviewToUi(
        maskableValueFormatter: MaskableValueFormatter,
        locksOverview: GovernanceLocksOverview?,
        asset: Asset
    ): GovernanceLocksModel? {
        if (locksOverview == null) return null

        return GovernanceLocksModel(
            title = resourceManager.getString(R.string.wallet_balance_locked),
            amount = maskableValueFormatter.format { tokenFormatter.formatToken(locksOverview.locked, asset) },
            showUnlockableLocks = maskableValueFormatter.format { locksOverview.hasClaimableLocks }.unfoldHidden { false }
        )
    }

    private fun mapDelegatedToUi(maskableValueFormatter: MaskableValueFormatter, delegatedState: DelegatedState, asset: Asset): GovernanceLocksModel? {
        return when (delegatedState) {
            is DelegatedState.Delegated -> GovernanceLocksModel(
                amount = maskableValueFormatter.format { tokenFormatter.formatToken(delegatedState.amount, asset) },
                title = resourceManager.getString(R.string.delegation_your_delegations),
                showUnlockableLocks = false
            )

            DelegatedState.NotDelegated -> GovernanceLocksModel(
                amount = null,
                title = resourceManager.getString(R.string.common_add_delegation),
                showUnlockableLocks = false
            )

            DelegatedState.DelegationNotSupported -> null
        }
    }

    private fun mapTinderGovToUi(
        maskableValueFormatter: MaskableValueFormatter,
        chain: Chain,
        referendaSummariesLoadingState: ExtendedLoadingState<Map<ReferendumId, String>>
    ): TinderGovBannerModel? {
        if (!chain.supportTinderGov()) return null

        val referendumSummaries = referendaSummariesLoadingState.dataOrNull ?: return null

        return TinderGovBannerModel(
            if (referendumSummaries.isEmpty()) {
                null
            } else {
                maskableValueFormatter.format {
                    resourceManager.getString(R.string.referenda_swipe_gov_banner_chip, referendumSummaries.size)
                }
            }
        )
    }

    private fun mapReferendumGroupToUi(referendumGroup: ReferendumGroup, groupSize: Int, maskableValueFormatter: MaskableValueFormatter): ReferendaGroupModel {
        val nameRes = when (referendumGroup) {
            ReferendumGroup.ONGOING -> R.string.common_ongoing
            ReferendumGroup.COMPLETED -> R.string.common_completed
        }

        return ReferendaGroupModel(
            name = resourceManager.getString(nameRes),
            badge = maskableValueFormatter.format { groupSize.format() }
        )
    }

    private fun mapFilterTypeToIconRes(it: ReferendumTypeFilter) =
        if (it.selectedType == ReferendumType.ALL) {
            R.drawable.ic_chip_filter
        } else {
            R.drawable.ic_chip_filter_indicator
        }

    private suspend fun mapReferendaListToStateList(state: ReferendaListState, maskableValueFormatter: MaskableValueFormatter): ReferendaListStateModel {
        val asset = assetSelectorMixin.selectedAssetFlow.first()
        val chain = selectedAssetSharedState.chain()

        val referendaList = state.groupedReferenda.toListWithHeaders(
            keyMapper = { group, referenda -> mapReferendumGroupToUi(group, referenda.size, maskableValueFormatter) },
            valueMapper = { referendumFormatterFlow.first().formatReferendumPreview(it, asset.token, chain) }
        )

        val placeholderModel = mapReferendaListPlaceholder(referendaList)

        return ReferendaListStateModel(placeholderModel, referendaList)
    }

    private suspend fun mapReferendaListPlaceholder(referendaList: List<Any>): PlaceholderModel? {
        val selectedReferendaType = referendaFilters.first().selectedType

        return if (referendaList.isEmpty()) {
            if (selectedReferendaType == ReferendumType.ALL) {
                PlaceholderModel(
                    resourceManager.getString(R.string.referenda_list_placeholder),
                    R.drawable.ic_placeholder
                )
            } else {
                PlaceholderModel(
                    resourceManager.getString(R.string.referenda_list_filter_placeholder),
                    R.drawable.ic_planet_outline
                )
            }
        } else {
            null
        }
    }

    fun governanceLocksClicked() {
        governanceRouter.openGovernanceLocksOverview()
    }

    fun delegationsClicked() {
        launch {
            val state = referendaListStateFlow.firstLoaded()

            if (state.delegated is DelegatedState.Delegated) {
                governanceRouter.openYourDelegations()
            } else {
                governanceRouter.openAddDelegation()
            }
        }
    }

    fun searchClicked() {
        governanceRouter.openReferendaSearch()
    }

    fun filtersClicked() {
        governanceRouter.openReferendaFilters()
    }
}
