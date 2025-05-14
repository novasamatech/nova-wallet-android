package io.novafoundation.nova.feature_pay_impl.data.raise.brands

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.common.data.model.getPageNumberOrThrow
import io.novafoundation.nova.common.utils.NetworkStateService
import io.novafoundation.nova.common.utils.recoverWithDispatcher
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.feature_pay_impl.data.ShopBrandsRepository
import io.novafoundation.nova.feature_pay_impl.domain.brand.model.RaiseBrand
import io.novafoundation.nova.feature_pay_impl.domain.brand.model.RaisePopularBrand
import io.novafoundation.nova.feature_pay_impl.data.raise.brands.network.RaiseBrandsApi
import io.novafoundation.nova.feature_pay_impl.data.raise.brands.network.RaisePopularBrandsApi
import io.novafoundation.nova.feature_pay_impl.data.raise.brands.network.model.RaiseBrandResponse
import io.novafoundation.nova.feature_pay_impl.data.raise.common.RaiseListBody
import io.novafoundation.nova.feature_pay_impl.data.raise.common.toPageOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private const val BRANDS_PAGE_SIZE = 50
private const val EMPTY_QUERY = ""

class RealShopBrandsRepository(
    private val raiseBrandsApi: RaiseBrandsApi,
    private val raisePopularBrandsApi: RaisePopularBrandsApi,
    private val networkStateService: NetworkStateService,
    private val raiseBrandsConverter: RaiseBrandsConverter
) : ShopBrandsRepository {

    private val popularBrands: MutableSharedFlow<List<RaisePopularBrand>> = singleReplaySharedFlow()

    private suspend fun prefetchPopularBrands() {
        runCatching {
            withContext(Dispatchers.IO) {
                val remoteConfigPopularBrands = raisePopularBrandsApi.getPopularBrands()

                val idsQuery = remoteConfigPopularBrands.joinToString(",") { it.id }
                val raisePopularBrands = raiseBrandsApi.getBrandsByIds(idsQuery).data
                    .mapNotNull { raiseBrandsConverter.convertBrandResponseToBrand(it) }

                remoteConfigPopularBrands
                    .mapNotNull { popularBrand ->
                        raisePopularBrands.find { it.id == popularBrand.id }
                            ?.let { RaisePopularBrand(it, popularBrand.name) }
                    }
                    .let { popularBrands.emit(it) }
            }
        }
    }

    override suspend fun getBrands(query: String, pageOffset: PageOffset.Loadable) = fetchBrands(query, pageOffset)

    override suspend fun getPopularBrands() = Result.success(popularBrands.first())

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
