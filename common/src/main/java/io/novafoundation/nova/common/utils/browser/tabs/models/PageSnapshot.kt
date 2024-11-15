package io.novafoundation.nova.common.utils.browser.tabs.models

class PageSnapshot(
    val pageName: String?,
    val pageIconPath: String?,
    val pagePicturePath: String?
) {

    companion object;
}

fun PageSnapshot.Companion.withNameOnly(name: String) = PageSnapshot(
    pageName = name,
    pageIconPath = null,
    pagePicturePath = null
)

