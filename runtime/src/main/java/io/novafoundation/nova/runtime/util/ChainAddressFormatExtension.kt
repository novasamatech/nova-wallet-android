package io.novafoundation.nova.runtime.util

import io.novafoundation.nova.common.address.format.AddressFormat
import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.common.address.format.EthereumAddressFormat
import io.novafoundation.nova.common.address.format.SubstrateAddressFormat
import io.novafoundation.nova.runtime.ext.addressScheme
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun AddressFormat.Companion.forChain(chain: Chain): AddressFormat {
    return when (chain.addressScheme) {
        AddressScheme.EVM -> EthereumAddressFormat()
        AddressScheme.SUBSTRATE -> SubstrateAddressFormat.forSS58rPrefix(chain.addressPrefix.toShort())
    }
}
