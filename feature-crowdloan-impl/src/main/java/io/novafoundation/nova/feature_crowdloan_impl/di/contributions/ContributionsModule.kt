package io.novafoundation.nova.feature_crowdloan_impl.di.contributions

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.acala.AcalaApi
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.parallel.ParallelApi
import io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.source.CompositeContributionsSource
import io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.source.LiquidAcalaContributionSource
import io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.source.ParallelContributionSource
import io.novafoundation.nova.feature_crowdloan_impl.data.source.contribution.ExternalContributionSource
import io.novafoundation.nova.feature_crowdloan_impl.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.runtime.repository.ChainStateRepository

@Module
class ContributionsModule {

    @Provides
    @FeatureScope
    @IntoSet
    fun acalaLiquidSource(
        acalaApi: AcalaApi,
    ): ExternalContributionSource = LiquidAcalaContributionSource(acalaApi)

    @Provides
    @FeatureScope
    @IntoSet
    fun parallelSource(
        parallelApi: ParallelApi,
    ): ExternalContributionSource = ParallelContributionSource(parallelApi)

    @Provides
    @FeatureScope
    fun compositeSource(
        childSources: Set<@JvmSuppressWildcards ExternalContributionSource>,
    ): ExternalContributionSource = CompositeContributionsSource(childSources)

    @Provides
    @FeatureScope
    fun provideContributionsInteractor(
        externalContributionsSource: ExternalContributionSource,
        crowdloanRepository: CrowdloanRepository,
        accountRepository: AccountRepository,
        crowdloanSharedState: CrowdloanSharedState,
        chainStateRepository: ChainStateRepository,
    ) = ContributionsInteractor(
        externalContributionSource = externalContributionsSource,
        crowdloanRepository = crowdloanRepository,
        accountRepository = accountRepository,
        selectedAssetState = crowdloanSharedState,
        chainStateRepository = chainStateRepository
    )
}
