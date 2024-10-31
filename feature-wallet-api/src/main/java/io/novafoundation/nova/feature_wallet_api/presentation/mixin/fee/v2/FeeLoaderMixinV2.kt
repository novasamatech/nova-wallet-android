package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2

import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.SetFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount.DefaultFeeBalanceExtractor
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount.FeeBalanceExtractor
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter.DefaultFeeFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.formatter.FeeFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.ChooseFeeCurrencyPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.PaymentCurrencySelectionMode
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2.Configuration
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2.Factory
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface FeeLoaderMixinV2<F, D> : Retriable {

    class Configuration<F, D>(
        val showZeroFiat: Boolean = true,
        val initialState: InitialState<F, D> = InitialState(),
        val onRetryCancelled: () -> Unit = {}
    ) {

        class InitialState<F, D>(
            val paymentCurrencySelectionMode: PaymentCurrencySelectionMode = PaymentCurrencySelectionMode.DISABLED,
            val feeStatus: FeeStatus<F, D>? = null
        )
    }

    val fee: StateFlow<FeeStatus<F, D>>

    val userCanChangeFeeAsset: Flow<Boolean>

    val chooseFeeAsset: ActionAwaitableMixin<ChooseFeeCurrencyPayload, Chain.Asset>

    fun changePaymentCurrencyClicked()

    interface Presentation<F, D> : FeeLoaderMixinV2<F, D>, SetFee<F> {

        suspend fun feeAsset(): Asset

        suspend fun feePaymentCurrency(): FeePaymentCurrency

        fun loadFee(feeConstructor: FeeConstructor<F>)

        suspend fun setPaymentCurrencySelectionMode(mode: PaymentCurrencySelectionMode)

        suspend fun setFeeOrHide(fee: F?)

        suspend fun setFeeLoading()

        suspend fun setFeeStatus(feeStatus: FeeStatus<F, D>)
    }

    interface Factory {

        fun <F, D> create(
            scope: CoroutineScope,
            selectedChainAssetFlow: Flow<Chain.Asset>,
            feeFormatter: FeeFormatter<F, D>,
            feeBalanceExtractor: FeeBalanceExtractor<F>,
            configuration: Configuration<F, D> =  Configuration()
        ): Presentation<F, D>
    }
}

typealias FeeConstructor<F> = suspend (FeePaymentCurrency) -> F?

fun <F : FeeBase> Factory.createDefault(
    scope: CoroutineScope,
    selectedChainAssetFlow: Flow<Chain.Asset>,
    configuration: Configuration<F, FeeDisplay> = Configuration()
): FeeLoaderMixinV2.Presentation<F, FeeDisplay> {
    return create(
        scope = scope,
        selectedChainAssetFlow = selectedChainAssetFlow,
        feeFormatter = DefaultFeeFormatter(),
        feeBalanceExtractor = DefaultFeeBalanceExtractor(),
        configuration = configuration
    )
}
