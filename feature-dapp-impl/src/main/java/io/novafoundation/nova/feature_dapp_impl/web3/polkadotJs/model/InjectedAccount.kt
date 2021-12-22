package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model

import jp.co.soramitsu.fearless_utils.encrypt.EncryptionType

class InjectedAccount internal constructor(
    val address: String,
    val genesisHash: String?,
    val name: String?,
    val type: String
)

fun InjectedAccount(
    address: String,
    genesisHash: String?,
    name: String?,
    encryption: EncryptionType
) = InjectedAccount(address, genesisHash, name, encryption.rawName)
