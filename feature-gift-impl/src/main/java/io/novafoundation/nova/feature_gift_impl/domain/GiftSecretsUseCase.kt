package io.novafoundation.nova.feature_gift_impl.domain

import io.novafoundation.nova.common.data.secrets.v2.ChainAccountSecrets
import io.novafoundation.nova.feature_account_api.data.repository.CreateSecretsRepository
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.account.common.forChain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.encrypt.seed.SeedCreator
import io.novasama.substrate_sdk_android.scale.EncodableStruct
import org.bouncycastle.crypto.generators.SCrypt

private const val GIFT_SEED_SIZE_BYTES = 10

class GiftSecretsUseCase(
    private val createSecretsRepository: CreateSecretsRepository,
    private val encryptionDefaults: EncryptionDefaults
) {

    companion object {
        private const val GIFT_SALT = "gift"
        private const val SCRYPT_KEY_SIZE = 32
        private const val N = 16384
        private const val p = 1
        private const val r = 8
    }

    suspend fun createGiftSecrets(chain: Chain, seed: ByteArray): EncodableStruct<ChainAccountSecrets> {
        val encryption = encryptionDefaults.forChain(chain)

        return createSecretsRepository.createSecretsWithSeed(
            seed = getGiftSeedHash(seed),
            cryptoType = encryption.cryptoType,
            derivationPath = encryption.derivationPath,
            isEthereum = chain.isEthereumBased
        )
    }

    fun createRandomGiftSeed(): ByteArray {
        return SeedCreator.randomSeed(sizeBytes = GIFT_SEED_SIZE_BYTES)
    }

    private fun getGiftSeedHash(seed: ByteArray): ByteArray {
        val saltBytes = GIFT_SALT.toByteArray()
        return SCrypt.generate(seed, saltBytes, N, r, p, SCRYPT_KEY_SIZE)
    }
}
