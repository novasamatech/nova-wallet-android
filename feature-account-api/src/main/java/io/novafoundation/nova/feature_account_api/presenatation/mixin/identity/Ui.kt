package io.novafoundation.nova.feature_account_api.presenatation.mixin.identity

import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.common.mixin.impl.observeBrowserEvents
import io.novafoundation.nova.common.utils.sendEmailIntent

fun BaseFragmentMixin<*>.setupIdentityMixin(
    mixin: IdentityMixin,
    view: IdentityView
) {
    view.onEmailClicked { mixin.emailClicked() }
    view.onWebClicked { mixin.webClicked() }
    view.onTwitterClicked { mixin.twitterClicked() }

    observeBrowserEvents(mixin)

    mixin.openEmailEvent.observeEvent {
        providedContext.sendEmailIntent(it)
    }

    mixin.identityFlow.observe(view::setModelOrHide)
}
