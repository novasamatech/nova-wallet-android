package io.novafoundation.nova.feature_account_impl.data.secrets

import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.common.data.secrets.v2.MetaAccountSecrets
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.DEFAULT_DERIVATION_PATH
import io.novafoundation.nova.core.model.CryptoType
import io.novasama.substrate_sdk_android.encrypt.junction.BIP32JunctionDecoder
import io.novasama.substrate_sdk_android.encrypt.keypair.Keypair
import io.novasama.substrate_sdk_android.encrypt.keypair.bip32.Bip32EcdsaKeypairFactory
import io.novasama.substrate_sdk_android.encrypt.keypair.bip32.Bip32Ed25519KeypairFactory
import io.novasama.substrate_sdk_android.encrypt.keypair.bip32.generate
import io.novasama.substrate_sdk_android.encrypt.seed.SeedFactory
import io.novasama.substrate_sdk_android.encrypt.seed.bip39.Bip39SeedFactory
import io.novasama.substrate_sdk_android.scale.EncodableStruct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@FeatureScope
class TrustWalletSecretsFactory @Inject constructor() {

    companion object {

        private const val TRUST_SUBSTRATE_DERIVATION_PATH = "//44//354//0//0//0"
    }

    suspend fun metaAccountSecrets(mnemonicWords: String): TrustWalletMetaAccountSecrets = withContext(Dispatchers.Default) {
        val seedResult = deriveSeed(mnemonicWords)

        val secrets = MetaAccountSecrets(
            entropy = seedResult.mnemonic.entropy,
            substrateDerivationPath = TRUST_SUBSTRATE_DERIVATION_PATH,
            substrateSeed = seedResult.seed,
            substrateKeyPair = deriveKeypair(seedResult, AddressScheme.SUBSTRATE),
            ethereumKeypair = deriveKeypair(seedResult, AddressScheme.EVM),
            ethereumDerivationPath = BIP32JunctionDecoder.DEFAULT_DERIVATION_PATH
        )

        TrustWalletMetaAccountSecrets(secrets, substrateCryptoType = CryptoType.ED25519)
    }

    private fun deriveSeed(mnemonic: String): SeedFactory.Result {
        return Bip39SeedFactory.deriveSeed(mnemonic, password = null)
    }

    private fun deriveKeypair(seedResult: SeedFactory.Result, addressScheme: AddressScheme): Keypair {
        val derivationPath = getDerivationPath(addressScheme)

        return when (addressScheme) {
            AddressScheme.EVM -> Bip32EcdsaKeypairFactory.generate(seedResult.seed, derivationPath)
            AddressScheme.SUBSTRATE -> Bip32Ed25519KeypairFactory.generate(seedResult.seed, derivationPath)
        }
    }

    private fun getDerivationPath(addressScheme: AddressScheme): String {
        return when (addressScheme) {
            AddressScheme.EVM -> BIP32JunctionDecoder.DEFAULT_DERIVATION_PATH
            AddressScheme.SUBSTRATE -> TRUST_SUBSTRATE_DERIVATION_PATH
        }
    }

    data class TrustWalletMetaAccountSecrets(val secrets: EncodableStruct<MetaAccountSecrets>, val substrateCryptoType: CryptoType)
}
