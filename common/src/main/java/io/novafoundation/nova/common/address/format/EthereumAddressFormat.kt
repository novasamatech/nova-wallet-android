package io.novafoundation.nova.common.address.format

import jp.co.soramitsu.fearless_utils.extensions.asEthereumAccountId
import jp.co.soramitsu.fearless_utils.extensions.asEthereumAddress
import jp.co.soramitsu.fearless_utils.extensions.asEthereumPublicKey
import jp.co.soramitsu.fearless_utils.extensions.isValid
import jp.co.soramitsu.fearless_utils.extensions.toAccountId
import jp.co.soramitsu.fearless_utils.extensions.toAddress

class EthereumAddressFormat : AddressFormat {

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
