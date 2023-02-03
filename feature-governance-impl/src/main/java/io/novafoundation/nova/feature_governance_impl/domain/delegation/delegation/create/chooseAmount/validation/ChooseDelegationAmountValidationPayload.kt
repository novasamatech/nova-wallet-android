package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount.validation

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigDecimal

class ChooseDelegationAmountValidationPayload(
    val fee: BigDecimal,
    val asset: Asset,
    val amount: BigDecimal,
    val delegate: AccountId
)
