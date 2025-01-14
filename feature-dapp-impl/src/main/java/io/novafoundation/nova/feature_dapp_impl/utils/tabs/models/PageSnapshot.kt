package io.novafoundation.nova.feature_dapp_impl.utils.tabs.models

class PageSnapshot(
    val pageName: String?,
    val pageIconPath: String?,
    val pagePicturePath: String?
) {

    companion object;
}

fun PageSnapshot.Companion.fromName(name: String) = PageSnapshot(
    pageName = name,
    pageIconPath = null,
    pagePicturePath = null
)
