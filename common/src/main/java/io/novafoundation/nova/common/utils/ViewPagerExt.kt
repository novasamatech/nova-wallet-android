package io.novafoundation.nova.common.utils

import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2

fun ViewPager2.onCurrentPageViewChange(onChange: (View) -> Unit) {
    registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            val recyclerView = getChildAt(0) as? ViewGroup ?: return
            recyclerView.getChildAt(position)?.let { onChange(it) }
        }
    })
}
