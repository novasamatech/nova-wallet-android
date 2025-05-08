package io.novafoundation.nova.feature_pay_impl.presentation.shop

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_pay_impl.domain.ShopInteractor
import io.novafoundation.nova.feature_pay_impl.presentation.PayRouter
import kotlinx.coroutines.flow.Flow

class ShopViewModel(
    private val router: PayRouter,
    private val shopInteractor: ShopInteractor
) : BaseViewModel() {

    val isWalletAvailableFlow: Flow<Boolean> = shopInteractor.observeAccountAvailableForShopping()
        .shareInBackground()
}
