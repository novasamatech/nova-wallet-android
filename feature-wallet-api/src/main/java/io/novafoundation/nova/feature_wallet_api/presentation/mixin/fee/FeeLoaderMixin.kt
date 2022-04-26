package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.model.FeeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.BigInteger

sealed class FeeStatus {
    object Loading : FeeStatus()

    class Loaded(val feeModel: FeeModel) : FeeStatus()

    object Error : FeeStatus()
}

interface FeeLoaderMixin : Retriable {

    class Configuration(
        val showZeroFiat: Boolean = true
    )

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

        suspend fun setFee(fee: BigDecimal)

        fun requireFee(
            block: (BigDecimal) -> Unit,
            onError: (title: String, message: String) -> Unit,
        )
    }

    interface Factory {

        fun create(
            tokenFlow: Flow<Token>,
            configuration: Configuration = Configuration()
        ): Presentation
    }
}

fun FeeLoaderMixin.Factory.create(assetFlow: Flow<Asset>) = create(assetFlow.map { it.token })
fun FeeLoaderMixin.Factory.create(tokenUseCase: TokenUseCase) = create(tokenUseCase.currentTokenFlow())

fun FeeLoaderMixin.Presentation.requireFee(
    viewModel: BaseViewModel,
    block: (BigDecimal) -> Unit,
) {
    requireFee(block) { title, message ->
        viewModel.showError(title, message)
    }
}
