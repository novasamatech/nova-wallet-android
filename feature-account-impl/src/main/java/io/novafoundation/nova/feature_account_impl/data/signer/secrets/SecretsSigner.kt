package io.novafoundation.nova.feature_account_impl.data.signer.secrets

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.getChainAccountKeypair
import io.novafoundation.nova.common.data.secrets.v2.getMetaAccountKeypair
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationResult
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationService
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.multiChainEncryptionFor
import io.novafoundation.nova.feature_account_impl.data.signer.LeafSigner
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import io.novasama.substrate_sdk_android.encrypt.MultiChainEncryption
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.KeyPairSigner
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw

class SecretsSignerFactory(
    private val secretStoreV2: SecretStoreV2,
    private val chainRegistry: ChainRegistry,
    private val twoFactorVerificationService: TwoFactorVerificationService
) {

    fun create(metaAccount: MetaAccount): SecretsSigner {
        return SecretsSigner(
            metaAccount = metaAccount,
            secretStoreV2 = secretStoreV2,
            chainRegistry = chainRegistry,
            twoFactorVerificationService = twoFactorVerificationService
        )
    }
}

class SecretsSigner(
    private val metaAccount: MetaAccount,
    private val secretStoreV2: SecretStoreV2,
    private val chainRegistry: ChainRegistry,
    private val twoFactorVerificationService: TwoFactorVerificationService,
) : LeafSigner(metaAccount) {

    override suspend fun signExtrinsic(payloadExtrinsic: SignerPayloadExtrinsic): SignedExtrinsic {
        runTwoFactorVerificationIfEnabled()

        val delegate = createDelegate(payloadExtrinsic.accountId)
        return delegate.signExtrinsic(payloadExtrinsic)
    }

    override suspend fun signRaw(payload: SignerPayloadRaw): SignedRaw {
        runTwoFactorVerificationIfEnabled()

        val delegate = createDelegate(payload.accountId)
        return delegate.signRaw(payload)
    }

    private suspend fun runTwoFactorVerificationIfEnabled() {
        if (twoFactorVerificationService.isEnabled()) {
            val confirmationResult = twoFactorVerificationService.requestConfirmationIfEnabled()
            if (confirmationResult != TwoFactorVerificationResult.CONFIRMED) {
                throw SigningCancelledException()
            }
        }
    }

    private suspend fun createDelegate(accountId: AccountId): KeyPairSigner {
        val chainsById = chainRegistry.chainsById()
        val multiChainEncryption = metaAccount.multiChainEncryptionFor(accountId, chainsById)!!

        val keypair = secretStoreV2.getKeypair(
            metaAccount = metaAccount,
            accountId = accountId,
            isEthereumBased = multiChainEncryption is MultiChainEncryption.Ethereum
        )

        return KeyPairSigner(keypair, multiChainEncryption)
    }

    private suspend fun SecretStoreV2.getKeypair(
        metaAccount: MetaAccount,
        accountId: AccountId,
        isEthereumBased: Boolean
    ) = if (hasChainSecrets(metaAccount.id, accountId)) {
        getChainAccountKeypair(metaAccount.id, accountId)
    } else {
        getMetaAccountKeypair(metaAccount.id, isEthereumBased)
    }
}
