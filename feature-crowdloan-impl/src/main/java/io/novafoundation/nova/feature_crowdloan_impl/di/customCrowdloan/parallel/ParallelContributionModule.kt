package io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.parallel

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.parallel.ParallelApi

@Module
class ParallelContributionModule {

    @Provides
    @FeatureScope
    fun provideParallelApi(
        networkApiCreator: NetworkApiCreator,
    ) = networkApiCreator.create(ParallelApi::class.java, customBaseUrl = ParallelApi.BASE_URL)
}
