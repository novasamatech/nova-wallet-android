package io.novafoundation.nova.feature_governance_impl.di.modules.v1

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v1.PolkassemblyV1Api
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v1.PolkassemblyV1ReferendaDataSource

@Module
class PolkassemblyV1Module {

    @Provides
    @FeatureScope
    fun provideApi(apiCreator: NetworkApiCreator): PolkassemblyV1Api = apiCreator.create(PolkassemblyV1Api::class.java)

    @Provides
    @FeatureScope
    fun provideDataSource(api: PolkassemblyV1Api): PolkassemblyV1ReferendaDataSource = PolkassemblyV1ReferendaDataSource(api)
}
