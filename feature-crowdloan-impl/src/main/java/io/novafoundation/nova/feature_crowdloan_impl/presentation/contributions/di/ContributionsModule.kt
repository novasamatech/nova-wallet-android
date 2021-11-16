package io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.acala.AcalaApi
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.parallel.ParallelApi
import io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.source.CompositeContributionsSource
import io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.source.DirectContributionsSource
import io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.source.LiquidAcalaContributionSource
import io.novafoundation.nova.feature_crowdloan_impl.data.repository.contributions.source.ParallelContributionSource
import io.novafoundation.nova.feature_crowdloan_impl.data.source.contribution.ContributionSource
import io.novafoundation.nova.feature_crowdloan_impl.domain.contributions.ContributionsInteractor

@Module
class ContributionsModule {

    @Provides
    @ScreenScope
    @IntoSet
    fun directSource(
        crowdloanRepository: CrowdloanRepository,
    ): ContributionSource = DirectContributionsSource(crowdloanRepository)

    @Provides
    @ScreenScope
    @IntoSet
    fun acalaLiquidSource(
        acalaApi: AcalaApi,
    ): ContributionSource = LiquidAcalaContributionSource(acalaApi)

    @Provides
    @ScreenScope
    @IntoSet
    fun parallelSource(
        parallelApi: ParallelApi,
    ): ContributionSource = ParallelContributionSource(parallelApi)

    @Provides
    @ScreenScope
    fun compositeSource(
        childSources: Set<@JvmSuppressWildcards ContributionSource>,
    ): ContributionSource = CompositeContributionsSource(childSources)

    @Provides
    @ScreenScope
    fun provideContributionsInteractor(
        source: ContributionSource,
        crowdloanRepository: CrowdloanRepository,
        accountRepository: AccountRepository,
        crowdloanSharedState: CrowdloanSharedState,
    ) = ContributionsInteractor(
        source = source,
        crowdloanRepository = crowdloanRepository,
        accountRepository = accountRepository,
        selectedAssetState = crowdloanSharedState
    )
}
