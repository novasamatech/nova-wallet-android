package io.novafoundation.nova.feature_dapp_impl.utils.tabs.models

interface OnPageChangedCallback {

    fun onPageChanged(tabId: String, url: String, title: String?)
}
