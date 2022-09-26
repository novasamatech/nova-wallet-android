package io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.scan

import io.novafoundation.nova.common.address.format.asAddress

interface ParitySignerConnectQrDecoder {

    sealed class Error : Throwable() {

        class UnknownAddressFormat : Error()

        class InvalidAddress : Error()
    }

    class Decoded(
        val address: String,
        val accountType: ParitySignerAccount.Type,
    )

    fun decode(qrContent: String): Result<Decoded>
}

private const val DELIMITER = ":"
private const val TYPE_SUBSTRATE = "substrate"
private const val TYPE_ETHEREUM = "ethereum"

class RealParitySignerConnectQrDecoder : ParitySignerConnectQrDecoder {

    override fun decode(qrContent: String): Result<ParitySignerConnectQrDecoder.Decoded> = runCatching {
        val (type, address) = qrContent.split(DELIMITER)

        val accountType = when (type) {
            TYPE_SUBSTRATE -> ParitySignerAccount.Type.SUBSTRATE
            TYPE_ETHEREUM -> ParitySignerAccount.Type.ETHEREUM
            else -> throw ParitySignerConnectQrDecoder.Error.UnknownAddressFormat()
        }

        val isValidAddress = accountType.addressFormat.isValidAddress(address.asAddress())
        if (!isValidAddress) throw ParitySignerConnectQrDecoder.Error.InvalidAddress()

        ParitySignerConnectQrDecoder.Decoded(address, accountType)
    }
}
