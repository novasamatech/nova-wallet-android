package io.novafoundation.nova.feature_wallet_impl.domain.validaiton.multisig

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.intoOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.data.multisig.repository.MultisigValidationsRepository
import io.novafoundation.nova.feature_account_api.data.multisig.repository.getMultisigDeposit
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidation
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationFailure
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationPayload
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationStatus
import io.novafoundation.nova.feature_account_api.data.multisig.validation.SignatoryFeePaymentMode
import io.novafoundation.nova.feature_account_api.data.multisig.validation.signatoryAccountId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.accountBalanceForValidation
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.validation.balance.BalanceValidationResult
import io.novafoundation.nova.feature_wallet_api.domain.validation.balance.beginValidation
import io.novafoundation.nova.feature_wallet_api.domain.validation.balance.toValidationStatus
import io.novafoundation.nova.feature_wallet_api.domain.validation.balance.tryReserve
import io.novafoundation.nova.feature_wallet_api.domain.validation.balance.tryWithdrawFee
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall


class MultisigSignatoryHasEnoughBalanceValidation(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val extrinsicService: ExtrinsicService,
    private val multisigValidationsRepository: MultisigValidationsRepository,
) : MultisigExtrinsicValidation {

    override suspend fun validate(value: MultisigExtrinsicValidationPayload): ValidationStatus<MultisigExtrinsicValidationFailure> {
        val chain = value.chain
        val chainAsset = chain.utilityAsset

        val fee = calculateFee(value)
        val deposit = determineNeededDeposit(value)

        var balanceValidation = assetSourceRegistry.sourceFor(chainAsset).balance
            .accountBalanceForValidation(value.chain, chainAsset, value.signatoryAccountId())
            .beginValidation()

        if (fee != null) {
            balanceValidation = balanceValidation.tryWithdrawFee(fee)
        }

        if (deposit != null) {
            balanceValidation = balanceValidation.tryReserve(deposit)
        }

        return balanceValidation.toValidationStatus { prepareError(value, fee, deposit, it) }
    }

    private suspend fun calculateFee(payload: MultisigExtrinsicValidationPayload): Fee? {
        return when (val mode = payload.signatoryFeePaymentMode) {
            SignatoryFeePaymentMode.NothingToPay -> null

            is SignatoryFeePaymentMode.PaysSubmissionFee -> calculateFee(payload, mode.actualCall)
        }
    }

    private suspend fun determineNeededDeposit(payload: MultisigExtrinsicValidationPayload): Balance? {
        return multisigValidationsRepository.getMultisigDeposit(payload.chain.id, payload.multisig.threshold)
            .takeIf { it.isPositive() }
    }

    private fun prepareError(
        payload: MultisigExtrinsicValidationPayload,
        fee: FeeBase?,
        deposit: Balance?,
        balanceError: BalanceValidationResult.Failure
    ): MultisigExtrinsicValidationStatus {
        return MultisigExtrinsicValidationFailure.NotEnoughSignatoryBalance(
            signatory = payload.signatory,
            asset = payload.chain.utilityAsset,
            fee = fee?.amount,
            deposit = deposit,
            balanceToAdd = balanceError.negativeImbalance.value
        ).validationError()
    }

    private suspend fun calculateFee(value: MultisigExtrinsicValidationPayload, signatoryCall: GenericCall.Instance): Fee {
        return extrinsicService.estimateFee(value.chain, value.signatory.intoOrigin()) {
            call(signatoryCall)
        }
    }
}
