package io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.images.Icon

data class MultisigCallPreviewModel(
    val title: String,
    val subtitle: String?,
    val primaryValue: String?,
    val icon: Icon,
    val onBehalfOf: AddressModel?
)
