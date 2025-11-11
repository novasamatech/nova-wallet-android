package io.novafoundation.nova.feature_account_impl.data.fee.capability

import io.novafoundation.nova.feature_account_api.data.fee.capability.CustomFeeCapability
import io.novafoundation.nova.feature_account_api.data.fee.capability.CustomFeeCapabilityFacade
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requestedAccountPaysFees
import io.novafoundation.nova.runtime.ext.isCommissionAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class RealCustomCustomFeeCapabilityFacade(
    private val accountRepository: AccountRepository,
) : CustomFeeCapabilityFacade {

    override suspend fun canPayFeeInNonUtilityToken(chainAsset: Chain.Asset, customFeeCapability: CustomFeeCapability): Boolean {
        val isCustomFeeToken = !chainAsset.isCommissionAsset

        return isCustomFeeToken && !hasGlobalFeePaymentRestrictions() && customFeeCapability.canPayFeeInNonUtilityToken(chainAsset)
    }

    override suspend fun hasGlobalFeePaymentRestrictions(): Boolean {
        val currentMetaAccount = accountRepository.getSelectedMetaAccount()
        return !currentMetaAccount.type.requestedAccountPaysFees()
    }
}
