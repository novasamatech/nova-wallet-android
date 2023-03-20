package io.novafoundation.nova.feature_governance_impl.di.modules.v2

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v2.PolkassemblyV2Api
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v2.PolkassemblyV2ReferendaDataSource

@Module
class PolkassemblyV2Module {

    @Provides
    @FeatureScope
    fun provideApi(apiCreator: NetworkApiCreator): PolkassemblyV2Api = apiCreator.create(PolkassemblyV2Api::class.java)

    @Provides
    @FeatureScope
    fun provideDataSource(api: PolkassemblyV2Api): PolkassemblyV2ReferendaDataSource = PolkassemblyV2ReferendaDataSource(api)
}
