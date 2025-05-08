package io.novafoundation.nova.feature_pay_impl.data.raise.brands

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.common.data.model.getPageNumberOrThrow
import io.novafoundation.nova.common.utils.NetworkStateService
import io.novafoundation.nova.common.utils.recoverWithDispatcher
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.feature_pay_impl.data.raise.brands.network.RaiseBrandsApi
import io.novafoundation.nova.feature_pay_impl.data.raise.brands.network.model.RaiseBrandResponse
import io.novafoundation.nova.feature_pay_impl.data.raise.common.RaiseListBody
import io.novafoundation.nova.feature_pay_impl.data.raise.common.toPageOffset
import io.novafoundation.nova.feature_pay_impl.domain.brand.model.RaiseBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

interface ShopBrandsRepository {

    suspend fun prefetch()

    suspend fun getBrands(query: String, pageOffset: PageOffset.Loadable): Result<DataPage<RaiseBrand>>
}

private const val BRANDS_PAGE_SIZE = 50
private const val EMPTY_QUERY = ""

class RealShopBrandsRepository(
    private val raiseBrandsApi: RaiseBrandsApi,
    private val networkStateService: NetworkStateService,
    private val raiseBrandsConverter: RaiseBrandsConverter
) : ShopBrandsRepository {

    private val firstPageBrands: MutableSharedFlow<DataPage<RaiseBrand>> = singleReplaySharedFlow()

    override suspend fun prefetch() {
        coroutineScope {
            launch { prefetchFirstPage() }
        }
    }

    private suspend fun prefetchFirstPage() {
        fetchBrands(EMPTY_QUERY, PageOffset.Loadable.FirstPage)
            .onSuccess { firstPageBrands.emit(it) }
    }

    override suspend fun getBrands(query: String, pageOffset: PageOffset.Loadable) =
        if (pageOffset is PageOffset.Loadable.FirstPage && query == EMPTY_QUERY) Result.success(firstPageBrands.first()) else fetchBrands(query, pageOffset)

    private suspend fun fetchBrands(
        query: String,
        pageOffset: PageOffset.Loadable,
    ) = networkStateService.recoverWithDispatcher(Dispatchers.IO) {
        raiseBrandsApi.getBrands(query = query, pageSize = BRANDS_PAGE_SIZE, pageNumber = pageOffset.getPageNumberOrThrow())
            .toDomain(pageOffset.getPageNumberOrThrow(), BRANDS_PAGE_SIZE)
    }

    private fun RaiseListBody<RaiseBrandResponse>.toDomain(pageIndex: Int, pageSize: Int): DataPage<RaiseBrand> {
        val offset = meta.toPageOffset(usedPageIndex = pageIndex, usedPageSize = pageSize)
        val items = data.mapNotNull(raiseBrandsConverter::convertBrandResponseToBrand)

        return DataPage(offset, items)
    }
}
