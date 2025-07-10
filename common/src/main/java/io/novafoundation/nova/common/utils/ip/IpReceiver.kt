package io.novafoundation.nova.common.utils.ip

interface IpReceiver {
    suspend fun get(): String
}

suspend fun IpReceiver.getOrNull() = runCatching { get() }.getOrNull()
