package io.novafoundation.nova.feature_account_api.data.secrets

import io.novafoundation.nova.common.data.secrets.v2.ChainAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.MetaAccountSecrets
import io.novafoundation.nova.core.model.CryptoType
import io.novasama.substrate_sdk_android.scale.EncodableStruct
import io.novasama.substrate_sdk_android.scale.Schema

interface AccountSecretsFactory {

    sealed class AccountSource {
        class Mnemonic(val cryptoType: CryptoType, val mnemonic: String) : AccountSource()

        class Seed(val cryptoType: CryptoType, val seed: String) : AccountSource()

        class Json(val json: String, val password: String) : AccountSource()
    }

    sealed class SecretsError : Exception() {

        class NotValidEthereumCryptoType : SecretsError()

        class NotValidSubstrateCryptoType : SecretsError()
    }

    data class Result<S : Schema<S>>(val secrets: EncodableStruct<S>, val cryptoType: CryptoType)

    suspend fun chainAccountSecrets(
        derivationPath: String?,
        accountSource: AccountSource,
        isEthereum: Boolean
    ): Result<ChainAccountSecrets>

    suspend fun metaAccountSecrets(
        substrateDerivationPath: String?,
        ethereumDerivationPath: String?,
        accountSource: AccountSource
    ): Result<MetaAccountSecrets>
}
