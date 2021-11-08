package io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.astar

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeFactory
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.astar.AstarContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.astar.AstarContributeSubmitter

@Module
class AstarContributionModule {

    @Provides
    @FeatureScope
    fun provideAstarInteractor(
        selectedAssetSharedState: CrowdloanSharedState,
    ) = AstarContributeInteractor(selectedAssetSharedState)

    @Provides
    @FeatureScope
    fun provideAstarSubmitter(
        interactor: AstarContributeInteractor,
    ) = AstarContributeSubmitter(interactor)

    @Provides
    @FeatureScope
    fun provideAstarExtraFlow(
        interactor: AstarContributeInteractor,
        resourceManager: ResourceManager,
    ) = AstarExtraBonusFlow(
        interactor = interactor,
        resourceManager = resourceManager
    )

    @Provides
    @FeatureScope
    @IntoSet
    fun provideAstarFactory(
        submitter: AstarContributeSubmitter,
        astarExtraBonusFlow: AstarExtraBonusFlow,
    ): CustomContributeFactory = AstarContributeFactory(
        submitter = submitter,
        extraBonusFlow = astarExtraBonusFlow
    )
}
