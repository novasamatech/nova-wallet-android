package io.novafoundation.nova.runtime.multiNetwork.qr

import jp.co.soramitsu.fearless_utils.encrypt.qr.QrSharing
import jp.co.soramitsu.fearless_utils.encrypt.qr.formats.AddressQrFormat
import jp.co.soramitsu.fearless_utils.encrypt.qr.formats.SubstrateQrFormat

class MultiChainQrSharingFactory {

    fun create(addressValidator: (String) -> Boolean): QrSharing {
        val substrateFormat = SubstrateQrFormat()
        val onlyAddressFormat = AddressQrFormat(addressValidator)

        val formats = listOf(
            substrateFormat,
            onlyAddressFormat
        )

        return QrSharing(
            decodingFormats = formats,
            encodingFormat = onlyAddressFormat
        )
    }
}
