package io.novafoundation.nova.feature_governance_impl.di.modules.v2

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v2.SubSquareV2Api
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v2.SubSquareV2ReferendaDataSource

@Module
class SubSquareV2Module {

    @Provides
    @FeatureScope
    fun provideApi(apiCreator: NetworkApiCreator): SubSquareV2Api = apiCreator.create(SubSquareV2Api::class.java)

    @Provides
    @FeatureScope
    fun provideDataSource(api: SubSquareV2Api): SubSquareV2ReferendaDataSource = SubSquareV2ReferendaDataSource(api)
}
