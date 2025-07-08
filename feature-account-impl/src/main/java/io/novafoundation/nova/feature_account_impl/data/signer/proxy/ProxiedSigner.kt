package io.novafoundation.nova.feature_account_impl.data.signer.proxy

import android.util.Log
import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.data.memory.SingleValueCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxiedExtrinsicValidationFailure.ProxyNotEnoughFee
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxiedExtrinsicValidationPayload
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxyExtrinsicValidationRequestBus
import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.data.signer.SigningContext
import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy
import io.novafoundation.nova.feature_account_api.data.signer.intersect
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxiedMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_api.presenatation.account.proxy.ProxySigningPresenter
import io.novafoundation.nova.feature_account_impl.data.signer.LeafSigner
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.definitions.types.instances.AddressInstanceConstructor
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw
import javax.inject.Inject

@FeatureScope
class ProxiedSignerFactory @Inject constructor(
    private val accountRepository: AccountRepository,
    private val proxySigningPresenter: ProxySigningPresenter,
    private val getProxyRepository: GetProxyRepository,
    private val proxyExtrinsicValidationEventBus: ProxyExtrinsicValidationRequestBus,
    private val proxyCallFilterFactory: ProxyCallFilterFactory
) {

    fun create(metaAccount: ProxiedMetaAccount, signerProvider: SignerProvider, isRoot: Boolean): ProxiedSigner {
        return ProxiedSigner(
            accountRepository = accountRepository,
            signerProvider = signerProvider,
            proxySigningPresenter = proxySigningPresenter,
            getProxyRepository = getProxyRepository,
            proxyExtrinsicValidationEventBus = proxyExtrinsicValidationEventBus,
            isRootSigner = isRoot,
            proxyCallFilterFactory = proxyCallFilterFactory,
            proxiedMetaAccount = metaAccount
        )
    }
}

