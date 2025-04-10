package io.novafoundation.nova.feature_account_impl.data.signer.proxy

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.data.memory.SingleValueCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.getChainIdOrThrow
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxiedExtrinsicValidationFailure.ProxyNotEnoughFee
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxiedExtrinsicValidationPayload
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxyExtrinsicValidationRequestBus
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.presenatation.account.proxy.ProxySigningPresenter
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.feature_account_impl.domain.account.model.ProxiedMetaAccount
import io.novafoundation.nova.feature_account_api.data.signer.SigningContext
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw
import javax.inject.Inject

@FeatureScope
class ProxiedSignerFactory @Inject constructor(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val proxySigningPresenter: ProxySigningPresenter,
    private val getProxyRepository: GetProxyRepository,
    private val proxyExtrinsicValidationEventBus: ProxyExtrinsicValidationRequestBus,
    private val proxyCallFilterFactory: ProxyCallFilterFactory
) {

    fun create(metaAccount: ProxiedMetaAccount, signerProvider: SignerProvider, isRoot: Boolean): ProxiedSigner {
        return ProxiedSigner(
            proxiedMetaAccount = metaAccount,
            chainRegistry = chainRegistry,
            accountRepository = accountRepository,
            signerProvider = signerProvider,
            proxySigningPresenter = proxySigningPresenter,
            getProxyRepository = getProxyRepository,
            proxyExtrinsicValidationEventBus = proxyExtrinsicValidationEventBus,
            isRootProxied = isRoot,
            proxyCallFilterFactory = proxyCallFilterFactory,
            metaAccount = metaAccount
        )
    }
}

class ProxiedSigner(
    private val proxiedMetaAccount: MetaAccount,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val signerProvider: SignerProvider,
    private val proxySigningPresenter: ProxySigningPresenter,
    private val getProxyRepository: GetProxyRepository,
    private val proxyExtrinsicValidationEventBus: ProxyExtrinsicValidationRequestBus,
    private val isRootProxied: Boolean,
    private val proxyCallFilterFactory: ProxyCallFilterFactory,
    override val metaAccount: MetaAccount
) : NovaSigner {

    private val proxyMetaAccount = SingleValueCache {
        computeProxyMetaAccount()
    }

    private val delegateSigner = SingleValueCache {
        signerProvider.nestedSignerFor(proxyMetaAccount())
    }

    override suspend fun actualSignerAccountId(chain: Chain): AccountId {
        return delegateSigner().actualSignerAccountId(chain)
    }

    context(ExtrinsicBuilder)
    override suspend fun setSignerData(context: SigningContext) {
        wrapCallsInProxyForSubmission()
        delegateSigner().setSignerData(context)

        if (isRootProxied) {
            acknowledgeProxyOperation(proxyMetaAccount())
            validateExtrinsic(context.chain)
        }
    }

    context(ExtrinsicBuilder)
    override suspend fun setSignerDataForFee(context: SigningContext) {
        wrapCallsInProxyForFee()
        delegateSigner().setSignerDataForFee(context)
    }

    override suspend fun signRaw(payload: SignerPayloadRaw): SignedRaw {
        signingNotSupported()
    }

    override suspend fun maxCallsPerTransaction(): Int? {
        return delegateSigner().maxCallsPerTransaction()
    }

    context(ExtrinsicBuilder)
    private suspend fun validateExtrinsic(chain: Chain) {
        val proxyAccountId = actualSignerAccountId(chain)
        val proxyAccount = accountRepository.findMetaAccount(proxyAccountId, chain.id) ?: throw IllegalStateException("Proxy account is not found")

        val validationPayload = ProxiedExtrinsicValidationPayload(
            proxyMetaAccount = proxyAccount,
            proxyAccountId = proxyAccountId,
            chainWithAsset = ChainWithAsset(chain, chain.commissionAsset),
            call = getWrappedCall()
        )

        val requestBusPayload = ProxyExtrinsicValidationRequestBus.Request(validationPayload)
        val validationResponse = proxyExtrinsicValidationEventBus.handle(requestBusPayload)

        val validationStatus = validationResponse.validationResult.getOrNull()
        if (validationStatus is ValidationStatus.NotValid && validationStatus.reason is ProxyNotEnoughFee) {
            val reason = validationStatus.reason as ProxyNotEnoughFee
            proxySigningPresenter.notEnoughFee(reason.metaAccount, reason.asset, reason.availableBalance, reason.fee)

            throw SigningCancelledException()
        }
    }

    context(ExtrinsicBuilder)
    private suspend fun wrapCallsInProxyForSubmission() {
        val chainId = getChainIdOrThrow()
        val chain = chainRegistry.getChain(chainId)

        val call = getWrappedCall()

        val proxyAccountId = proxyMetaAccount().requireAccountIdIn(chain)
        val proxiedAccountId = proxiedMetaAccount.requireAccountIdIn(chain)

        val availableProxyTypes = getProxyRepository.getDelegatedProxyTypesRemote(
            chainId = chain.id,
            proxiedAccountId = proxiedAccountId,
            proxyAccountId = proxyAccountId
        )

        val proxyType = proxyCallFilterFactory.getFirstMatchedTypeOrNull(call, availableProxyTypes)
            ?: notEnoughPermission(proxyMetaAccount(), availableProxyTypes)

        return wrapCallsIntoProxy(
            proxiedAccountId = proxiedAccountId,
            proxyType = proxyType,
        )
    }

    // Wrap without verifying proxy permissions and hardcode proxy type
    // to speed up fee calculation
    context(ExtrinsicBuilder)
    private suspend fun wrapCallsInProxyForFee() {
        val chainId = getChainIdOrThrow()
        val chain = chainRegistry.getChain(chainId)

        val proxiedAccountId = proxiedMetaAccount.requireAccountIdIn(chain)

        return wrapCallsIntoProxy(
            proxiedAccountId = proxiedAccountId,
            proxyType = ProxyType.Any,
        )
    }

    private suspend fun acknowledgeProxyOperation(proxyMetaAccount: MetaAccount) {
        val resume = proxySigningPresenter.acknowledgeProxyOperation(proxiedMetaAccount, proxyMetaAccount)
        if (!resume) {
            throw SigningCancelledException()
        }
    }

    private suspend fun computeProxyMetaAccount(): MetaAccount {
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
