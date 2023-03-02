package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.common.view.showLoadingState

class DelegateLabelModel(
    val icon: DelegateIcon,
    val address: String,
    val name: String?,
    val type: DelegateTypeModel?
)

fun DelegateLabelModel.nameOrAddress(): String {
    return name ?: address
}

fun TableCellView.setDelegateLabelModel(model: DelegateLabelModel) {
    image.makeVisible()
    image.setDelegateIcon(icon = model.icon, imageLoader = imageLoader, squareCornerRadiusDp = 4)

    showValue(model.nameOrAddress())
}

fun TableCellView.setDelegateLabelState(state: ExtendedLoadingState<DelegateLabelModel>) {
    showLoadingState(state, ::setDelegateLabelModel)
}
