package io.novafoundation.nova.runtime.state

import kotlinx.coroutines.flow.Flow

interface SelectedOptionSharedState<out T> {

    val selectedOption: Flow<T>
}
