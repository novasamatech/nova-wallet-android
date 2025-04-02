package io.novafoundation.nova.feature_xcm_api.converter.chain

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface ChainMultiLocationConverterFactory {

    fun resolveSelfAndChildrenParachains(self: Chain): ChainMultiLocationConverter
}
