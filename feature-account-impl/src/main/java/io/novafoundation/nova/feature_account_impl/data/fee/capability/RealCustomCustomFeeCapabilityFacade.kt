package io.novafoundation.nova.feature_account_impl.data.fee.capability

import io.novafoundation.nova.feature_account_api.data.fee.capability.CustomFeeCapability
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requestedAccountPaysFees
import io.novafoundation.nova.feature_account_api.data.fee.capability.CustomFeeCapabilityFacade
import io.novafoundation.nova.runtime.ext.isCommissionAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class RealCustomCustomFeeCapabilityFacade(
    private val accountRepository: AccountRepository,
) : CustomFeeCapabilityFacade {

    override suspend fun canPayFeeInNonUtilityToken(chainAsset: Chain.Asset, customFeeCapability: CustomFeeCapability): Boolean {
        val isCustomFeeToken = !chainAsset.isCommissionAsset
        val currentMetaAccount = accountRepository.getSelectedMetaAccount()

        // TODO we disable custom fee tokens payment for account types where current account is not the one who pays fees (e.g. it is proxied).
        // This restriction can be removed once we consider all corner-cases
        return isCustomFeeToken && customFeeCapability.canPayFeeInNonUtilityToken(chainAsset) && currentMetaAccount.type.requestedAccountPaysFees()
    }
}
