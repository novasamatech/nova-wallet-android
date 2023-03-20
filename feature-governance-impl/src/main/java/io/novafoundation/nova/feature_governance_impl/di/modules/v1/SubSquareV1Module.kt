package io.novafoundation.nova.feature_governance_impl.di.modules.v1

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v1.SubSquareV1ReferendaDataSource

@Module
class SubSquareV1Module {

//    @Provides
//    @FeatureScope
//    fun provideApi(apiCreator: NetworkApiCreator): SubSquareV2Api = apiCreator.create(SubSquareV2Api::class.java)

    @Provides
    @FeatureScope
    fun provideDataSource(): SubSquareV1ReferendaDataSource = SubSquareV1ReferendaDataSource()
}
