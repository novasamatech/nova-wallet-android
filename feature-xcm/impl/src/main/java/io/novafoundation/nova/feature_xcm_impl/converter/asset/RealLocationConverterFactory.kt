package io.novafoundation.nova.feature_xcm_impl.converter.asset

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_xcm_api.config.XcmConfigRepository
import io.novafoundation.nova.feature_xcm_api.converter.LocationConverterFactory
import io.novafoundation.nova.feature_xcm_api.converter.asset.ChainAssetLocationConverter
import io.novafoundation.nova.feature_xcm_api.converter.chain.ChainLocationConverter
import io.novafoundation.nova.feature_xcm_impl.converter.chain.RealChainLocationConverter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import javax.inject.Inject

@FeatureScope
class RealLocationConverterFactory @Inject constructor(
    private val xcmConfigRepository: XcmConfigRepository,
    private val chainRegistry: ChainRegistry,
): LocationConverterFactory {

    override suspend fun createChainConverter(): ChainLocationConverter {
        val config = xcmConfigRepository.awaitXcmConfig()
        return RealChainLocationConverter(config.chains, chainRegistry)
    }

    override suspend fun createAssetLocationConverter(): ChainAssetLocationConverter {
        val config = xcmConfigRepository.awaitXcmConfig()
        val chainLocationConverter = RealChainLocationConverter(config.chains, chainRegistry)
        return RealChainAssetLocationConverter(config.assets, chainLocationConverter, chainRegistry)
    }
}
