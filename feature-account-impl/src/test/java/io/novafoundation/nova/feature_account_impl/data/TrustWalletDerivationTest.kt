package io.novafoundation.nova.feature_account_impl.data

import io.novafoundation.nova.common.utils.substrateAccountId
import io.novasama.substrate_sdk_android.encrypt.keypair.bip32.Bip32Ed25519KeypairFactory
import io.novasama.substrate_sdk_android.encrypt.keypair.bip32.generate
import io.novasama.substrate_sdk_android.encrypt.seed.bip39.Bip39SeedFactory
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class TrustWalletDerivationTest {

    @Test
    fun `should derive substrate address from trust wallet`() {
        val mnemonic = "fine engage seed popular upon round differ belt engage space author pet"
        val expectedAccountId = "16PfWao1oeVQXAK6Qvj4owWVg49toh9ARbRmuCA3F2Gwxi3z".toAccountId()

        val seed = Bip39SeedFactory.deriveSeed(mnemonic, password = null)
        val keypair = Bip32Ed25519KeypairFactory.generate(seed.seed, "//44//354//0//0//0")

        val actualAccountId = keypair.publicKey.substrateAccountId()

        assertArrayEquals(expectedAccountId, actualAccountId)
    }
}
