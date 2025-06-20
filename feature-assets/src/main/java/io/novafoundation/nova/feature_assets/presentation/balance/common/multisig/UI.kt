package io.novafoundation.nova.feature_assets.presentation.balance.common.multisig

import io.novafoundation.nova.common.base.BaseScreenMixin
import io.novafoundation.nova.common.view.bottomSheet.action.observeActionBottomSheet

fun BaseScreenMixin<*>.observeMultisigCheck(mixin: MultisigRestrictionCheckMixin) {
    observeActionBottomSheet(mixin.actionLauncher)
}
