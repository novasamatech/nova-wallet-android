package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model

import io.novasama.substrate_sdk_android.encrypt.EncryptionType

class InjectedAccount internal constructor(
    val address: String,
    val genesisHash: String?,
    val name: String?,
    val type: String?
)

fun InjectedAccount(
    address: String,
    genesisHash: String?,
    name: String?,
    encryption: EncryptionType?
) = InjectedAccount(address, genesisHash, name, encryption?.rawName)
