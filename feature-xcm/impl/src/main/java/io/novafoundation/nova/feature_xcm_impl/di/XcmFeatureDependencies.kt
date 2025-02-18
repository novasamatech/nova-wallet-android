package io.novafoundation.nova.feature_xcm_impl.di

import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface XcmFeatureDependencies {

    val chainRegistry: ChainRegistry

    val runtimeCallApi: MultiChainRuntimeCallsApi
}
