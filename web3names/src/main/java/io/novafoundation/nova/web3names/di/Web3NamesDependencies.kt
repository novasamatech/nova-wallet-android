package io.novafoundation.nova.web3names.di

import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

interface Web3NamesDependencies {

    val networkApiCreator: NetworkApiCreator

    @Named(REMOTE_STORAGE_SOURCE)
    fun remoteStorageSource(): StorageDataSource
}
