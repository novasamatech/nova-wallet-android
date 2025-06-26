package io.novafoundation.nova.common.address.format

import io.novafoundation.nova.common.utils.GENERIC_ADDRESS_PREFIX
import io.novasama.substrate_sdk_android.ss58.SS58Encoder

interface AddressFormat {

    val scheme: AddressScheme

    companion object {

        fun evm(): AddressFormat {
            return EthereumAddressFormat()
        }

        fun defaultForScheme(scheme: AddressScheme, substrateAddressPrefix: Short = SS58Encoder.GENERIC_ADDRESS_PREFIX): AddressFormat {
            return when (scheme) {
                AddressScheme.EVM -> EthereumAddressFormat()
                AddressScheme.SUBSTRATE -> SubstrateAddressFormat.forSS58rPrefix(substrateAddressPrefix)
            }
        }
    }

    @JvmInline
    value class PublicKey(val value: ByteArray)

    @JvmInline
    value class AccountId(val value: ByteArray)

    @JvmInline
    value class Address(val value: String)

    fun addressOf(accountId: AccountId): Address

    fun accountIdOf(address: Address): AccountId

    fun accountIdOf(publicKey: PublicKey): AccountId

    fun isValidAddress(address: Address): Boolean
}

fun ByteArray.asPublicKey() = AddressFormat.PublicKey(this)
fun ByteArray.asAccountId() = AddressFormat.AccountId(this)
fun String.asAddress() = AddressFormat.Address(this)
