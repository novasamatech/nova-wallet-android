package io.novafoundation.nova.feature_staking_impl.data.bittensor.mev

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicBuildingContext
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.feature_account_api.data.signer.SigningContext
import io.novafoundation.nova.feature_account_api.domain.model.cryptoTypeIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev.BittensorMevTiming
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.InheritedImplication
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.ChargeTransactionPayment
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.CheckNonce.Companion.setNonce
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.verifySignature.GeneralTransactionSigner
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.verifySignature.VerifySignature.Companion.setVerifySignature
import io.novasama.substrate_sdk_android.runtime.extrinsic.call
import java.math.BigInteger

class BittensorMevFeeSignerFactory {

    fun create(type: CryptoType): GeneralTransactionSigner {
        val signature = when (type) {
            CryptoType.SR25519 -> SignatureWrapper.Sr25519(ByteArray(64))
            CryptoType.ED25519 -> SignatureWrapper.Ed25519(ByteArray(64))
            CryptoType.ECDSA -> SignatureWrapper.Ecdsa(ByteArray(1), ByteArray(32), ByteArray(32))
        }
        return BittensorMevFeeSigner(signature)
    }
}

private class BittensorMevFeeSigner(private val signature: SignatureWrapper) : GeneralTransactionSigner {

    override suspend fun signInheritedImplication(inheritedImplication: InheritedImplication, accountId: AccountId): SignatureWrapper = signature
}

class BittensorMevExtrinsics(
    private val builders: ExtrinsicBuilderFactory,
    private val rpc: RpcCalls,
    private val feeSigners: BittensorMevFeeSignerFactory
) {

    suspend fun build(
        chain: Chain,
        signer: NovaSigner,
        timing: BittensorMevTiming,
        nonce: BigInteger,
        estimate: Boolean,
        form: FormExtrinsicWithOrigin
    ): SendableExtrinsic {
        val accountId = signer.metaAccount.requireAccountIdIn(chain)
        val options = ExtrinsicBuilderFactory.Options(BatchMode.BATCH_ALL, timing.mortality)
        val builder = builders.create(chain, options)
        val origin = SubmissionOrigin(accountId, accountId)
        val context = ExtrinsicBuildingContext(origin, signer, chain)
        builder.form(context)
        builder.setTransactionExtension(ChargeTransactionPayment(BigInteger.ZERO))
        if (estimate) {
            val feeSigner = feeSigners.create(requireNotNull(signer.metaAccount.cryptoTypeIn(chain)))
            builder.setNonce(nonce)
            builder.setVerifySignature(feeSigner, accountId)
        } else {
            val signingContext = object : SigningContext {
                override val chain = chain

                override suspend fun getNonce(accountId: AccountIdKey): BigInteger = nonce
            }
            with(builder) { signer.setSignerDataForSubmission(signingContext) }
        }
        return builder.buildExtrinsic()
    }

    suspend fun fee(chain: Chain, signer: NovaSigner, extrinsic: SendableExtrinsic): Fee {
        val accountId = signer.metaAccount.requireAccountIdIn(chain)
        val response = rpc.getExtrinsicFee(chain, extrinsic)
        return SubstrateFee(response.partialFee, SubmissionOrigin(accountId, accountId), chain.utilityAsset)
    }

    fun carrier(ciphertext: ByteArray): FormExtrinsicWithOrigin = {
        // A single top-level call: wrapping it in utility/proxy prevents the block author from decrypting it.
        call("MevShield", "submit_encrypted", mapOf("ciphertext" to ciphertext))
    }
}
