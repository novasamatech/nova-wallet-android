package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations

import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer.Payload.DialogAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isTrueOrWarning
import io.novafoundation.nova.feature_account_api.data.model.amountByExecutingAccount
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.NominationPoolsAvailableBalanceResolver
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.SimpleFeeProducer
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigDecimal

class PoolAvailableBalanceValidationFactory(
    private val poolsAvailableBalanceResolver: NominationPoolsAvailableBalanceResolver,
) {

    context(ValidationSystemBuilder<P, E>)
    fun <P, E> enoughAvailableBalanceToStake(
        asset: (P) -> Asset,
        fee: SimpleFeeProducer<P>,
        amount: (P) -> BigDecimal,
        error: (PoolAvailableBalanceValidation.ValidationError.Context) -> E
    ) {
        validate(
            PoolAvailableBalanceValidation(
                poolsAvailableBalanceResolver = poolsAvailableBalanceResolver,
                asset = asset,
                fee = fee,
                error = error,
                amount = amount
            )
        )
    }
}

class PoolAvailableBalanceValidation<P, E>(
    private val poolsAvailableBalanceResolver: NominationPoolsAvailableBalanceResolver,
    private val asset: (P) -> Asset,
    private val fee: SimpleFeeProducer<P>,
    private val amount: (P) -> BigDecimal,
    private val error: (ValidationError.Context) -> E
) : Validation<P, E> {

    interface ValidationError {

        val context: Context

        class Context(
            val availableBalance: Balance,
            val minimumBalance: Balance,
            val fee: Balance,
            val maximumToStake: Balance,
            val chainAsset: Chain.Asset,
        )
    }

    override suspend fun validate(value: P): ValidationStatus<E> {
        val asset = asset(value)
        val chainAsset = asset.token.configuration

        val fee = fee(value)?.amountByExecutingAccount.orZero()
        val availableBalance = poolsAvailableBalanceResolver.availableBalanceToStartStaking(asset)
        val maxToStake = poolsAvailableBalanceResolver.maximumBalanceToStake(asset, fee)
        val enteredAmount = chainAsset.planksFromAmount(amount(value))

        val hasEnoughToStake = enteredAmount <= maxToStake.maxToStake

        return hasEnoughToStake isTrueOrWarning {
            val errorContext = ValidationError.Context(
                availableBalance = availableBalance,
                minimumBalance = maxToStake.existentialDeposit,
                fee = fee,
                maximumToStake = maxToStake.maxToStake,
                chainAsset = chainAsset
            )
            error(errorContext)
        }
    }
}

fun <P> handlePoolAvailableBalanceError(
    error: PoolAvailableBalanceValidation.ValidationError,
    resourceManager: ResourceManager,
    flowActions: ValidationFlowActions<P>,
    modifyPayload: (oldPayload: P, maxAmountToStake: BigDecimal) -> P,
    updateAmountInUi: (maxAmountToStake: BigDecimal) -> Unit = {}
): TransformedFailure.Custom = with(error.context) {
    val maximumToStakeAmount = chainAsset.amountFromPlanks(maximumToStake)

    val dialogPayload = CustomDialogDisplayer.Payload(
        title = resourceManager.getString(R.string.common_not_enough_funds_title),
        message = resourceManager.getString(
            R.string.staking_pool_available_validation_message,
            availableBalance.formatPlanks(chainAsset),
            minimumBalance.formatPlanks(chainAsset),
            fee.formatPlanks(chainAsset),
            maximumToStakeAmount.formatTokenAmount(chainAsset)
        ),
        okAction = if (maximumToStake.isPositive()) {
            DialogAction(
                title = resourceManager.getString(R.string.staking_stake_max),
                action = {
                    updateAmountInUi(maximumToStakeAmount)

                    flowActions.revalidate { oldPayload -> modifyPayload(oldPayload, maximumToStakeAmount) }
                }
            )
        } else {
            null
        },
        cancelAction = DialogAction.noOp(resourceManager.getString(R.string.common_close))
    )

    TransformedFailure.Custom(dialogPayload)
}
