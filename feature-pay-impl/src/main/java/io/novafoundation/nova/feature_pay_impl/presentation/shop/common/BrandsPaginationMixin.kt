package io.novafoundation.nova.feature_pay_impl.presentation.shop.common

import android.util.Log
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.PaginationMixin
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateMachine
import io.novafoundation.nova.common.utils.stateMachine.list.PaginatedListStateMachine.SideEffect
import io.novafoundation.nova.feature_pay_impl.R
import io.novafoundation.nova.feature_pay_impl.domain.brand.ShopBrandsInteractor
import io.novafoundation.nova.feature_pay_impl.domain.brand.model.RaiseBrand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class BrandsPaginationMixinFactory(
    private val shopBrandsInteractor: ShopBrandsInteractor,
    private val resourceManager: ResourceManager,
) {

    fun create(coroutineScope: CoroutineScope) = BrandsPaginationMixin(
        shopBrandsInteractor,
        resourceManager,
        coroutineScope
    )
}

class BrandsPaginationMixin(
    private val shopBrandsInteractor: ShopBrandsInteractor,
    private val resourceManager: ResourceManager,
    coroutineScope: CoroutineScope
) : PaginationMixin<RaiseBrand>(coroutineScope) {

    val errorFlow = MutableSharedFlow<String>()

    override suspend fun loadPage(event: SideEffect.LoadPage) {
        shopBrandsInteractor.loadBrands(event.query, event.nextPageOffset)
            .onSuccess { dataPage ->
                listStateMachine.onEvent(PaginatedListStateMachine.Event.NewPage(dataPage, event.query))
            }.onFailure {
                Log.e(LOG_TAG, "Failed to load Raise brands")
                listStateMachine.onEvent(PaginatedListStateMachine.Event.PageError(it))
            }
    }

    override fun presentError() {
        launch {
            errorFlow.emit(resourceManager.getString(R.string.common_loading_error))
        }
    }
}
