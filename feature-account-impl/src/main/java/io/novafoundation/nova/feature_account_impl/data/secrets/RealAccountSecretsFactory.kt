package io.novafoundation.nova.feature_account_impl.data.secrets

import io.novafoundation.nova.common.data.mappers.mapCryptoTypeToEncryption
import io.novafoundation.nova.common.data.mappers.mapEncryptionToCryptoType
import io.novafoundation.nova.common.data.secrets.v2.ChainAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.MetaAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.mapKeypairStructToKeypair
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.deriveSeed32
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.data.derivationPath.DerivationPathDecoder
import io.novafoundation.nova.feature_account_api.data.secrets.AccountSecretsFactory
import io.novafoundation.nova.feature_account_api.data.secrets.AccountSecretsFactory.AccountSource
import io.novafoundation.nova.feature_account_api.data.secrets.AccountSecretsFactory.SecretsError
import io.novasama.substrate_sdk_android.encrypt.MultiChainEncryption
import io.novasama.substrate_sdk_android.encrypt.json.JsonSeedDecoder
import io.novasama.substrate_sdk_android.encrypt.junction.JunctionDecoder
import io.novasama.substrate_sdk_android.encrypt.keypair.ethereum.EthereumKeypairFactory
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.SubstrateKeypairFactory
import io.novasama.substrate_sdk_android.encrypt.mnemonic.MnemonicCreator
import io.novasama.substrate_sdk_android.encrypt.seed.SeedFactory
import io.novasama.substrate_sdk_android.encrypt.seed.ethereum.EthereumSeedFactory
import io.novasama.substrate_sdk_android.encrypt.seed.substrate.SubstrateSeedFactory
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.scale.EncodableStruct
import io.novasama.substrate_sdk_android.scale.Schema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealAccountSecretsFactory(
    private val jsonSeedDecoder: JsonSeedDecoder
) : AccountSecretsFactory {

    override suspend fun chainAccountSecrets(
        derivationPath: String?,
        accountSource: AccountSource,
        isEthereum: Boolean,
    ): AccountSecretsFactory.Result<ChainAccountSecrets> = withContext(Dispatchers.Default) {
        val mnemonicWords = accountSource.castOrNull<AccountSource.Mnemonic>()?.mnemonic
        val entropy = mnemonicWords?.let(MnemonicCreator::fromWords)?.entropy
        val decodedDerivationPath = decodeDerivationPath(derivationPath, ethereum = isEthereum)

        val decodedJson = accountSource.castOrNull<AccountSource.Json>()?.let { jsonSource ->
            jsonSeedDecoder.decode(jsonSource.json, jsonSource.password).also {
                // only allow Ethereum JSONs for ethereum chains
                if (isEthereum && it.multiChainEncryption != MultiChainEncryption.Ethereum) {
                    throw SecretsError.NotValidEthereumCryptoType()
                }

                // only allow Substrate JSONs for substrate chains
                if (!isEthereum && it.multiChainEncryption == MultiChainEncryption.Ethereum) {
                    throw SecretsError.NotValidSubstrateCryptoType()
                }
            }
        }

        val encryptionType = when (accountSource) {
            is AccountSource.Mnemonic -> mapCryptoTypeToEncryption(accountSource.cryptoType)
            is AccountSource.Seed -> mapCryptoTypeToEncryption(accountSource.cryptoType)
            is AccountSource.Json -> decodedJson!!.multiChainEncryption.encryptionType
        }

        val seed = when (accountSource) {
            is AccountSource.Mnemonic -> deriveSeed(accountSource.mnemonic, decodedDerivationPath?.password, ethereum = isEthereum).seed
            is AccountSource.Seed -> accountSource.seed.fromHex()
            is AccountSource.Json -> decodedJson!!.seed
        }

        val keypair = if (seed != null) {
            val junctions = decodedDerivationPath?.junctions.orEmpty()

            if (isEthereum) {
                EthereumKeypairFactory.generate(seed, junctions)
            } else {
                SubstrateKeypairFactory.generate(encryptionType, seed, junctions)
            }
        } else { // seed is null for some cases when importing with JSON
            decodedJson!!.keypair
        }

        val secrets = ChainAccountSecrets(
            keyPair = keypair,
            entropy = entropy,
            seed = seed,
            derivationPath = derivationPath,
        )

        AccountSecretsFactory.Result(secrets = secrets, cryptoType = mapEncryptionToCryptoType(encryptionType))
    }

    override suspend fun metaAccountSecrets(
        substrateDerivationPath: String?,
        ethereumDerivationPath: String?,
        accountSource: AccountSource,
    ): AccountSecretsFactory.Result<MetaAccountSecrets> = withContext(Dispatchers.Default) {
        val (substrateSecrets, substrateCryptoType) = chainAccountSecrets(
            derivationPath = substrateDerivationPath,
            accountSource = accountSource,
            isEthereum = false
        )

        val ethereumKeypair = accountSource.castOrNull<AccountSource.Mnemonic>()?.let {
            val decodedEthereumDerivationPath = decodeDerivationPath(ethereumDerivationPath, ethereum = true)

            val seed = deriveSeed(it.mnemonic, password = decodedEthereumDerivationPath?.password, ethereum = true).seed

            EthereumKeypairFactory.generate(seed = seed, junctions = decodedEthereumDerivationPath?.junctions.orEmpty())
        }

        val secrets = MetaAccountSecrets(
            entropy = substrateSecrets[ChainAccountSecrets.Entropy],
            seed = substrateSecrets[ChainAccountSecrets.Seed],
            substrateKeyPair = mapKeypairStructToKeypair(substrateSecrets[ChainAccountSecrets.Keypair]),
            substrateDerivationPath = substrateDerivationPath,
            ethereumKeypair = ethereumKeypair,
            ethereumDerivationPath = ethereumDerivationPath
        )

        AccountSecretsFactory.Result(secrets = secrets, cryptoType = substrateCryptoType)
    }

    private fun deriveSeed(mnemonic: String, password: String?, ethereum: Boolean): SeedFactory.Result {
        return if (ethereum) {
            EthereumSeedFactory.deriveSeed(mnemonic, password)
        } else {
            SubstrateSeedFactory.deriveSeed32(mnemonic, password)
        }
    }

    private fun decodeDerivationPath(derivationPath: String?, ethereum: Boolean): JunctionDecoder.DecodeResult? {
        return when {
            ethereum -> DerivationPathDecoder.decodeEthereumDerivationPath(derivationPath)
            else -> DerivationPathDecoder.decodeSubstrateDerivationPath(derivationPath)
        }
    }
}
