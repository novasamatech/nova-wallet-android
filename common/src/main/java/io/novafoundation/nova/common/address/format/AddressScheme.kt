package io.novafoundation.nova.common.address.format

enum class AddressScheme {
    /**
     * 20-byte address, Ethereum-like address encoding
     */
    EVM,

    /**
     * 32-byte address, ss58 address encoding
     */
    SUBSTRATE
}
