package io.novafoundation.nova.feature_vote.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_vote.presentation.VoteRouter
import io.novafoundation.nova.feature_vote.presentation.vote.di.VoteComponent

@Component(
    dependencies = [
        VoteFeatureDependencies::class
    ],
    modules = [
        VoteFeatureModule::class,
    ]
)
@FeatureScope
interface VoteFeatureComponent : VoteFeatureApi {

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance voteRouter: VoteRouter,
            deps: VoteFeatureDependencies
        ): VoteFeatureComponent
    }

    fun voteComponentFactory(): VoteComponent.Factory

    @Component(
        dependencies = [
            CommonApi::class,
            AccountFeatureApi::class
        ]
    )
    interface VoteFeatureDependenciesComponent : VoteFeatureDependencies
}
