package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount.validation

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigDecimal

class ChooseDelegationAmountValidationPayload(
    val fee: Fee,
    val asset: Asset,
    val amount: BigDecimal,
    val maxAvailableAmount: BigDecimal,
    val delegate: AccountId
)
