package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.presentation.ExtendedLoadingState
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.common.view.showLoadingState

class DelegateLabelModel(
    val icon: DelegateIcon,
    val addressModel: AddressModel,
    val type: DelegateTypeModel?
)

fun TableCellView.setDelegateLabelModel(model: DelegateLabelModel) {
    image.makeVisible()
    image.setDelegateIcon(model.icon, imageLoader, 4)

    showValue(model.addressModel.nameOrAddress)
}

fun TableCellView.setDelegateLabelState(state: ExtendedLoadingState<DelegateLabelModel>) {
    showLoadingState(state, ::setDelegateLabelModel)
}
