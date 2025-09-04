package io.novafoundation.nova.common.address.format

interface AddressFormat {

    val scheme: AddressScheme

    companion object {

        fun evm(): AddressFormat {
            return EthereumAddressFormat()
        }

        fun defaultForScheme(scheme: AddressScheme): AddressFormat {
            return when (scheme) {
                AddressScheme.EVM -> EthereumAddressFormat()
                AddressScheme.SUBSTRATE -> SubstrateAddressFormat.generic()
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
