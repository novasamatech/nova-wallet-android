package io.novafoundation.nova.feature_ledger_impl.domain.account.sign

import io.novafoundation.nova.common.utils.chainId
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.publicKeyIn
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.fearless_utils.encrypt.SignatureWrapper
import jp.co.soramitsu.fearless_utils.encrypt.Signer
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.genesisHash

interface SignLedgerInteractor {

    suspend fun verifySignature(payload: SignerPayloadExtrinsic, signature: SignatureWrapper): Boolean
}

class RealSignLedgerInteractor(
    private val metaAccountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
) : SignLedgerInteractor {

    override suspend fun verifySignature(payload: SignerPayloadExtrinsic, signature: SignatureWrapper): Boolean = runCatching {
        val metaAccount = metaAccountRepository.getSelectedMetaAccount()
        val chainId = payload.chainId
        val chain = chainRegistry.getChain(chainId)

        val publicKey = metaAccount.publicKeyIn(chain) ?: throw IllegalStateException("No public key for chain $chainId")

        Signer.verifyEd25519()
    }.getOrDefault(false)
}
