package io.novafoundation.nova.feature_xcm_impl.di

import com.google.gson.Gson
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.interfaces.FileCache
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface XcmFeatureDependencies {

    val chainRegistry: ChainRegistry

    val runtimeCallApi: MultiChainRuntimeCallsApi

    val rootScope: RootScope

    val fileCache: FileCache

    val gson: Gson

    val networkApiCreator: NetworkApiCreator
}
