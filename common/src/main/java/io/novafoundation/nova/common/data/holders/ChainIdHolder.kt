package io.novafoundation.nova.common.data.holders

interface ChainIdHolder {

    suspend fun chainId(): String
}
