package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.data.model.CursorPage

data class OperationsPageChange(
    val cursorPage: CursorPage<Operation>,
    val accountChanged: Boolean
)
