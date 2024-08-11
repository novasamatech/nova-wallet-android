package io.novafoundation.nova.feature_account_impl.data.extrinsic

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.extrinsic.multi.ExtrinsicSplitter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import kotlinx.coroutines.CoroutineScope

class RealExtrinsicServiceFactory(
    private val rpcCalls: RpcCalls,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val extrinsicBuilderFactory: ExtrinsicBuilderFactory,
    private val signerProvider: SignerProvider,
    private val extrinsicSplitter: ExtrinsicSplitter,
    private val feePaymentProviderRegistry: FeePaymentProviderRegistry
) : ExtrinsicService.Factory {

    override fun create(coroutineScope: CoroutineScope): ExtrinsicService {
        return RealExtrinsicService(
            rpcCalls,
            chainRegistry,
            accountRepository,
            extrinsicBuilderFactory,
            signerProvider,
            extrinsicSplitter,
            feePaymentProviderRegistry,
            coroutineScope
        )
    }
}
