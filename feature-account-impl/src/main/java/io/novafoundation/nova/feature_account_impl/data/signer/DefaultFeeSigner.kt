package io.novafoundation.nova.feature_account_impl.data.signer

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.extrinsic.signer.FeeSigner
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.encrypt.EncryptionType
import io.novasama.substrate_sdk_android.encrypt.MultiChainEncryption
import io.novasama.substrate_sdk_android.encrypt.keypair.Keypair
import io.novasama.substrate_sdk_android.encrypt.keypair.ethereum.EthereumKeypairFactory
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.SubstrateKeypairFactory
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.KeyPairSigner
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw

private val FAKE_CRYPTO_TYPE = EncryptionType.ECDSA

class DefaultFeeSigner(
    private val realMetaAccount: MetaAccount,
    private val chain: Chain
) : FeeSigner {

    private val fakeKeyPair = generateFakeKeyPair()

    override suspend fun signExtrinsic(payloadExtrinsic: SignerPayloadExtrinsic): SignedExtrinsic {
        val signer = KeyPairSigner(fakeKeyPair, multiChainEncryption())

        return signer.signExtrinsic(payloadExtrinsic)
    }

    override suspend fun signRaw(payload: SignerPayloadRaw): SignedRaw {
        val signer = KeyPairSigner(fakeKeyPair, multiChainEncryption())

        return signer.signRaw(payload)
    }

    override suspend fun maxCallsPerTransaction(): Int? {
        return null
    }

    override suspend fun actualFeeSignerId(chain: Chain): AccountId {
        return requestedFeeSignerId(chain)
    }

    override suspend fun requestedFeeSignerId(chain: Chain): AccountId {
        return realMetaAccount.requireAccountIdIn(chain)
    }

    override suspend fun signerAccountId(chain: Chain): ByteArray {
        require(chain.id == this.chain.id) {
            "Signer was created for the different chain, expected ${this.chain.name}, got ${chain.name}"
        }

        return chain.accountIdOf(fakeKeyPair.publicKey)
    }

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
