package io.novafoundation.nova.feature_account_impl.data.fee.capability

import android.util.Log
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.fee.capability.CustomFeeCapabilityFacade
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requestedAccountPaysFees

class RealCustomCustomFeeCapabilityFacade(
    private val accountRepository: AccountRepository,
    private val feePaymentProviderRegistry: FeePaymentProviderRegistry
) : CustomFeeCapabilityFacade {

    override suspend fun canPayFeeInCurrency(currency: FeePaymentCurrency): Boolean {
        return when (currency) {
            is FeePaymentCurrency.Asset -> canPayFeeInNonUtilityAsset(currency)

            FeePaymentCurrency.Native -> true
        }
    }

    private suspend fun canPayFeeInNonUtilityAsset(
        currency: FeePaymentCurrency.Asset,
    ): Boolean {
        if (hasGlobalFeePaymentRestrictions()) return false

        return feePaymentProviderRegistry.providerFor(currency.asset.chainId)
            .canPayFee(currency)
            .onFailure { Log.e("RealCustomCustomFeeCapabilityFacade", "Failed to check canPayFee", it) }
            .getOrDefault(false)
    }

    override suspend fun hasGlobalFeePaymentRestrictions(): Boolean {
        val currentMetaAccount = accountRepository.getSelectedMetaAccount()
        return !currentMetaAccount.type.requestedAccountPaysFees()
    }
}
