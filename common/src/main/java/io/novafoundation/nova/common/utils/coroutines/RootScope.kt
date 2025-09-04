package io.novafoundation.nova.common.utils.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

class RootScope : CoroutineScope by MainScope()
