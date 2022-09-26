package io.novafoundation.nova.common.address.format

import io.novafoundation.nova.common.utils.DEFAULT_PREFIX
import io.novafoundation.nova.common.utils.isValidSS58Address
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.addressPrefix
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.publicKeyToSubstrateAccountId
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAccountId
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAddress

class SpecificSubstrateAddressFormat(private val ss58Prefix: Short) : AddressFormat {

    override fun addressOf(accountId: AddressFormat.AccountId): AddressFormat.Address {
        return accountId.value.toAddress(ss58Prefix).asAddress()
    }

    override fun accountIdOf(address: AddressFormat.Address): AddressFormat.AccountId {
        return address.value.toAccountId().asAccountId()
    }

    override fun accountIdOf(publicKey: AddressFormat.PublicKey): AddressFormat.AccountId {
        return publicKey.value.publicKeyToSubstrateAccountId().asAccountId()
    }

    override fun isValidAddress(address: AddressFormat.Address): Boolean {
        return runCatching {
            address.value.isValidSS58Address()
                && address.value.addressPrefix() == ss58Prefix
        }.getOrDefault(false)
    }
}

class AnySubstrateChainAddressFormat : AddressFormat {

    override fun addressOf(accountId: AddressFormat.AccountId): AddressFormat.Address {
        return accountId.value.toAddress(SS58Encoder.DEFAULT_PREFIX).asAddress()
    }

    override fun accountIdOf(address: AddressFormat.Address): AddressFormat.AccountId {
        return address.value.toAccountId().asAccountId()
    }

    override fun accountIdOf(publicKey: AddressFormat.PublicKey): AddressFormat.AccountId {
        return publicKey.value.publicKeyToSubstrateAccountId().asAccountId()
    }

    override fun isValidAddress(address: AddressFormat.Address): Boolean {
        return address.value.isValidSS58Address()
    }
}
