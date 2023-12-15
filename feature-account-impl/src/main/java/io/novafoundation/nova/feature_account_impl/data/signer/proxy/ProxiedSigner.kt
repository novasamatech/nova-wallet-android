package io.novafoundation.nova.feature_account_impl.data.signer.proxy

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.utils.chainId
import io.novafoundation.nova.common.utils.toCallInstance
import io.novafoundation.nova.feature_account_api.data.repository.ProxyRepository
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount.ProxyType
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.presenatation.account.proxy.ProxySigningPresenter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignedExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignedRaw
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.Signer
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadRaw

class ProxiedSignerFactory(
    private val secretStoreV2: SecretStoreV2,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val proxySigningPresenter: ProxySigningPresenter,
    private val proxyRepository: ProxyRepository
) {

    fun create(metaAccount: MetaAccount, signerProvider: SignerProvider): ProxiedSigner {
        return ProxiedSigner(
            metaAccount,
            chainRegistry,
            accountRepository,
            signerProvider,
            proxySigningPresenter,
            proxyRepository
        )
    }
}

class ProxiedSigner(
    private val proxiedMetaAccount: MetaAccount,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val signerProvider: SignerProvider,
    private val proxySigningPresenter: ProxySigningPresenter,
    private val proxyRepository: ProxyRepository
) : Signer {

    override suspend fun signExtrinsic(payloadExtrinsic: SignerPayloadExtrinsic): SignedExtrinsic {
        val proxyMetaAccount = getProxyMetaAccount()

        acknowledgeProxyOperation(proxyMetaAccount)

        val delegate = createDelegate(proxyMetaAccount)
        val modifiedPayload = modifyPayload(proxyMetaAccount, payloadExtrinsic)

        return delegate.signExtrinsic(modifiedPayload)
    }

    override suspend fun signRaw(payload: SignerPayloadRaw): SignedRaw {
        signingNotSupported()
    }

    private suspend fun createDelegate(proxyMetaAccount: MetaAccount): Signer {
        return signerProvider.signerFor(proxyMetaAccount)
    }

    private suspend fun modifyPayload(proxyMetaAccount: MetaAccount, payload: SignerPayloadExtrinsic): SignerPayloadExtrinsic {
        val availableProxyTypes = proxyRepository.getDelegatedProxyTypes(
            payload.chainId,
            proxiedMetaAccount.getAccountId(payload.chainId),
            proxyMetaAccount.getAccountId(payload.chainId)
        )

        val callInstance = payload.call.toCallInstance() ?: signingNotSupported()
        val module = callInstance.call.module
        val proxyType = module.toProxyTypeMatcher()
            .matchToProxyTypes(availableProxyTypes)
            ?: notEnoughPermission(proxyMetaAccount, availableProxyTypes)

        return payload.wrapIntoProxyPayload(proxyMetaAccount.getAccountId(payload.chainId), proxyType, callInstance)
    }

    private suspend fun acknowledgeProxyOperation(proxyMetaAccount: MetaAccount) {
        val resume = proxySigningPresenter.acknowledgeProxyOperation(proxiedMetaAccount, proxyMetaAccount)
        if (!resume) {
            throw SigningCancelledException()
        }
    }

    private suspend fun MetaAccount.getAccountId(chainId: ChainId): ByteArray {
        val chain = chainRegistry.getChain(chainId)
        return requireAccountIdIn(chain)
    }

    private suspend fun getProxyMetaAccount(): MetaAccount {
        val proxyAccount = proxiedMetaAccount.proxy ?: throw IllegalStateException("Proxy account is not found")
        return accountRepository.getMetaAccount(proxyAccount.metaId)
    }

    private suspend fun notEnoughPermission(proxyMetaAccount: MetaAccount, availableProxyTypes: List<ProxyType>): Nothing {
        proxySigningPresenter.notEnoughPermission(proxiedMetaAccount, proxyMetaAccount, availableProxyTypes)
        throw SigningCancelledException()
    }

    private suspend fun signingNotSupported(): Nothing {
        proxySigningPresenter.signingIsNotSupported()
        throw SigningCancelledException()
    }
}
