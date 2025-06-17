package io.novafoundation.nova.feature_wallet_impl.domain.validaiton.multisig

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.isTrueOrError
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.intoOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.multisig.repository.MultisigValidationsRepository
import io.novafoundation.nova.feature_account_api.data.multisig.repository.getMultisigDeposit
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidation
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationFailure
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationPayload
import io.novafoundation.nova.feature_account_api.data.multisig.validation.SignatoryFeePaymentMode
import io.novafoundation.nova.feature_account_api.data.multisig.validation.signatoryAccountId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.ChainAssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

class MultisigSignatoryHasEnoughBalanceValidation(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val extrinsicService: ExtrinsicService,
    private val multisigValidationsRepository: MultisigValidationsRepository,
) : MultisigExtrinsicValidation {

    override suspend fun validate(value: MultisigExtrinsicValidationPayload): ValidationStatus<MultisigExtrinsicValidationFailure> {
        val chain = value.chain
        val chainAsset = chain.utilityAsset

        val balance = assetSourceRegistry.sourceFor(chainAsset).balance.queryAccountBalance(value.chain, chainAsset, value.signatoryAccountId())
        val multisigDeposit = multisigValidationsRepository.getMultisigDeposit(chain.id, value.multisig.threshold)
        val ed = assetSourceRegistry.existentialDepositInPlanks(chainAsset)

        return when (val mode = value.signatoryFeePaymentMode) {
            SignatoryFeePaymentMode.NothingToPay -> validateDeposit(value, balance, multisigDeposit, ed)
            is SignatoryFeePaymentMode.PaysSubmissionFee -> validateFeeAndDeposit(value, balance, multisigDeposit, ed, mode.actualCall)
        }
    }

    private fun validateDeposit(
        value: MultisigExtrinsicValidationPayload,
        balance: ChainAssetBalance,
        deposit: Balance,
        existentialDeposit: Balance,
    ): ValidationStatus<MultisigExtrinsicValidationFailure> {
        val reservable = balance.legacyAdapter().reservable(existentialDeposit)

        return (reservable >= deposit) isTrueOrError {
            MultisigExtrinsicValidationFailure.NotEnoughSignatoryBalance.ToPlaceDeposit(
                signatory = value.signatory,
                asset = value.chain.utilityAsset,
                deposit = deposit,
                availableBalance = balance.free
            )
        }
    }

    private suspend fun validateFeeAndDeposit(
        value: MultisigExtrinsicValidationPayload,
        balance: ChainAssetBalance,
        deposit: Balance,
        existentialDeposit: Balance,
        signatoryCall: GenericCall.Instance,
    ): ValidationStatus<MultisigExtrinsicValidationFailure> {
        val fee = calculateFee(value, signatoryCall)
        val asset = value.chain.utilityAsset

        val reservable = balance.legacyAdapter().reservable(existentialDeposit)

        if (deposit + fee.amount > reservable) {
            return MultisigExtrinsicValidationFailure.NotEnoughSignatoryBalance.ToPayFeeAndDeposit(
                signatory = value.signatory,
                asset = asset,
                fee = fee.amount,
                deposit = deposit,
                availableBalance = reservable
            ).validationError()
        }

        val ed = assetSourceRegistry.existentialDepositInPlanks(asset)
        val neededBalance = ed + fee.amount
        if (balance.countedTowardsEd < neededBalance) {
            return MultisigExtrinsicValidationFailure.NotEnoughSignatoryBalance.ToPayFeeAndStayAboveEd(
                signatory = value.signatory,
                asset = asset,
                neededBalance = neededBalance,
                availableBalance = balance.countedTowardsEd
            ).validationError()
        }

        return valid()
    }

    private suspend fun calculateFee(value: MultisigExtrinsicValidationPayload, signatoryCall: GenericCall.Instance): Fee {
        return extrinsicService.estimateFee(value.chain, value.signatory.intoOrigin()) {
            call(signatoryCall)
        }
    }
}
