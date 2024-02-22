package io.novafoundation.nova.runtime.multiNetwork.qr

import io.novasama.substrate_sdk_android.encrypt.qr.QrSharing
import io.novasama.substrate_sdk_android.encrypt.qr.formats.AddressQrFormat
import io.novasama.substrate_sdk_android.encrypt.qr.formats.SubstrateQrFormat

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