class ProxiedSigner(
    private val proxiedMetaAccount: ProxiedMetaAccount,
    private val accountRepository: AccountRepository,
    private val signerProvider: SignerProvider,
    private val proxySigningPresenter: ProxySigningPresenter,
    private val getProxyRepository: GetProxyRepository,
    private val proxyExtrinsicValidationEventBus: ProxyExtrinsicValidationRequestBus,
    private val isRootSigner: Boolean,
    private val proxyCallFilterFactory: ProxyCallFilterFactory,
) : NovaSigner {

    override val metaAccount = proxiedMetaAccount

    private val selfCallExecutionType = CallExecutionType.IMMEDIATE

    private val proxyMetaAccount = SingleValueCache {
        computeProxyMetaAccount()
    }

    private val delegateSigner = SingleValueCache {
        signerProvider.nestedSignerFor(proxyMetaAccount())
    }

    override suspend fun getSigningHierarchy(): SubmissionHierarchy {
        return delegateSigner().getSigningHierarchy() + SubmissionHierarchy(metaAccount, selfCallExecutionType)
    }

    override suspend fun submissionSignerAccountId(chain: Chain): AccountId {
        return delegateSigner().submissionSignerAccountId(chain)
    }

    override suspend fun callExecutionType(): CallExecutionType {
        return delegateSigner().callExecutionType().intersect(selfCallExecutionType)
    }

    context(ExtrinsicBuilder)
    override suspend fun setSignerDataForSubmission(context: SigningContext) {
        if (isRootSigner) {
            acknowledgeProxyOperation(proxyMetaAccount())
        }

        val proxiedCall = getWrappedCall()

        validateExtrinsic(context.chain, proxyMetaAccount = proxyMetaAccount(), proxiedCall = proxiedCall)

        wrapCallsInProxyForSubmission(context.chain, proxiedCall = proxiedCall)

        Log.d("Signer", "ProxiedSigner: wrapped proxy calls for submission")

        delegateSigner().setSignerDataForSubmission(context)
    }

    context(ExtrinsicBuilder)
    override suspend fun setSignerDataForFee(context: SigningContext) {
        val proxiedCall = getWrappedCall()

        wrapCallsInProxyForFee(context.chain, proxiedCall = proxiedCall)

        Log.d("Signer", "ProxiedSigner: wrapped proxy calls for fee")

        delegateSigner().setSignerDataForFee(context)
    }

    override suspend fun signRaw(payload: SignerPayloadRaw): SignedRaw {
        signingNotSupported()
    }

    override suspend fun maxCallsPerTransaction(): Int? {
        return delegateSigner().maxCallsPerTransaction()
    }

    context(ExtrinsicBuilder)
    private suspend fun validateExtrinsic(
        chain: Chain,
        proxyMetaAccount: MetaAccount,
        proxiedCall: GenericCall.Instance,
    ) {
        if (!proxyPaysFees()) return

        val validationPayload = ProxiedExtrinsicValidationPayload(
            proxiedMetaAccount = proxiedMetaAccount,
            proxyMetaAccount = proxyMetaAccount,
            chainWithAsset = ChainWithAsset(chain, chain.commissionAsset),
            proxiedCall = proxiedCall
        )

        val requestBusPayload = ProxyExtrinsicValidationRequestBus.Request(validationPayload)
        val validationResponse = proxyExtrinsicValidationEventBus.handle(requestBusPayload)

        val validationStatus = validationResponse.validationResult.getOrNull()
        if (validationStatus is ValidationStatus.NotValid && validationStatus.reason is ProxyNotEnoughFee) {
            val reason = validationStatus.reason as ProxyNotEnoughFee
            proxySigningPresenter.notEnoughFee(reason.proxy, reason.asset, reason.availableBalance, reason.fee)

            throw SigningCancelledException()
        }
    }

    context(ExtrinsicBuilder)
    private suspend fun wrapCallsInProxyForSubmission(chain: Chain, proxiedCall: GenericCall.Instance) {
        val proxyAccountId = proxyMetaAccount().requireAccountIdIn(chain)
        val proxiedAccountId = proxiedMetaAccount.requireAccountIdIn(chain)

        val availableProxyTypes = getProxyRepository.getDelegatedProxyTypesRemote(
            chainId = chain.id,
            proxiedAccountId = proxiedAccountId,
            proxyAccountId = proxyAccountId
        )

        val proxyType = proxyCallFilterFactory.getFirstMatchedTypeOrNull(proxiedCall, availableProxyTypes)
            ?: notEnoughPermission(proxyMetaAccount(), availableProxyTypes)

        return wrapCallsIntoProxy(
            proxiedAccountId = proxiedAccountId,
            proxyType = proxyType,
            proxiedCall = proxiedCall
        )
    }

    // Wrap without verifying proxy permissions and hardcode proxy type
    // to speed up fee calculation
    context(ExtrinsicBuilder)
    private fun wrapCallsInProxyForFee(chain: Chain, proxiedCall: GenericCall.Instance) {
        val proxiedAccountId = proxiedMetaAccount.requireAccountIdIn(chain)

        return wrapCallsIntoProxy(
            proxiedAccountId = proxiedAccountId,
            proxyType = ProxyType.Any,
            proxiedCall = proxiedCall
        )
    }

    context(ExtrinsicBuilder)
    private fun wrapCallsIntoProxy(
        proxiedAccountId: AccountId,
        proxyType: ProxyType,
        proxiedCall: GenericCall.Instance,
    ) {
        val proxyCall = runtime.composeCall(
            moduleName = Modules.PROXY,
            callName = "proxy",
            arguments = mapOf(
                "real" to AddressInstanceConstructor.constructInstance(runtime.typeRegistry, proxiedAccountId),
                "force_proxy_type" to DictEnum.Entry(proxyType.name, null),
                "call" to proxiedCall
            )
        )

        resetCalls()
        call(proxyCall)
    }

    private suspend fun acknowledgeProxyOperation(proxyMetaAccount: MetaAccount) {
        val resume = proxySigningPresenter.acknowledgeProxyOperation(proxiedMetaAccount, proxyMetaAccount)
        if (!resume) {
            throw SigningCancelledException()
        }
    }

    private suspend fun computeProxyMetaAccount(): MetaAccount {
        val proxyAccount = proxiedMetaAccount.proxy
        return accountRepository.getMetaAccount(proxyAccount.proxyMetaId)
    }

    private suspend fun notEnoughPermission(proxyMetaAccount: MetaAccount, availableProxyTypes: List<ProxyType>): Nothing {
        proxySigningPresenter.notEnoughPermission(proxiedMetaAccount, proxyMetaAccount, availableProxyTypes)
        throw SigningCancelledException()
    }

    private suspend fun signingNotSupported(): Nothing {
        proxySigningPresenter.signingIsNotSupported()
        throw SigningCancelledException()
    }

    private suspend fun proxyPaysFees(): Boolean {
        // Our direct proxy only pay fees it is a leaf. Otherwise fees paid by proxy's own delegate
        return delegateSigner() is LeafSigner
    }
}
