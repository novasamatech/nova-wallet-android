package io.novafoundation.nova.web3names.di

import com.google.gson.Gson
import io.novafoundation.nova.caip.caip19.Caip19MatcherFactory
import io.novafoundation.nova.caip.caip19.Caip19Parser
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

interface Web3NamesDependencies {

    val networkApiCreator: NetworkApiCreator

    val gson: Gson

    val caip19Parser: Caip19Parser

    val caip19MatcherFactory: Caip19MatcherFactory

    @Named(REMOTE_STORAGE_SOURCE)
    fun remoteStorageSource(): StorageDataSource
}
