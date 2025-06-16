package io.novafoundation.nova.feature_account_api.data.multisig.validation

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

sealed interface MultisigExtrinsicValidationFailure {

    sealed interface NotEnoughSignatoryBalance : MultisigExtrinsicValidationFailure {

        val signatory: MetaAccount

        class ToPayFeeAndDeposit(
            override val signatory: MetaAccount,
            val asset: Chain.Asset,
            val fee: BigInteger,
            val deposit: BigInteger,
            val availableBalance: BigInteger
        ) : NotEnoughSignatoryBalance

        class ToPlaceDeposit(
            override val signatory: MetaAccount,
            val asset: Chain.Asset,
            val deposit: BigInteger,
            val availableBalance: BigInteger
        ) : NotEnoughSignatoryBalance

        class ToPayFeeAndStayAboveEd(
            override val signatory: MetaAccount,
            val asset: Chain.Asset,
            val neededBalance: BalanceOf,
            val availableBalance: BigInteger
        ) : NotEnoughSignatoryBalance
    }

    class OperationAlreadyExists(
        val multisigAccount: MultisigMetaAccount
    ) : MultisigExtrinsicValidationFailure
}
