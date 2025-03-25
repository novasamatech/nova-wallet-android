package io.novafoundation.nova.feature_governance_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksCommunicator
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_deep_link_building.di.DeepLinkBuildingFeatureApi
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.description.di.DescriptionComponent
import io.novafoundation.nova.feature_governance_impl.presentation.common.description.di.ReferendumInfoComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.delegators.di.DelegateDelegatorsComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.main.di.DelegateDetailsComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.detail.votedReferenda.di.VotedReferendaComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.list.di.DelegateListComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.search.di.DelegateSearchComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegated.di.YourDelegationsComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseAmount.di.NewDelegationChooseAmountComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.chooseTrack.di.NewDelegationChooseTracksComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.create.confirm.di.NewDelegationConfirmComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.removeVotes.di.RemoveVotesComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.chooseTracks.di.RevokeDelegationChooseTracksComponent
import io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegation.revoke.confirm.di.RevokeDelegationConfirmComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.di.ReferendumDetailsComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.filters.di.ReferendaFiltersComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.di.ReferendumFullDetailsComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.di.ReferendaListComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.search.di.ReferendaSearchComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.di.ConfirmReferendumVoteComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.referenda.di.SetupReferendumVoteComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.di.SetupTinderGovVoteComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.TinderGovVoteCommunicator
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.di.ReferendumVotersComponent
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.basket.di.TinderGovBasketComponent
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.di.TinderGovCardsComponent
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.confirm.di.ConfirmTinderGovVoteComponent
import io.novafoundation.nova.feature_governance_impl.presentation.tracks.select.governanceTracks.di.SelectGovernanceTracksComponent
import io.novafoundation.nova.feature_governance_impl.presentation.unlock.confirm.di.ConfirmGovernanceUnlockComponent
import io.novafoundation.nova.feature_governance_impl.presentation.unlock.list.di.GovernanceLocksOverviewComponent
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        GovernanceFeatureDependencies::class,
    ],
    modules = [
        GovernanceFeatureModule::class,
    ]
)
@FeatureScope
interface GovernanceFeatureComponent : GovernanceFeatureApi {

    fun referendaListFactory(): ReferendaListComponent.Factory

    fun referendaSearchFactory(): ReferendaSearchComponent.Factory

    fun referendumDetailsFactory(): ReferendumDetailsComponent.Factory

    fun descriptionFactory(): DescriptionComponent.Factory

    fun referendumInfoFactory(): ReferendumInfoComponent.Factory

    fun referendumFullDetailsFactory(): ReferendumFullDetailsComponent.Factory

    fun setupReferendumVoteFactory(): SetupReferendumVoteComponent.Factory

    fun setupTinderGovVoteFactory(): SetupTinderGovVoteComponent.Factory

    fun confirmReferendumVoteFactory(): ConfirmReferendumVoteComponent.Factory

    fun confirmTinderGovVoteFactory(): ConfirmTinderGovVoteComponent.Factory

    fun referendumVotersFactory(): ReferendumVotersComponent.Factory

    fun confirmGovernanceUnlockFactory(): ConfirmGovernanceUnlockComponent.Factory

    fun governanceLocksOverviewFactory(): GovernanceLocksOverviewComponent.Factory

    fun delegateListFactory(): DelegateListComponent.Factory

    fun delegateSearchFactory(): DelegateSearchComponent.Factory

    fun delegateDetailsFactory(): DelegateDetailsComponent.Factory

    fun votedReferendaFactory(): VotedReferendaComponent.Factory

    fun removeVoteFactory(): RemoveVotesComponent.Factory

    fun delegateDelegatorsFactory(): DelegateDelegatorsComponent.Factory

    fun yourDelegationsFactory(): YourDelegationsComponent.Factory

    fun newDelegationChooseTracks(): NewDelegationChooseTracksComponent.Factory

    fun selectGovernanceTracks(): SelectGovernanceTracksComponent.Factory

    fun newDelegationChooseAmountFactory(): NewDelegationChooseAmountComponent.Factory

    fun newDelegationConfirmFactory(): NewDelegationConfirmComponent.Factory

    fun revokeDelegationChooseTracksFactory(): RevokeDelegationChooseTracksComponent.Factory

    fun revokeDelegationConfirmFactory(): RevokeDelegationConfirmComponent.Factory

    fun referendaFiltersFactory(): ReferendaFiltersComponent.Factory

    fun tinderGovCardsFactory(): TinderGovCardsComponent.Factory

    fun tinderGovBasketFactory(): TinderGovBasketComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            deps: GovernanceFeatureDependencies,
            @BindsInstance router: GovernanceRouter,
            @BindsInstance selectTracksCommunicator: SelectTracksCommunicator,
            @BindsInstance tinderGovVoteCommunicator: TinderGovVoteCommunicator
        ): GovernanceFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            RuntimeApi::class,
            WalletFeatureApi::class,
            AccountFeatureApi::class,
            DAppFeatureApi::class,
            DbApi::class,
            DeepLinkBuildingFeatureApi::class
        ]
    )
    interface GovernanceFeatureDependenciesComponent : GovernanceFeatureDependencies
}
