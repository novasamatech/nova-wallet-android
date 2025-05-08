package io.novafoundation.nova.feature_pay_impl.presentation.shop

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_pay_impl.domain.ShopInteractor
import io.novafoundation.nova.feature_pay_impl.domain.brand.ShopBrandsInteractor
import io.novafoundation.nova.feature_pay_impl.presentation.PayRouter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ShopViewModel(
    private val router: PayRouter,
    private val shopInteractor: ShopInteractor,
    private val shopBrandsInteractorUseCase: ShopBrandsInteractor
) : BaseViewModel() {

    val isWalletAvailableFlow: Flow<Boolean> = shopInteractor.observeAccountAvailableForShopping()
        .shareInBackground()

    init {
        launch {
            shopBrandsInteractorUseCase.invoke()
        }
    }

}
