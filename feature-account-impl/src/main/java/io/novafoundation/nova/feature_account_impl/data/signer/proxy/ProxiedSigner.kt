package io.novafoundation.nova.feature_account_impl.data.signer.proxy

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.utils.chainId
import io.novafoundation.nova.common.utils.toCallInstance
import io.novafoundation.nova.feature_account_api.data.repository.ProxyRepository
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount.ProxyType
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.proxy.ProxySigningPresenter
import io.novafoundation.nova.runtime.extrinsic.signer.NovaSigner
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignedExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignedRaw
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadRaw

class ProxiedSignerFactory(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val proxySigningPresenter: ProxySigningPresenter,
    private val proxyRepository: ProxyRepository,
    private val rpcCalls: RpcCalls
) {

    fun create(metaAccount: MetaAccount, signerProvider: SignerProvider): ProxiedSigner {
        return ProxiedSigner(
            proxiedMetaAccount = metaAccount,
            chainRegistry = chainRegistry,
            accountRepository = accountRepository,
            signerProvider = signerProvider,
            proxySigningPresenter = proxySigningPresenter,
            proxyRepository = proxyRepository,
            rpcCalls = rpcCalls
        )
    }
}

class ProxiedSigner(
    private val proxiedMetaAccount: MetaAccount,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val signerProvider: SignerProvider,
    private val proxySigningPresenter: ProxySigningPresenter,
    private val proxyRepository: ProxyRepository,
    private val rpcCalls: RpcCalls,
) : NovaSigner {

    override suspend fun signerAccountId(chain: Chain): AccountId {
        val proxyMetaAccount = getProxyMetaAccount()
        val delegate = createDelegate(proxyMetaAccount)

        return delegate.signerAccountId(chain)
    }

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

    private fun createDelegate(proxyMetaAccount: MetaAccount): NovaSigner {
        return signerProvider.signerFor(proxyMetaAccount)
    }

    private suspend fun modifyPayload(proxyMetaAccount: MetaAccount, payload: SignerPayloadExtrinsic): SignerPayloadExtrinsic {
        val chain = chainRegistry.getChain(payload.chainId)

        val proxyAccountId = proxyMetaAccount.requireAccountIdIn(chain)
        val proxiedAccountId = proxiedMetaAccount.requireAccountIdIn(chain)

        val availableProxyTypes = proxyRepository.getDelegatedProxyTypes(
            chainId = payload.chainId,
            proxiedAccountId = proxiedAccountId,
            proxyAccountId = proxyAccountId
        )

        val callInstance = payload.call.toCallInstance() ?: signingNotSupported()
        val module = callInstance.call.module

        val proxyType = module.toProxyTypeMatcher()
            .matchToProxyTypes(availableProxyTypes)
            ?: notEnoughPermission(proxyMetaAccount, availableProxyTypes)

        val proxyAddress = proxyMetaAccount.requireAddressIn(chain)
        val nonce = rpcCalls.getNonce(payload.chainId, proxyAddress)

        return payload.wrapIntoProxyPayload(
            proxyAccountId = proxyAccountId,
            proxyType = proxyType,
            callInstance = callInstance,
            currentProxyNonce = nonce
        )
    }

    private suspend fun acknowledgeProxyOperation(proxyMetaAccount: MetaAccount) {
        val resume = proxySigningPresenter.acknowledgeProxyOperation(proxiedMetaAccount, proxyMetaAccount)
        if (!resume) {
            throw SigningCancelledException()
        }
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
