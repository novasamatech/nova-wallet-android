package io.novafoundation.nova.app.root.presentation.requestBusHandler

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.bus.observeBusEvent
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxyExtrinsicValidationRequestBus
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxyExtrinsicValidationRequestBus.ValidationResponse
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxiedExtrinsicValidationFailure
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxiedExtrinsicValidationPayload
import io.novafoundation.nova.feature_wallet_api.domain.validation.ProxyHaveEnoughFeeValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.proxyHasEnoughFeeValidation
import kotlinx.coroutines.flow.launchIn

class ProxyExtrinsicValidationRequestBusHandler(
    private val scope: RootScope,
    private val proxyProxyExtrinsicValidationRequestBus: ProxyExtrinsicValidationRequestBus,
    private val proxyHaveEnoughFeeValidationFactory: ProxyHaveEnoughFeeValidationFactory,
    private val resourceManager: ResourceManager
) : RequestBusHandler {

    override fun observe() {
        proxyProxyExtrinsicValidationRequestBus.observeEvent()
            .observeBusEvent { request ->
                val validationResult = createValidationSystem()
                    .validate(request.validationPayload)
                ValidationResponse(validationResult)
            }.launchIn(scope)
    }

    private fun createValidationSystem(): ValidationSystem<ProxiedExtrinsicValidationPayload, ProxiedExtrinsicValidationFailure> {
        return ValidationSystem<ProxiedExtrinsicValidationPayload, ProxiedExtrinsicValidationFailure> {
            proxyHasEnoughFee()
        }
    }

    private fun ValidationSystemBuilder<ProxiedExtrinsicValidationPayload, ProxiedExtrinsicValidationFailure>.proxyHasEnoughFee() {
        proxyHasEnoughFeeValidation(
            factory = proxyHaveEnoughFeeValidationFactory,
            metaAccount = { it.proxyMetaAccount },
            proxyAccountId = { it.proxyAccountId },
            call = { it.call },
            chainWithAsset = { it.chainWithAsset },
            proxyNotEnoughFee = { payload, availableBalance, fee ->
                ProxiedExtrinsicValidationFailure.ProxyNotEnoughFee(
                    payload.proxyMetaAccount,
                    payload.chainWithAsset.asset,
                    availableBalance,
                    fee
                )
            }
        )
    }
}
