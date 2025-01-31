package io.novafoundation.nova.feature_account_api.presenatation.mixin.importType

import io.novafoundation.nova.common.base.BaseFragment

fun BaseFragment<*, *>.setupImportTypeChooser(mixin: ImportTypeChooserMixin) {
    mixin.showChooserEvent.observeEvent {
        ImportTypeChooserBottomSheet(
            context = requireContext(),
            onChosen = it.onChosen,
            allowedSources = it.allowedTypes
        ).show()
    }
}
