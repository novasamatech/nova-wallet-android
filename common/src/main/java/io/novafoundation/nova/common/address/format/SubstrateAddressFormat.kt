package io.novafoundation.nova.common.address.format

import io.novafoundation.nova.common.utils.GENERIC_ADDRESS_PREFIX
import io.novafoundation.nova.common.utils.substrateAccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.addressPrefix
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAddress

class SubstrateAddressFormat private constructor(
    private val addressPrefix: Short?
) : AddressFormat {

    override val scheme: AddressScheme = AddressScheme.SUBSTRATE

    companion object {

        fun forSS58rPrefix(prefix: Short): SubstrateAddressFormat {
            return SubstrateAddressFormat(prefix)
        }
    }

    override fun addressOf(accountId: AddressFormat.AccountId): AddressFormat.Address {
        val addressPrefixOrDefault = addressPrefix ?: SS58Encoder.GENERIC_ADDRESS_PREFIX
        return accountId.value.toAddress(addressPrefixOrDefault).asAddress()
    }

    override fun accountIdOf(address: AddressFormat.Address): AddressFormat.AccountId {
        val accountId = address.value.toAccountId()

        addressPrefix?.let {
            require(addressPrefix == address.value.addressPrefix()) {
                "Address prefix mismatch. Expected: $addressPrefix, Got: ${address.value}"
            }
        }

        return accountId.asAccountId()
    }

    override fun accountIdOf(publicKey: AddressFormat.PublicKey): AddressFormat.AccountId {
        return publicKey.value.substrateAccountId().asAccountId()
    }

    override fun isValidAddress(address: AddressFormat.Address): Boolean {
        return kotlin.runCatching { accountIdOf(address) }.isSuccess
    }
}
