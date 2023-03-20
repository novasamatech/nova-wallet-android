package io.novafoundation.nova.feature_governance_impl.di.modules.v1

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v1.SubSquareV1Api
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v1.SubSquareV1ReferendaDataSource

@Module
class SubSquareV1Module {

    @Provides
    @FeatureScope
    fun provideApi(apiCreator: NetworkApiCreator): SubSquareV1Api = apiCreator.create(SubSquareV1Api::class.java)

    @Provides
    @FeatureScope
    fun provideDataSource(api: SubSquareV1Api): SubSquareV1ReferendaDataSource = SubSquareV1ReferendaDataSource(api)
}
