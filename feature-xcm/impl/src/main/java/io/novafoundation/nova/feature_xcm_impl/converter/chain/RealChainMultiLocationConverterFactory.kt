package io.novafoundation.nova.feature_xcm_impl.converter.chain

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_xcm_api.converter.chain.ChainMultiLocationConverter
import io.novafoundation.nova.feature_xcm_api.converter.chain.ChainMultiLocationConverterFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

@FeatureScope
class RealChainMultiLocationConverterFactory @Inject constructor(
    private val chainRegistry: ChainRegistry
) : ChainMultiLocationConverterFactory {

    override fun resolveSelfAndChildrenParachains(self: Chain): ChainMultiLocationConverter {
        return CompoundChainLocationConverter(
            LocalChainMultiLocationConverter(self),
            ChildParachainLocationConverter(self, chainRegistry)
        )
    }
}
