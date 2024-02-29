package io.novafoundation.nova.feature_staking_api.presentation.nominationPools.display

import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.view.TableCellView
import io.novasama.substrate_sdk_android.runtime.AccountId

class PoolDisplayModel(
    val icon: Icon,
    val title: String,
    val poolAccountId: AccountId,
    val address: String
)

fun TableCellView.showPool(poolDisplayModel: PoolDisplayModel) {
    loadImage(poolDisplayModel.icon)
    showValue(poolDisplayModel.title)
}
