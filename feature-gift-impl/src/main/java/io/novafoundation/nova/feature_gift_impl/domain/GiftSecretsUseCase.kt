package io.novafoundation.nova.feature_gift_impl.domain

import io.novafoundation.nova.common.data.secrets.v2.ChainAccountSecrets
import io.novafoundation.nova.common.utils.normalizeSeed
import io.novafoundation.nova.feature_account_api.data.repository.CreateSecretsRepository
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.account.common.forChain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.encrypt.seed.SeedCreator
import io.novasama.substrate_sdk_android.scale.EncodableStruct

private const val GIFT_SEED_SIZE_BYTES = 10

class GiftSecretsUseCase(
    private val createSecretsRepository: CreateSecretsRepository,
    private val encryptionDefaults: EncryptionDefaults
) {

    suspend fun createGiftSecrets(chain: Chain, seed: ByteArray): EncodableStruct<ChainAccountSecrets> {
        val encryption = encryptionDefaults.forChain(chain)

        return createSecretsRepository.createSecretsWithSeed(
            seed = seed.normalizeSeed(),
            cryptoType = encryption.cryptoType,
            derivationPath = encryption.derivationPath,
            isEthereum = chain.isEthereumBased
        )
    }
}

suspend fun GiftSecretsUseCase.createRandomGiftSecrets(chain: Chain): EncodableStruct<ChainAccountSecrets> {
    val seed = SeedCreator.randomSeed(sizeBytes = GIFT_SEED_SIZE_BYTES)

    return createGiftSecrets(chain, seed)
}
