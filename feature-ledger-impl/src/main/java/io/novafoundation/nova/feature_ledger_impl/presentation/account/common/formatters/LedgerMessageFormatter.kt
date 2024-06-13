package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.formatters

import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.view.AlertModel

interface LedgerMessageFormatter {

    enum class MessageKind {
        APP_NOT_OPEN, OTHER
    }

    suspend fun appName(): String

    context(Browserable.Presentation)
    suspend fun alertForKind(
        messageKind: MessageKind,
    ): AlertModel?
}
