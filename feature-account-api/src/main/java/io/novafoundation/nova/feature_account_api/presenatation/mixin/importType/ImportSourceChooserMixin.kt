package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.importType

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportType

interface ImportTypeChooserMixin {

    class Payload(
        val allowedTypes: Set<ImportType> = ImportType.values().toSet(),
        val onChosen: (ImportType) -> Unit
    )

    val showChooserEvent: LiveData<Event<Payload>>

    interface Presentation : ImportTypeChooserMixin {

        fun showChooser(payload: Payload)
    }
}
