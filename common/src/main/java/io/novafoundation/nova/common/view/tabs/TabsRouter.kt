package io.novafoundation.nova.common.view.tabs

import androidx.lifecycle.Lifecycle

interface TabsRouter {

    fun openTabAt(index: Int)

    fun listenCurrentTab(lifecycle: Lifecycle, onChange: (index: Int) -> Unit)
}
