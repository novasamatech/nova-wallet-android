package io.novafoundation.nova.feature_account_impl.data.signer.proxy

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.utils.chainId
import io.novafoundation.nova.common.utils.toCallInstance
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxyExtrinsicValidationRequestBus
import io.novafoundation.nova.feature_account_api.data.repository.ProxyRepository
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount.ProxyType
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.proxy.ProxySigningPresenter
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxiedExtrinsicValidationFailure.ProxyNotEnoughFee
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxiedExtrinsicValidationPayload
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.extrinsic.signer.NovaSigner
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
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
    private val rpcCalls: RpcCalls,
    private val proxyExtrinsicValidationEventBus: ProxyExtrinsicValidationRequestBus
) {

    fun create(metaAccount: MetaAccount, signerProvider: SignerProvider): ProxiedSigner {
        return ProxiedSigner(
            proxiedMetaAccount = metaAccount,
            chainRegistry = chainRegistry,
            accountRepository = accountRepository,
            signerProvider = signerProvider,
            proxySigningPresenter = proxySigningPresenter,
            proxyRepository = proxyRepository,
            rpcCalls = rpcCalls,
            proxyExtrinsicValidationEventBus = proxyExtrinsicValidationEventBus
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
    private val proxyExtrinsicValidationEventBus: ProxyExtrinsicValidationRequestBus
) : NovaSigner {

    override suspend fun signerAccountId(chain: Chain): AccountId {
        val proxyMetaAccount = getProxyMetaAccount()
        val delegate = createDelegate(proxyMetaAccount)

        return delegate.signerAccountId(chain)
    }

    override suspend fun signExtrinsic(payloadExtrinsic: SignerPayloadExtrinsic): SignedExtrinsic {
        val chain = chainRegistry.getChain(payloadExtrinsic.chainId)
        val proxyMetaAccount = getProxyMetaAccount()

        acknowledgeProxyOperation(proxyMetaAccount)

        if (isRootProxiedSigner()) {
            validateExtrinsic(payloadExtrinsic, chain)
        }

        val delegate = createDelegate(proxyMetaAccount)
        val modifiedPayload = modifyPayload(proxyMetaAccount, payloadExtrinsic, chain)

        val signedExtrinsic = delegate.signExtrinsic(modifiedPayload)
        return signedExtrinsic
    }

    override suspend fun signRaw(payload: SignerPayloadRaw): SignedRaw {
        signingNotSupported()
    }

    private fun createDelegate(proxyMetaAccount: MetaAccount): NovaSigner {
        return signerProvider.signerFor(proxyMetaAccount)
    }

    private suspend fun validateExtrinsic(signedExtrinsic: SignerPayloadExtrinsic, chain: Chain) {
        val proxyAccount = accountRepository.getLastProxyAccountFor(proxiedMetaAccount.id) ?: throw IllegalStateException("Proxy account is not found")
        val proxyAccountId = proxyAccount.accountIdIn(chain) ?: throw IllegalStateException("Proxy account is not found")

        val callInstance = signedExtrinsic.call.toCallInstance() ?: signingNotSupported()

        val validationPayload = ProxiedExtrinsicValidationPayload(
            proxyAccount,
            proxyAccountId,
            ChainWithAsset(chain, chain.commissionAsset),
            callInstance.call
        )

        val requestBusPayload = ProxyExtrinsicValidationRequestBus.Request(validationPayload)
        val validationResponse = proxyExtrinsicValidationEventBus.handle(requestBusPayload)

        val validationStatus = validationResponse.validationResult.getOrNull()
        if (validationStatus is ValidationStatus.NotValid && validationStatus.reason is ProxyNotEnoughFee) {
            val reason = validationStatus.reason as ProxyNotEnoughFee
            proxySigningPresenter.notEnoughFee(reason.errorMessage)

            throw SigningCancelledException()
        }
    }

    private suspend fun modifyPayload(proxyMetaAccount: MetaAccount, payload: SignerPayloadExtrinsic, chain: Chain): SignerPayloadExtrinsic {
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

    private suspend fun isRootProxiedSigner(): Boolean {
        val selectedMetaAccount = accountRepository.getSelectedMetaAccount()
        return selectedMetaAccount.id == proxiedMetaAccount.id
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
