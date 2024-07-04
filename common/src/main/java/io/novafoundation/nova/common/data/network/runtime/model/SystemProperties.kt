package io.novafoundation.nova.common.data.network.runtime.model

class SystemProperties(
    val isEthereum: Boolean?,
    val ss58Format: Int?,
    val SS58Prefix: Int,
    val tokenDecimals: List<Int>,
    val tokenSymbol: List<String>
)
