package io.novafoundation.nova.feature_account_impl.data.signer.proxy

import io.novafoundation.nova.common.utils.toCallInstance
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.runtime.extrinsic.feeSigner.FeeSigner
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignedExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignedRaw
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadRaw

class ProxiedFeeSignerFactory(
    private val accountRepository: AccountRepository
) {

    fun create(metaAccount: MetaAccount, chain: Chain, signerProvider: SignerProvider): ProxiedFeeSigner {
        return ProxiedFeeSigner(
            metaAccount,
            chain,
            signerProvider,
            accountRepository,
        )
    }
}

class ProxiedFeeSigner(
    private val proxiedMetaAccount: MetaAccount,
    private val chain: Chain,
    private val signerProvider: SignerProvider,
    private val accountRepository: AccountRepository,
) : FeeSigner {

    private var proxyMetaAccount: MetaAccount? = null
    private var delegate: FeeSigner? = null

    override suspend fun signExtrinsic(payloadExtrinsic: SignerPayloadExtrinsic): SignedExtrinsic {
        val delegator = getDelegator()

        val callInstance = payloadExtrinsic.call.toCallInstance()
        if (callInstance == null) {
            return delegator.signExtrinsic(payloadExtrinsic)
        } else {
            val modifienPayloadExtrinsic = payloadExtrinsic.wrapIntoProxyPayload(
                getProxyAccountId(),
                ProxyAccount.ProxyType.Any,
                callInstance
            )

            return delegator.signExtrinsic(modifienPayloadExtrinsic)
        }
    }

    override suspend fun signRaw(payload: SignerPayloadRaw): SignedRaw {
        return getDelegator().signRaw(payload)
    }

    override suspend fun accountId() = getDelegator().accountId()

    private suspend fun getProxyAccountId(): ByteArray {
        return getProxyMetaAccount().requireAccountIdIn(chain)
    }

    private suspend fun getDelegator(): FeeSigner {
        if (delegate == null) {
            delegate = signerProvider.feeSigner(getProxyMetaAccount(), chain)
        }

        return delegate!!
    }

    private suspend fun getProxyMetaAccount(): MetaAccount {
        if (proxyMetaAccount == null) {
            proxyMetaAccount = accountRepository.getMetaAccount(proxiedMetaAccount.proxy!!.metaId)
        }

        return proxyMetaAccount ?: throw IllegalStateException("Proxy meta account not found")
    }
}
