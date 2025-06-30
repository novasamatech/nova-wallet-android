package io.novafoundation.nova.feature_account_impl.data.signer

import android.util.Log
import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.feature_account_api.data.signer.SigningContext
import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy
import io.novafoundation.nova.feature_account_api.data.signer.TxModificationInfo
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdKeyIn
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.encrypt.EncryptionType
import io.novasama.substrate_sdk_android.encrypt.MultiChainEncryption
import io.novasama.substrate_sdk_android.encrypt.keypair.Keypair
import io.novasama.substrate_sdk_android.encrypt.keypair.ethereum.EthereumKeypairFactory
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.SubstrateKeypairFactory
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.KeyPairSigner
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.CheckNonce.Companion.setNonce
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.verifySignature.GeneralTransactionSigner
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.verifySignature.VerifySignature.Companion.setVerifySignature

private val FAKE_CRYPTO_TYPE = EncryptionType.ECDSA

/**
 * A basic implementation of [NovaSigner] that implements foundation for any signer that
 * does not delegate any of the [NovaSigner] methods to the nested signers
 */
abstract class LeafSigner(
    override val metaAccount: MetaAccount,
) : NovaSigner, GeneralTransactionSigner {

    override suspend fun getSigningHierarchy(): SubmissionHierarchy {
        return SubmissionHierarchy(metaAccount, callExecutionType())
    }

    // All leaf signers are immediate atm so we implement it here to reduce boilerplate
    // Feel free to move it down if some leaf signer is actually immediate
    override suspend fun callExecutionType(): CallExecutionType {
        return CallExecutionType.IMMEDIATE
    }

    context(ExtrinsicBuilder)
    override suspend fun setSignerDataForSubmission(context: SigningContext): TxModificationInfo {
        val accountId = metaAccount.requireAccountIdKeyIn(context.chain)
        setNonce(context.getNonce(accountId))

        Log.d("Signer", "${this::class.simpleName}: set real signature")

        setVerifySignature(signer = this, accountId = accountId.value)

        return TxModificationInfo.modifiedNothing()
    }

    context(ExtrinsicBuilder)
    override suspend fun setSignerDataForFee(context: SigningContext): TxModificationInfo{
        // We set it to 100 so we won't accidentally cause fee underestimation
        // Underestimation might happen because fee depends on the extrinsic length and encoding of zero is more compact
        setNonce(100.toBigInteger())

        val (signer, accountId) = createFeeSigner(context.chain)

        Log.d("Signer", "${this::class.simpleName}: set fake signature")

        setVerifySignature(signer, accountId)

        return TxModificationInfo.modifiedNothing()
    }

    override suspend fun submissionSignerAccountId(chain: Chain): AccountId {
        return metaAccount.requireAccountIdIn(chain)
    }

    context(ExtrinsicBuilder)
    private fun createFeeSigner(chain: Chain): Pair<KeyPairSigner, AccountId> {
        val keypair = generateFeeKeyPair(chain)
        val signer = KeyPairSigner(keypair, feeMultiChainEncryption(chain))

        return signer to chain.accountIdOf(keypair.publicKey)
    }

    private fun feeMultiChainEncryption(chain: Chain) = if (chain.isEthereumBased) {
        MultiChainEncryption.Ethereum
    } else {
        MultiChainEncryption.Substrate(FAKE_CRYPTO_TYPE)
    }

    context(ExtrinsicBuilder)
    private fun generateFeeKeyPair(chain: Chain): Keypair {
        return if (chain.isEthereumBased) {
            val emptySeed = ByteArray(64) { 1 }

            EthereumKeypairFactory.generate(emptySeed, junctions = emptyList())
        } else {
            val emptySeed = ByteArray(32) { 1 }

            SubstrateKeypairFactory.generate(FAKE_CRYPTO_TYPE, emptySeed, junctions = emptyList())
        }
    }
}
