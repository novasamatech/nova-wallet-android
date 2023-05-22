package io.novafoundation.nova.feature_account_api.domain.validation

import androidx.annotation.StringRes
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer.Payload.DialogAction.Companion.noOp
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.validationError
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.LEDGER
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount.Type.PARITY_SIGNER
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.hasAccountIn
import io.novafoundation.nova.feature_account_api.domain.validation.NoChainAccountFoundError.AddAccountState
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.SubstrateApplicationConfig
import io.novafoundation.nova.feature_ledger_api.sdk.application.substrate.supports
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface NoChainAccountFoundError {
    val chain: Chain
    val account: MetaAccount
    val addAccountState: AddAccountState

    enum class AddAccountState {
        CAN_ADD, LEDGER_NOT_SUPPORTED, PARITY_SIGNER_NOT_SUPPORTED
    }
}

class HasChainAccountValidation<P, E>(
    private val chainExtractor: (P) -> Chain,
    private val metaAccountExtractor: (P) -> MetaAccount,
    private val errorProducer: (chain: Chain, account: MetaAccount, addAccountState: AddAccountState) -> E
) : Validation<P, E> {

    override suspend fun validate(value: P): ValidationStatus<E> {
        val account = metaAccountExtractor(value)
        val chain = chainExtractor(value)

        return when {
            account.hasAccountIn(chain) -> ValidationStatus.Valid()
            account.type == LEDGER && !SubstrateApplicationConfig.supports(chain.id) -> {
                errorProducer(chain, account, AddAccountState.LEDGER_NOT_SUPPORTED).validationError()
            }
            account.type == PARITY_SIGNER && chain.isEthereumBased -> {
                errorProducer(chain, account, AddAccountState.PARITY_SIGNER_NOT_SUPPORTED).validationError()
            }
            else -> errorProducer(chain, account, AddAccountState.CAN_ADD).validationError()
        }
    }
}

fun <P, E> ValidationSystemBuilder<P, E>.hasChainAccount(
    chain: (P) -> Chain,
    metaAccount: (P) -> MetaAccount,
    error: (chain: Chain, account: MetaAccount, addAccountState: AddAccountState) -> E
) {
    validate(HasChainAccountValidation(chain, metaAccount, error))
}

fun handleChainAccountNotFound(
    failure: NoChainAccountFoundError,
    @StringRes addAccountDescriptionRes: Int,
    resourceManager: ResourceManager,
    goToWalletDetails: () -> Unit
): TransformedFailure {
    val chainName = failure.chain.name

    return when (failure.addAccountState) {
        AddAccountState.CAN_ADD -> TransformedFailure.Custom(
            dialogPayload = CustomDialogDisplayer.Payload(
                title = resourceManager.getString(R.string.common_missing_account_title, chainName),
                message = resourceManager.getString(addAccountDescriptionRes, chainName),
                okAction = CustomDialogDisplayer.Payload.DialogAction(
                    title = resourceManager.getString(R.string.common_add),
                    action = goToWalletDetails
                ),
                cancelAction = noOp(resourceManager.getString(R.string.common_cancel)),
                customStyle = R.style.AccentNegativeAlertDialogTheme
            )
        )
        AddAccountState.LEDGER_NOT_SUPPORTED -> TransformedFailure.Default(
            resourceManager.getString(R.string.ledger_chain_not_supported, chainName) to null
        )
        AddAccountState.PARITY_SIGNER_NOT_SUPPORTED -> TransformedFailure.Default(
            resourceManager.getString(R.string.account_parity_signer_chain_not_supported, chainName) to null
        )
    }
}
