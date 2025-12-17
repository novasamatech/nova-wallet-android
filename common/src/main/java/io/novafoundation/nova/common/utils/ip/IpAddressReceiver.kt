package io.novafoundation.nova.common.utils.ip

interface IpAddressReceiver {
    suspend fun get(): String
}

suspend fun IpAddressReceiver.getOrNull() = runCatching { get() }.getOrNull()
