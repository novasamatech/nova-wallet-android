package io.novafoundation.nova.feature_account_impl.data.signer.secrets

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.getChainAccountKeypair
import io.novafoundation.nova.common.data.secrets.v2.getMetaAccountKeypair
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.multiChainEncryptionFor
import jp.co.soramitsu.fearless_utils.encrypt.MultiChainEncryption
import jp.co.soramitsu.fearless_utils.encrypt.SignatureWrapper
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.KeyPairSigner
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.Signer
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadRaw

class SecretsSignerFactory(
    private val secretStoreV2: SecretStoreV2
) {

    fun create(metaAccount: MetaAccount): SecretsSigner {
        return SecretsSigner(metaAccount, secretStoreV2)
    }
}

class SecretsSigner(
    private val metaAccount: MetaAccount,
    private val secretStoreV2: SecretStoreV2
) : Signer {

    override suspend fun signExtrinsic(payloadExtrinsic: SignerPayloadExtrinsic): SignatureWrapper {
        val delegate = createDelegate(payloadExtrinsic.accountId)

        return delegate.signExtrinsic(payloadExtrinsic)
    }

    override suspend fun signRaw(payload: SignerPayloadRaw): SignatureWrapper {
        val delegate = createDelegate(payload.accountId)

        return delegate.signRaw(payload)
    }

    private suspend fun createDelegate(accountId: AccountId): KeyPairSigner {
        val multiChainEncryption = metaAccount.multiChainEncryptionFor(accountId)!!

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
