package io.novafoundation.nova.feature_account_impl.data.signer.proxy

import io.novafoundation.nova.common.utils.toCallInstance
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.runtime.extrinsic.signer.FeeSigner
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignedExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignedRaw
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadRaw
import java.math.BigInteger

class ProxiedFeeSignerFactory(
    private val accountRepository: AccountRepository
) {

    fun create(metaAccount: MetaAccount, chain: Chain, signerProvider: SignerProvider): ProxiedFeeSigner {
        return ProxiedFeeSigner(
            proxiedMetaAccount = metaAccount,
            chain = chain,
            signerProvider = signerProvider,
            accountRepository = accountRepository,
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
        return if (callInstance == null) {
            delegator.signExtrinsic(payloadExtrinsic)
        } else {
            val modifiedPayloadExtrinsic = payloadExtrinsic.wrapIntoProxyPayload(
                proxyAccountId = getProxyAccountId(),
                currentProxyNonce = BigInteger.ZERO,
                proxyType = ProxyType.Any,
                callInstance = callInstance
            )

            delegator.signExtrinsic(modifiedPayloadExtrinsic)
        }
    }

    override suspend fun signRaw(payload: SignerPayloadRaw): SignedRaw {
        return getDelegator().signRaw(payload)
    }

    override suspend fun actualFeeSignerId(chain: Chain): AccountId {
        return getDelegator().actualFeeSignerId(chain)
    }

    override suspend fun requestedFeeSignerId(chain: Chain): AccountId {
        return proxiedMetaAccount.requireAccountIdIn(chain)
    }

    override suspend fun signerAccountId(chain: Chain): AccountId {
        require(chain.id == this.chain.id) {
            "Signer was created for the different chain, expected ${this.chain.name}, got ${chain.name}"
        }

        return getDelegator().signerAccountId(chain)
    }

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

        return requireNotNull(proxyMetaAccount)
    }
}
