package io.novafoundation.nova.common.utils

import com.google.android.material.tabs.TabLayout

fun TabLayout.createTab(textResId: Int) {
    val tab = newTab()
    tab.setText(textResId)

    addTab(tab)
}
