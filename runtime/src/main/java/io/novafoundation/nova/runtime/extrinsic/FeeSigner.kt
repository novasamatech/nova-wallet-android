package io.novafoundation.nova.runtime.extrinsic

import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.encrypt.EncryptionType
import jp.co.soramitsu.fearless_utils.encrypt.MultiChainEncryption
import jp.co.soramitsu.fearless_utils.encrypt.SignatureWrapper
import jp.co.soramitsu.fearless_utils.encrypt.keypair.Keypair
import jp.co.soramitsu.fearless_utils.encrypt.keypair.ethereum.EthereumKeypairFactory
import jp.co.soramitsu.fearless_utils.encrypt.keypair.substrate.SubstrateKeypairFactory
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.KeyPairSigner
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.Signer
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadRaw

private val FAKE_CRYPTO_TYPE = EncryptionType.ECDSA

class FeeSigner(private val chain: Chain) : Signer {

    private val keypair = generateFakeKeyPair()

    override suspend fun signExtrinsic(payloadExtrinsic: SignerPayloadExtrinsic): SignatureWrapper {
        val signer = KeyPairSigner(keypair, multiChainEncryption())

        return signer.signExtrinsic(payloadExtrinsic)
    }

    override suspend fun signRaw(payload: SignerPayloadRaw): SignatureWrapper {
        val signer = KeyPairSigner(keypair, multiChainEncryption())

        return signer.signRaw(payload)
    }

    fun accountId() = chain.accountIdOf(keypair.publicKey)

    private fun multiChainEncryption() = if (chain.isEthereumBased) {
        MultiChainEncryption.Ethereum
    } else {
        MultiChainEncryption.Substrate(FAKE_CRYPTO_TYPE)
    }

    private fun generateFakeKeyPair(): Keypair {
        return if (chain.isEthereumBased) {
            val emptySeed = ByteArray(64) { 1 }

            EthereumKeypairFactory.generate(emptySeed, junctions = emptyList())
        } else {
            val emptySeed = ByteArray(32) { 1 }

            SubstrateKeypairFactory.generate(FAKE_CRYPTO_TYPE, emptySeed, junctions = emptyList())
        }
    }
}
