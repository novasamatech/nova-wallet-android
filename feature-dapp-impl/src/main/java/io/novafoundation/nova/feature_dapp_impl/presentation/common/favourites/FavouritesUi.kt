package io.novafoundation.nova.feature_dapp_impl.presentation.common.favourites

import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationAwaitable
import io.novafoundation.nova.common.view.dialog.warningDialog
import io.novafoundation.nova.feature_dapp_impl.R

typealias RemoveFavouritesPayload = String // dApp title

fun BaseFragmentMixin<*>.setupRemoveFavouritesConfirmation(awaitableMixin: ConfirmationAwaitable<RemoveFavouritesPayload>) {
    awaitableMixin.awaitableActionLiveData.observeEvent {
        warningDialog(
            context = providedContext,
            onPositiveClick = { it.onSuccess(Unit) },
            positiveTextRes = R.string.common_remove,
            onNegativeClick = it.onCancel
        ) {
            setTitle(R.string.dapp_favourites_remove_title)

            setMessage(providedContext.getString(R.string.dapp_favourites_remove_description, it.payload))
        }
    }
}
