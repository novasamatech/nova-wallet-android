package io.novafoundation.nova.common.address.format

import io.novasama.substrate_sdk_android.extensions.asEthereumAccountId
import io.novasama.substrate_sdk_android.extensions.asEthereumAddress
import io.novasama.substrate_sdk_android.extensions.asEthereumPublicKey
import io.novasama.substrate_sdk_android.extensions.isValid
import io.novasama.substrate_sdk_android.extensions.toAccountId
import io.novasama.substrate_sdk_android.extensions.toAddress

class EthereumAddressFormat : AddressFormat {

    override val scheme: AddressScheme = AddressScheme.EVM

    override fun addressOf(accountId: AddressFormat.AccountId): AddressFormat.Address {
        return accountId.value.asEthereumAccountId()
            .toAddress().value.asAddress()
    }

    override fun accountIdOf(address: AddressFormat.Address): AddressFormat.AccountId {
        return address.value.asEthereumAddress()
            .toAccountId().value.asAccountId()
    }

    override fun accountIdOf(publicKey: AddressFormat.PublicKey): AddressFormat.AccountId {
        return publicKey.value.asEthereumPublicKey()
            .toAccountId().value.asAccountId()
    }

    override fun isValidAddress(address: AddressFormat.Address): Boolean {
        return address.value.asEthereumAddress().isValid()
    }
}
