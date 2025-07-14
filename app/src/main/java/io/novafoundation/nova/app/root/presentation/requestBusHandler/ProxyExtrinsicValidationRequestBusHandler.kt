package io.novafoundation.nova.app.root.presentation.requestBusHandler

import io.novafoundation.nova.common.utils.bus.observeBusEvent
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxyExtrinsicValidationRequestBus
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxyExtrinsicValidationRequestBus.ValidationResponse
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxiedExtrinsicValidationFailure
import io.novafoundation.nova.feature_account_api.data.proxy.validation.ProxiedExtrinsicValidationPayload
import io.novafoundation.nova.feature_wallet_api.domain.validation.ProxyHaveEnoughFeeValidationFactory
import io.novafoundation.nova.feature_wallet_api.domain.validation.proxyHasEnoughFeeValidation
import kotlinx.coroutines.flow.Flow

class ProxyExtrinsicValidationRequestBusHandler(
    private val proxyProxyExtrinsicValidationRequestBus: ProxyExtrinsicValidationRequestBus,
    private val proxyHaveEnoughFeeValidationFactory: ProxyHaveEnoughFeeValidationFactory
) : RequestBusHandler {

    override fun observe(): Flow<*> {
        return proxyProxyExtrinsicValidationRequestBus.observeEvent()
            .observeBusEvent { request ->
                val validationResult = createValidationSystem()
                    .validate(request.validationPayload)
                ValidationResponse(validationResult)
            }
    }

    private fun createValidationSystem(): ValidationSystem<ProxiedExtrinsicValidationPayload, ProxiedExtrinsicValidationFailure> {
        return ValidationSystem {
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
                val asset = payload.chainWithAsset.asset
                ProxiedExtrinsicValidationFailure.ProxyNotEnoughFee(
                    metaAccount = payload.proxyMetaAccount,
                    asset = asset,
                    fee = fee.amount,
                    availableBalance = availableBalance
                )
            }
        )
    }
}
