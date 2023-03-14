package io.novafoundation.nova.common.mixin.impl

import io.novafoundation.nova.common.base.BaseFragmentMixin
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.showBrowser

fun BaseFragmentMixin<*>.observeBrowserEvents(mixin: Browserable) {
    mixin.openBrowserEvent.observeEvent(providedContext::showBrowser)
}
