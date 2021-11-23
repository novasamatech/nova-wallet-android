package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.model.FeeModel
import kotlinx.coroutines.CoroutineScope
import java.math.BigDecimal
import java.math.BigInteger

sealed class FeeStatus {
    object Loading : FeeStatus()

    class Loaded(val feeModel: FeeModel) : FeeStatus()

    object Error : FeeStatus()
}

interface FeeLoaderMixin : Retriable {

    val feeLiveData: LiveData<FeeStatus>

    interface Presentation : FeeLoaderMixin {

        suspend fun loadFeeSuspending(
            retryScope: CoroutineScope,
            feeConstructor: suspend (Token) -> BigInteger,
            onRetryCancelled: () -> Unit,
        )

        fun loadFee(
            coroutineScope: CoroutineScope,
            feeConstructor: suspend (Token) -> BigInteger,
            onRetryCancelled: () -> Unit,
        )

        fun requireFee(
            block: (BigDecimal) -> Unit,
            onError: (title: String, message: String) -> Unit,
        )
    }
}

fun FeeLoaderMixin.Presentation.requireFee(
    viewModel: BaseViewModel,
    block: (BigDecimal) -> Unit
) {
    requireFee(block) { title, message ->
        viewModel.showError(title, message)
    }
}
