package io.novafoundation.nova.runtime.multiNetwork.qr

import jp.co.soramitsu.fearless_utils.encrypt.qr.QrSharing
import jp.co.soramitsu.fearless_utils.encrypt.qr.formats.AddressQrFormat
import jp.co.soramitsu.fearless_utils.encrypt.qr.formats.SubstrateQrFormat

class MultiChainQrSharingFactory {

    fun create(addressValidator: (String) -> Boolean): QrSharing {
        val mainFormat = SubstrateQrFormat()

        val formats = listOf(
            mainFormat,
            AddressQrFormat(addressValidator)
        )

        return QrSharing(
            decodingFormats = formats,
            encodingFormat = mainFormat
        )
    }
}
