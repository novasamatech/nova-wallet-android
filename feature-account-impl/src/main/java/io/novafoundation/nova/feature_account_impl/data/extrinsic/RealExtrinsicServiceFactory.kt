package io.novafoundation.nova.feature_account_impl.data.extrinsic

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.extrinsic.multi.ExtrinsicSplitter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.network.rpc.RpcCalls

class RealExtrinsicServiceFactory(
    private val rpcCalls: RpcCalls,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val extrinsicBuilderFactory: ExtrinsicBuilderFactory,
    private val signerProvider: SignerProvider,
    private val extrinsicSplitter: ExtrinsicSplitter,
    private val eventsRepository: EventsRepository,
    private val feePaymentProviderRegistry: FeePaymentProviderRegistry
) : ExtrinsicService.Factory {

    override fun create(feeConfig: ExtrinsicService.FeePaymentConfig): ExtrinsicService {
        val registry = getRegistry(feeConfig)
        return RealExtrinsicService(
            rpcCalls = rpcCalls,
            chainRegistry = chainRegistry,
            accountRepository = accountRepository,
            extrinsicBuilderFactory = extrinsicBuilderFactory,
            signerProvider = signerProvider,
            extrinsicSplitter = extrinsicSplitter,
            feePaymentProviderRegistry = registry,
            eventsRepository = eventsRepository,
            coroutineScope = feeConfig.coroutineScope
        )
    }

    private fun getRegistry(config: ExtrinsicService.FeePaymentConfig): FeePaymentProviderRegistry {
        return config.customFeePaymentRegistry ?: feePaymentProviderRegistry
    }
}
