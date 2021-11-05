package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam

import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ParachainMetadata
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam.MakeSignatureRequest
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam.MoonbeamApi
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam.makeSignature
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.PrivateCrowdloanSignatureProvider
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.PrivateCrowdloanSignatureProvider.Mode
import jp.co.soramitsu.fearless_utils.encrypt.EncryptionType
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.MultiSignature
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.prepareForEncoding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

class MoonbeamPrivateSignatureProvider(
    private val moonbeamApi: MoonbeamApi,
    private val httpExceptionHandler: HttpExceptionHandler,
) : PrivateCrowdloanSignatureProvider {

    override suspend fun provideSignature(
        chainMetadata: ParachainMetadata,
        previousContribution: BigInteger,
        newContribution: BigInteger,
        address: String,
        mode: Mode,
    ): Any = withContext(Dispatchers.Default) {
        when (mode) {
            Mode.FEE -> sr25519SignatureOf(ByteArray(64)) // sr25519 is 65 bytes
            Mode.SUBMIT -> {
                val request = MakeSignatureRequest(address, previousContribution.toString(), newContribution.toString())
                val response = httpExceptionHandler.wrap { moonbeamApi.makeSignature(chainMetadata, request) }

                sr25519SignatureOf(response.signature.fromHex())
            }
        }
    }

    private fun sr25519SignatureOf(bytes: ByteArray): Any {
        return MultiSignature(EncryptionType.SR25519, bytes).prepareForEncoding()
    }
}
