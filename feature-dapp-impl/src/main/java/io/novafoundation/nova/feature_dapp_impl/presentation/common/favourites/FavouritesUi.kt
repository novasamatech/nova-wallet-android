package io.novafoundation.nova.feature_dapp_impl.presentation.common.favourites

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationAwaitable
import io.novafoundation.nova.common.view.dialog.warningDialog
import io.novafoundation.nova.feature_dapp_impl.R

typealias RemoveFavouritesPayload = Unit

fun BaseFragment<*>.setupRemoveFavouritesConfirmation(awaitableMixin: ConfirmationAwaitable<RemoveFavouritesPayload>) {
    awaitableMixin.awaitableActionLiveData.observeEvent {
        warningDialog(
            context = requireContext(),
            onConfirm = { it.onSuccess(Unit) },
            confirmTextRes =  R.string.common_remove,
            onCancel = it.onCancel
        ) {
            setTitle(R.string.dapp_favourites_remove_title)
        }
    }
}
