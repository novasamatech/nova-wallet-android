package io.novafoundation.nova.feature_governance_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.description.di.DescriptionComponent
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
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.di.ReferendumFullDetailsComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.di.ReferendaListComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.confirm.di.ConfirmReferendumVoteComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.di.SetupVoteReferendumComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.di.ReferendumVotersComponent
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

    fun referendumDetailsFactory(): ReferendumDetailsComponent.Factory

    fun descriptionFactory(): DescriptionComponent.Factory

    fun referendumFullDetailsFactory(): ReferendumFullDetailsComponent.Factory

    fun setupVoteReferendumFactory(): SetupVoteReferendumComponent.Factory

    fun confirmReferendumVoteFactory(): ConfirmReferendumVoteComponent.Factory

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

    fun newDelegationChooseAmountFactory(): NewDelegationChooseAmountComponent.Factory

    fun newDelegationConfirmFactory(): NewDelegationConfirmComponent.Factory

    fun revokeDelegationChooseTracksFactory(): RevokeDelegationChooseTracksComponent.Factory

    fun revokeDelegationConfirmFactory(): RevokeDelegationConfirmComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            deps: GovernanceFeatureDependencies,
            @BindsInstance router: GovernanceRouter,
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
        ]
    )
    interface GovernanceFeatureDependenciesComponent : GovernanceFeatureDependencies
}
