package io.novafoundation.nova.feature_dapp_impl.presentation.common

import io.novafoundation.nova.feature_dapp_api.data.model.DappCategory
import io.novafoundation.nova.feature_dapp_impl.presentation.main.model.DAppCategoryModel


fun dappCategoryToUi(dappCategory: DappCategory, isSelected: Boolean): DAppCategoryModel {
    return DAppCategoryModel(
        id = dappCategory.id,
        name = dappCategory.name,
        selected = isSelected,
        iconUrl = dappCategory.iconUrl
    )
}
