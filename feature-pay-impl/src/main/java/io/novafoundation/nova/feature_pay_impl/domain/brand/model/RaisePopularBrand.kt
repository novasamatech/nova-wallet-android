package io.novafoundation.nova.feature_pay_impl.domain.brand.model

import io.novafoundation.nova.common.utils.Identifiable

data class RaisePopularBrand(
    val raiseBrand: RaiseBrand,
    val name: String
) : Identifiable {
    override val identifier: String = raiseBrand.id
}
