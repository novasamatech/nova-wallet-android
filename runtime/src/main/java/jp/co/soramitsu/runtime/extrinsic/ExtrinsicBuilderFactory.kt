package jp.co.soramitsu.runtime.extrinsic

import jp.co.soramitsu.common.data.mappers.mapCryptoTypeToEncryption
import jp.co.soramitsu.common.data.network.runtime.binding.bindMultiAddress
import jp.co.soramitsu.core.model.CryptoType
import jp.co.soramitsu.fearless_utils.encrypt.keypair.Keypair
import jp.co.soramitsu.fearless_utils.encrypt.keypair.ethereum.EthereumKeypairFactory
import jp.co.soramitsu.fearless_utils.encrypt.keypair.substrate.SubstrateKeypairFactory
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHex
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Extrinsic
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.instances.AddressInstanceConstructor
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import jp.co.soramitsu.runtime.ext.accountIdFromPublicKey
import jp.co.soramitsu.runtime.ext.addressFromPublicKey
import jp.co.soramitsu.runtime.ext.addressOf
import jp.co.soramitsu.runtime.ext.genesisHash
import jp.co.soramitsu.runtime.ext.multiAddressOf
import jp.co.soramitsu.runtime.ext.signatureHashing
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.runtime.network.rpc.RpcCalls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val FAKE_CRYPTO_TYPE = CryptoType.ECDSA

class ExtrinsicBuilderFactory(
    private val rpcCalls: RpcCalls,
    private val chainRegistry: ChainRegistry,
    private val mortalityConstructor: MortalityConstructor,
) {

    /**
     * Create with fake keypair
     * Should be primarily used for fee calculation
     */
    suspend fun create(
        chain: Chain,
    ) = create(chain, generateFakeKeyPair(chain), FAKE_CRYPTO_TYPE)

    /**
     * Create with real keypair
     */
    suspend fun create(
        chain: Chain,
        keypair: Keypair,
        cryptoType: CryptoType,
    ): ExtrinsicBuilder {
        val runtime = chainRegistry.getRuntime(chain.id)

        val accountId = chain.accountIdFromPublicKey(keypair.publicKey)
        val accountAddress = chain.addressOf(accountId)

        val nonce = rpcCalls.getNonce(chain.id, accountAddress)
        val runtimeVersion = rpcCalls.getRuntimeVersion(chain.id)
        val mortality = mortalityConstructor.constructMortality(chain.id)

        return ExtrinsicBuilder(
            runtime = runtime,
            keypair = keypair,
            nonce = nonce,
            runtimeVersion = runtimeVersion,
            genesisHash = chain.genesisHash.fromHex(),
            blockHash = mortality.blockHash.fromHex(),
            era = mortality.era,
            encryptionType = mapCryptoTypeToEncryption(cryptoType),
            accountIdentifier = AddressInstanceConstructor.constructInstance(runtime.typeRegistry, accountId),
            signatureHashing = chain.signatureHashing
        )
    }

    private suspend fun generateFakeKeyPair(chain: Chain) = withContext(Dispatchers.Default) {
        if (chain.isEthereumBased) {
            val emptySeed = ByteArray(64) { 1 }

            EthereumKeypairFactory.generate(emptySeed, junctions = emptyList())
        } else {
            val cryptoType = mapCryptoTypeToEncryption(FAKE_CRYPTO_TYPE)
            val emptySeed = ByteArray(32) { 1 }

            SubstrateKeypairFactory.generate(cryptoType, emptySeed, junctions = emptyList())
        }
    }
}
