package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import java.math.BigDecimal

data class BittensorStakeValidationPayload(
    val expectedMetaId: Long,
    val selectedMetaId: Long,
    val asset: Asset,
    val amount: BigDecimal,
    val fee: Fee
)

enum class BittensorStakeValidationFailure { WALLET_CHANGED, NON_POSITIVE_AMOUNT, INSUFFICIENT_BALANCE }

typealias BittensorStakeValidationSystem = ValidationSystem<BittensorStakeValidationPayload, BittensorStakeValidationFailure>

class BittensorSelectedWalletValidation : Validation<BittensorStakeValidationPayload, BittensorStakeValidationFailure> {

    override suspend fun validate(value: BittensorStakeValidationPayload) = validOrError(value.expectedMetaId == value.selectedMetaId) {
        BittensorStakeValidationFailure.WALLET_CHANGED
    }
}

fun bittensorStakeValidationSystem(walletValidation: BittensorSelectedWalletValidation): BittensorStakeValidationSystem = ValidationSystem {
    validate(walletValidation)
    positiveAmount(amount = { it.amount }, error = { BittensorStakeValidationFailure.NON_POSITIVE_AMOUNT })
    sufficientBalance(
        fee = { it.fee },
        amount = { it.amount },
        available = { it.asset.transferable },
        error = { BittensorStakeValidationFailure.INSUFFICIENT_BALANCE }
    )
}
