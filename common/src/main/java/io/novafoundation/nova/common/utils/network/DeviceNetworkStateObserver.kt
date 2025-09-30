package io.novafoundation.nova.common.utils.network

import kotlinx.coroutines.flow.Flow

interface DeviceNetworkStateObserver {
    fun observeIsNetworkAvailable(): Flow<Boolean>
}
