package io.novafoundation.nova.feature_account_impl.presentation.multisig

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.amountFromPlanks
import io.novafoundation.nova.common.utils.boldSpan
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatTokenAmount
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationFailure
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationFailure.NotEnoughSignatoryBalance
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported.SigningNotSupportedPresentable
import io.novafoundation.nova.feature_account_impl.presentation.sign.NestedSigningPresenter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

interface MultisigSigningPresenter {

    suspend fun acknowledgeMultisigOperation(multisig: MultisigMetaAccount, signatory: MetaAccount): Boolean

    suspend fun signingIsNotSupported()

    suspend fun presentValidationFailure(failure: MultisigExtrinsicValidationFailure)
}

@FeatureScope
class RealMultisigSigningPresenter @Inject constructor(
    private val nestedSigningPresenter: NestedSigningPresenter,
    private val resourceManager: ResourceManager,
    private val signingNotSupportedPresentable: SigningNotSupportedPresentable,
) : MultisigSigningPresenter {

    override suspend fun acknowledgeMultisigOperation(multisig: MultisigMetaAccount, signatory: MetaAccount): Boolean {
        return nestedSigningPresenter.acknowledgeNestedSignOperation(
            warningShowFor = multisig,
            title = { resourceManager.getString(R.string.multisig_signing_warning_title) },
            subtitle = { formatSubtitleForWarning(signatory) },
            iconRes = { R.drawable.ic_multisig }
        )
    }

    override suspend fun signingIsNotSupported() {
        signingNotSupportedPresentable.presentSigningNotSupported(
            SigningNotSupportedPresentable.Payload(
                iconRes = R.drawable.ic_multisig,
                message = resourceManager.getString(R.string.multisig_signing_is_not_supported_message)
            )
        )
    }

    override suspend fun presentValidationFailure(failure: MultisigExtrinsicValidationFailure) {
        val (title, message) = when (failure) {
            is NotEnoughSignatoryBalance -> formatBalanceFailure(failure)

            is MultisigExtrinsicValidationFailure.OperationAlreadyExists -> formatOperationAlreadyExists(failure)
        }

        nestedSigningPresenter.presentValidationFailure(title, message)
    }

    private fun formatOperationAlreadyExists(failure: MultisigExtrinsicValidationFailure.OperationAlreadyExists): ValidationTitleAndMessage {
        val title = resourceManager.getString(R.string.multisig_callhash_exists_title)

        val messageFormat = resourceManager.getString(R.string.multisig_callhash_exists_message)
        val nameFormatted = formatName(failure.multisigAccount)
        val message = SpannableFormatter.format(messageFormat, nameFormatted)

        return title to message
    }

    private fun formatBalanceFailure(failure: NotEnoughSignatoryBalance): ValidationTitleAndMessage {
        val title: String = resourceManager.getString(R.string.common_error_not_enough_tokens)

        val signatoryName = formatName(failure.signatory)
        val message: CharSequence = when (failure) {
            is NotEnoughSignatoryBalance.ToPayFeeAndDeposit -> {
                val format = resourceManager.getString(R.string.multisig_signatory_validation_deposit_fee)
                SpannableFormatter.format(
                    format,
                    signatoryName,
                    formatAmount(failure.asset, failure.fee),
                    formatAmount(failure.asset, failure.deposit),
                    formatAmount(failure.asset, failure.availableBalance)
                )
            }

            is NotEnoughSignatoryBalance.ToPayFeeAndStayAboveEd -> {
                val format = resourceManager.getString(R.string.multisig_signatory_validation_ed)
                SpannableFormatter.format(
                    format,
                    signatoryName,
                    formatAmount(failure.asset, failure.neededBalance),
                    formatAmount(failure.asset, failure.availableBalance)
                )
            }

            is NotEnoughSignatoryBalance.ToPlaceDeposit -> {
                val format = resourceManager.getString(R.string.multisig_signatory_validation_deposit)
                SpannableFormatter.format(
                    format,
                    signatoryName,
                    formatAmount(failure.asset, failure.deposit),
                    formatAmount(failure.asset, failure.availableBalance)
                )
            }
        }

        return title to message
    }

    private fun formatAmount(asset: Chain.Asset, amount: BalanceOf): String {
        return amount.amountFromPlanks(asset.precision).formatTokenAmount(asset.symbol)
    }

    private fun formatName(metaAccount: MetaAccount): CharSequence {
        return metaAccount.name.toSpannable(boldSpan())
    }

    private fun formatSubtitleForWarning(signatory: MetaAccount): CharSequence {
        val subtitle = resourceManager.getString(R.string.multisig_signing_warning_message)
        val primaryColor = resourceManager.getColor(R.color.text_primary)
        val proxyName = signatory.name.toSpannable(colorSpan(primaryColor))
        return SpannableFormatter.format(subtitle, proxyName)
    }
}

private typealias ValidationTitleAndMessage = Pair<String, CharSequence>
