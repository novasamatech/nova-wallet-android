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
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.description.di.ReferendumDescriptionComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.di.ReferendumDetailsComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.full.di.ReferendumFullDetailsComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.di.ReferendaListComponent
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters.di.ReferendumVotersComponent
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

    fun referendumDescriptionFactory(): ReferendumDescriptionComponent.Factory

    fun referendumFullDetailsFactory(): ReferendumFullDetailsComponent.Factory

    fun referendumVotersFactory(): ReferendumVotersComponent.Factory

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
