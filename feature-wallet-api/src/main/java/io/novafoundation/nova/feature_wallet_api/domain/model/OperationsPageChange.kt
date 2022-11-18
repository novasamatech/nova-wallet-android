package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.data.model.DataPage

data class OperationsPageChange(
    val cursorPage: DataPage<Operation>,
    val accountChanged: Boolean
)
