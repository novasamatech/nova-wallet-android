package io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev

import com.google.gson.JsonParser
import io.novafoundation.nova.feature_staking_impl.data.bittensor.mev.BittensorMevProtocol
import io.novafoundation.nova.feature_staking_impl.data.bittensor.mev.BittensorMevSealer
import io.novasama.substrate_sdk_android.extensions.fromHex
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.SecureRandom

class BittensorMevSealerTest {

    @Test
    fun `sealed payload matches every byte of SDK and iOS vectors`() {
        val source = requireNotNull(javaClass.getResourceAsStream("/mev-shield-vector.json"))
        val vector = source.bufferedReader().use { JsonParser.parseReader(it).asJsonObject }
        val publicKey = vector["ek"].asString.fromHex()
        for (sample in vector["cases"].asJsonArray) {
            val fields = sample.asJsonObject
            val draws = listOf(fields["m"].asString.fromHex(), fields["nonce"].asString.fromHex())
            val randomness = ScriptedRandom(draws)
            val sealer = BittensorMevSealer(randomness)
            val payload = sealer.seal(fields["plaintext"].asString.fromHex(), publicKey)
            assertArrayEquals(fields["name"].asString, fields["payload"].asString.fromHex(), payload)
            assertEquals(listOf(32, 24), randomness.sizes)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `oversized plaintext is rejected before drawing randomness`() {
        val randomness = ScriptedRandom(emptyList())
        val sealer = BittensorMevSealer(randomness)
        sealer.seal(ByteArray(7047), ByteArray(BittensorMevProtocol.PUBLIC_KEY_BYTES))
    }

    @Test
    fun `maximum inner fits the runtime bounded vector exactly`() {
        assertEquals(8192, BittensorMevProtocol.payloadSize(7046))
    }

    private class ScriptedRandom(private val draws: List<ByteArray>) : SecureRandom() {

        val sizes = mutableListOf<Int>()

        override fun nextBytes(bytes: ByteArray) {
            val draw = draws[sizes.size]
            assertEquals(draw.size, bytes.size)
            sizes += bytes.size
            draw.copyInto(bytes)
        }
    }
}
