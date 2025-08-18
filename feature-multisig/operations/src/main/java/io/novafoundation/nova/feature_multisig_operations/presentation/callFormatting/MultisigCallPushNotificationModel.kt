package io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting

import io.novafoundation.nova.common.address.AddressModel

class MultisigCallPushNotificationModel(
    val text: String,
    val onBehalfOf: AddressModel?
)
