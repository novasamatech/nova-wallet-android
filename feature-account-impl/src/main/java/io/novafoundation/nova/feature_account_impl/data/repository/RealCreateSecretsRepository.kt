package io.novafoundation.nova.feature_account_impl.data.repository

import io.novafoundation.nova.common.data.secrets.v2.ChainAccountSecrets
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.data.repository.CreateSecretsRepository
import io.novafoundation.nova.feature_account_impl.data.secrets.AccountSecretsFactory
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.scale.EncodableStruct

class RealCreateSecretsRepository(
    private val accountSecretsFactory: AccountSecretsFactory,
) : CreateSecretsRepository {
    override suspend fun createSecretsWithSeed(
        seed: ByteArray,
        cryptoType: CryptoType,
        derivationPath: String?,
        isEthereum: Boolean
    ): EncodableStruct<ChainAccountSecrets> {
        val seedString = seed.toHexString()
        return accountSecretsFactory.chainAccountSecrets(
            derivationPath,
            AccountSecretsFactory.AccountSource.Seed(cryptoType, seedString),
            isEthereum = isEthereum
        ).secrets
    }
}
