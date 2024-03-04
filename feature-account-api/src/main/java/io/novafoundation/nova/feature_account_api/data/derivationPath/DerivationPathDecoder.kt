package io.novafoundation.nova.feature_account_api.data.derivationPath

import io.novasama.substrate_sdk_android.encrypt.junction.BIP32JunctionDecoder
import io.novasama.substrate_sdk_android.encrypt.junction.JunctionDecoder
import io.novasama.substrate_sdk_android.encrypt.junction.SubstrateJunctionDecoder

object DerivationPathDecoder {

    @Throws
    fun decodeEthereumDerivationPath(derivationPath: String?): JunctionDecoder.DecodeResult? {
        if (derivationPath.isNullOrEmpty()) return null

        return BIP32JunctionDecoder.decode(derivationPath)
    }

    @Throws
    fun decodeSubstrateDerivationPath(derivationPath: String?): JunctionDecoder.DecodeResult? {
        if (derivationPath.isNullOrEmpty()) return null

        return SubstrateJunctionDecoder.decode(derivationPath)
    }

    fun isEthereumDerivationPathValid(derivationPath: String?): Boolean {
        return try {
            decodeEthereumDerivationPath(derivationPath)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isSubstrateDerivationPathValid(derivationPath: String?): Boolean {
        return try {
            decodeSubstrateDerivationPath(derivationPath)
            true
        } catch (e: Exception) {
            false
        }
    }
}
