package io.novafoundation.nova.feature_account_api.data.repository

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.secrets.v2.ChainAccountSecrets
import io.novafoundation.nova.core.model.CryptoType
import io.novasama.substrate_sdk_android.scale.EncodableStruct

interface CreateSecretsRepository {

    suspend fun createSecretsWithSeed(
        seed: ByteArray,
        cryptoType: CryptoType,
        derivationPath: String?,
        isEthereum: Boolean,
    ): EncodableStruct<ChainAccountSecrets>
}
