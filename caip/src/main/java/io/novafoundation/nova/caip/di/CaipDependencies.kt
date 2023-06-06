package io.novafoundation.nova.caip.di

import com.google.gson.Gson
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

interface CaipDependencies {

    val networkApiCreator: NetworkApiCreator

    val gson: Gson

    val chainRegistry: ChainRegistry
}
