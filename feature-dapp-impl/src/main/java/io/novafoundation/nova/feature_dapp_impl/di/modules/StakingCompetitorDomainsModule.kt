package io.novafoundation.nova.feature_dapp_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_dapp_impl.BuildConfig
import io.novafoundation.nova.feature_dapp_impl.data.network.competitors.StakingCompetitorDomainsApi
import io.novafoundation.nova.feature_dapp_impl.data.repository.RealStakingCompetitorDomainsRepository
import io.novafoundation.nova.feature_dapp_impl.data.repository.StakingCompetitorDomainsRepository

@Module
class StakingCompetitorDomainsModule {

    @Provides
    @FeatureScope
    fun provideApi(apiCreator: NetworkApiCreator): StakingCompetitorDomainsApi =
        apiCreator.create(StakingCompetitorDomainsApi::class.java)

    @Provides
    @FeatureScope
    fun provideRepository(
        api: StakingCompetitorDomainsApi
    ): StakingCompetitorDomainsRepository = RealStakingCompetitorDomainsRepository(
        api = api,
        remoteUrl = BuildConfig.STAKING_COMPETITORS_URL
    )
}
