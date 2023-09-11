package io.novafoundation.nova.feature_staking_api.presentation.nominationPools.display

import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.view.TableCellView
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class PoolDisplayModel(
    val icon: Icon,
    val title: String,
    val poolAccountId: AccountId,
)

fun TableCellView.showPool(poolDisplayModel: PoolDisplayModel) {
    loadImage(poolDisplayModel.icon)
    showValue(poolDisplayModel.title)
}
