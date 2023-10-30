package io.novafoundation.nova.common.utils

import android.widget.CompoundButton
import android.widget.EditText
import android.widget.RadioGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.input.Input
import io.novafoundation.nova.common.utils.input.isModifiable
import io.novafoundation.nova.common.utils.input.modifyInput
import io.novafoundation.nova.common.utils.input.valueOrNull
import io.novafoundation.nova.common.view.InsertableInputField
import io.novafoundation.nova.common.view.input.seekbar.Seekbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration

inline fun <T, R> Flow<List<T>>.mapList(crossinline mapper: suspend (T) -> R) = map { list ->
    list.map { item -> mapper(item) }
}

inline fun <T, R> Flow<Result<T>>.mapResult(crossinline mapper: suspend (T) -> R) = map { result ->
    result.map { item -> mapper(item) }
}

inline fun <T, R> Flow<List<T>>.mapListNotNull(crossinline mapper: suspend (T) -> R?) = map { list ->
    list.mapNotNull { item -> mapper(item) }
}

/**
 * Modifies flow so that it firstly emits [LoadingState.Loading] state.
 * Then emits each element from upstream wrapped into [LoadingState.Loaded] state.
 */
fun <T> Flow<T>.withLoading(): Flow<LoadingState<T>> {
    return map<T, LoadingState<T>> { LoadingState.Loaded(it) }
        .onStart { emit(LoadingState.Loading()) }
}

fun <T> MutableStateFlow<T>.setter(): (T) -> Unit {
    return { value = it }
}

fun <T> Flow<T>.withItemScope(parentScope: CoroutineScope): Flow<Pair<T, CoroutineScope>> {
    var currentScope: CoroutineScope? = null

    return map {
        currentScope?.cancel()
        currentScope = parentScope.childScope(supervised = true)
        it to requireNotNull(currentScope)
    }
}

/**
 * Modifies flow so that it firstly emits [ExtendedLoadingState.Loading] state.
 * Then emits each element from upstream wrapped into [ExtendedLoadingState.Loaded] state.
 * If exception occurs, emits [ExtendedLoadingState.Error] state.
 */
fun <T> Flow<T>.withSafeLoading(): Flow<ExtendedLoadingState<T>> {
    return map<T, ExtendedLoadingState<T>> { ExtendedLoadingState.Loaded(it) }
        .onStart { emit(ExtendedLoadingState.Loading) }
        .catch { emit(ExtendedLoadingState.Error(it)) }
}

suspend fun <T> Flow<LoadingState<T>>.firstOnLoad(): T = transform {
    collect {
        if (it is LoadingState.Loaded<T>) {
            emit(it.data)
        }
    }
}.first()

fun <T> List<Flow<T>>.mergeIfMultiple(): Flow<T> = when (size) {
    0 -> emptyFlow()
    1 -> first()
    else -> merge()
}

fun <K, V> List<Flow<Map<K, V>>>.accumulateMaps(): Flow<Map<K, V>> {
    return mergeIfMultiple()
        .runningFold(emptyMap()) { acc, directions -> acc + directions }
}

inline fun <T> withFlowScope(crossinline block: suspend (scope: CoroutineScope) -> Flow<T>): Flow<T> {
    return flowOfAll {
        val flowScope = CoroutineScope(coroutineContext)

        block(flowScope)
    }
}

fun <T1, T2> combineToPair(flow1: Flow<T1>, flow2: Flow<T2>): Flow<Pair<T1, T2>> = combine(flow1, flow2, ::Pair)

/**
 * Modifies flow so that it firstly emits [LoadingState.Loading] state for each element from upstream.
 * Then, it constructs new source via [sourceSupplier] and emits all of its items wrapped into [LoadingState.Loaded] state
 * Old suppliers are discarded as per [Flow.transformLatest] behavior
 */
fun <T, R> Flow<T>.withLoading(sourceSupplier: suspend (T) -> Flow<R>): Flow<LoadingState<R>> {
    return transformLatest { item ->
        emit(LoadingState.Loading<R>())

        val newSource = sourceSupplier(item).map { LoadingState.Loaded(it) }

        emitAll(newSource)
    }
}

private enum class InnerState {
    INITIAL_START, SECONDARY_START, IN_PROGRESS
}

/**
 * Modifies flow so that it firstly emits [LoadingState.Loading] state for each element from upstream.
 * Then, it constructs new source via [sourceSupplier] and emits all of its items wrapped into [LoadingState.Loaded] state
 * Old suppliers are discarded as per [Flow.transformLatest] behavior
 *
 * NOTE: This is a modified version of [withLoading] that is intended to be used ONLY with [SharingStarted.WhileSubscribed].
 * In particular, it does not emit loading state on second and subsequent re-subscriptions
 */
fun <T, R> Flow<T>.withLoadingShared(sourceSupplier: suspend (T) -> Flow<R>): Flow<ExtendedLoadingState<R>> {
    var state: InnerState = InnerState.INITIAL_START

    return transformLatest { item ->
        if (state != InnerState.SECONDARY_START) {
            emit(ExtendedLoadingState.Loading)
        }
        state = InnerState.IN_PROGRESS

        val newSource = sourceSupplier(item).map { ExtendedLoadingState.Loaded(it) }

        emitAll(newSource)
    }
        .catch { emit(ExtendedLoadingState.Error(it)) }
        .onCompletion { state = InnerState.SECONDARY_START }
}

suspend inline fun <reified T> Flow<ExtendedLoadingState<T>>.firstLoaded(): T = first { it.dataOrNull != null }.dataOrNull as T

/**
 * Modifies flow so that it firstly emits [LoadingState.Loading] state.
 * Then emits each element from upstream wrapped into [LoadingState.Loaded] state.
 *
 * NOTE: This is a modified version of [withLoading] that is intended to be used ONLY with [SharingStarted.WhileSubscribed].
 * In particular, it does not emit loading state on second and subsequent re-subscriptions
 */
fun <T> Flow<T>.withLoadingShared(): Flow<ExtendedLoadingState<T>> {
    var state: InnerState = InnerState.INITIAL_START

    return map<T, ExtendedLoadingState<T>> { ExtendedLoadingState.Loaded(it) }
        .onStart {
            if (state != InnerState.SECONDARY_START) {
                emit(ExtendedLoadingState.Loading)
            }
            state = InnerState.IN_PROGRESS
        }
        .catch { emit(ExtendedLoadingState.Error(it)) }
        .onCompletion { state = InnerState.SECONDARY_START }
}

/**
 * Similar to [Flow.takeWhile] but emits last element too
 */
fun <T> Flow<T>.takeWhileInclusive(predicate: suspend (T) -> Boolean) = transformWhile {
    emit(it)

    predicate(it)
}

inline fun <T, R> Flow<T?>.mapNullable(crossinline mapper: suspend (T) -> R): Flow<R?> {
    return map { it?.let { mapper(it) } }
}

/**
 * Modifies flow so that it firstly emits [LoadingState.Loading] state for each element from upstream.
 * Then, it constructs new source via [sourceSupplier] and emits all of its items wrapped into [LoadingState.Loaded] state
 * Old suppliers are discarded as per [Flow.transformLatest] behavior
 */
fun <T, R> Flow<T>.withLoadingSingle(sourceSupplier: suspend (T) -> R): Flow<LoadingState<R>> {
    return transformLatest { item ->
        emit(LoadingState.Loading())

        val newSource = LoadingState.Loaded(sourceSupplier(item))

        emit(newSource)
    }
}

fun <T, R> Flow<T>.withLoadingResult(source: suspend (T) -> Result<R>): Flow<ExtendedLoadingState<R>> {
    return transformLatest { item ->
        emit(ExtendedLoadingState.Loading)

        source(item)
            .onSuccess { emit(ExtendedLoadingState.Loaded(it)) }
            .onFailure { emit(ExtendedLoadingState.Error(it)) }
    }
}

fun <T> Flow<T>.asLiveData(scope: CoroutineScope): LiveData<T> {
    val liveData = MutableLiveData<T>()

    onEach {
        liveData.value = it
    }.launchIn(scope)

    return liveData
}

fun <T : Identifiable> Flow<List<T>>.diffed(): Flow<CollectionDiffer.Diff<T>> {
    return zipWithPrevious().map { (previous, new) ->
        CollectionDiffer.findDiff(newItems = new, oldItems = previous.orEmpty(), forceUseNewItems = false)
    }
}

fun <T> Flow<T>.zipWithPrevious(): Flow<Pair<T?, T>> = flow {
    var current: T? = null

    collect {
        emit(current to it)

        current = it
    }
}

private fun <K> MutableMap<K, CoroutineScope>.removeAndCancel(key: K) {
    remove(key)?.also(CoroutineScope::cancel)
}

fun <T : Identifiable, R> Flow<List<T>>.transformLatestDiffed(transform: suspend FlowCollector<R>.(value: T) -> Unit): Flow<R> = flow {
    val parentScope = CoroutineScope(coroutineContext)
    val itemScopes = mutableMapOf<String, CoroutineScope>()

    diffed().onEach { diff ->
        diff.removed.forEach { removedItem ->
            itemScopes.removeAndCancel(removedItem.identifier)
        }

        diff.newOrUpdated.forEach { newOrUpdatedItem ->
            itemScopes.removeAndCancel(newOrUpdatedItem.identifier)

            val chainScope = parentScope.childScope(supervised = false)
            itemScopes[newOrUpdatedItem.identifier] = chainScope

            chainScope.launch {
                transform(this@flow, newOrUpdatedItem)
            }
        }
    }.launchIn(parentScope)
}

fun <T> singleReplaySharedFlow() = MutableSharedFlow<T>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

fun <T> Flow<T>.inBackground() = flowOn(Dispatchers.Default)

fun <T> Flow<T>.nullOnStart(): Flow<T?> {
    return onStart<T?> { emit(null) }
}

fun InsertableInputField.bindTo(flow: MutableSharedFlow<String>, scope: CoroutineScope) {
    content.bindTo(flow, scope)
}

fun EditText.bindTo(
    flow: MutableSharedFlow<String>,
    scope: CoroutineScope,
    moveSelectionToEndOnInsertion: Boolean = false,
) {
    bindTo(flow, scope, moveSelectionToEndOnInsertion, toT = { it }, fromT = { it })
}

inline fun <T> EditText.bindTo(
    flow: MutableSharedFlow<T>,
    scope: CoroutineScope,
    moveSelectionToEndOnInsertion: Boolean = false,
    crossinline toT: suspend (String) -> T,
    crossinline fromT: suspend (T) -> String,
) {
    val textWatcher = onTextChanged {
        scope.launch {
            flow.emit(toT(it))
        }
    }

    scope.launch {
        flow.collect { input ->
            val inputString = fromT(input)
            if (text.toString() != inputString) {
                removeTextChangedListener(textWatcher)
                setText(inputString)
                if (moveSelectionToEndOnInsertion) {
                    moveSelectionToTheEnd()
                }
                addTextChangedListener(textWatcher)
            }
        }
    }
}

fun EditText.moveSelectionToTheEnd() {
    if (hasFocus()) {
        setSelection(text.length)
    }
}

inline fun <R> MutableStateFlow<Boolean>.withFlagSet(action: () -> R): R {
    value = true

    val result = action()

    value = false

    return result
}

fun CompoundButton.bindTo(flow: Flow<Boolean>, scope: CoroutineScope, callback: (Boolean) -> Unit) {
    var oldValue = isChecked

    scope.launch {
        flow.collect { newValue ->
            if (isChecked != newValue) {
                oldValue = newValue
                isChecked = newValue
            }
        }
    }

    setOnCheckedChangeListener { _, newValue ->
        if (oldValue != newValue) {
            oldValue = newValue
            callback(newValue)
        }
    }
}

fun CompoundButton.bindTo(flow: MutableStateFlow<Boolean>, scope: CoroutineScope) {
    scope.launch {
        flow.collect { newValue ->
            if (isChecked != newValue) {
                isChecked = newValue
            }
        }
    }

    setOnCheckedChangeListener { _, newValue ->
        if (flow.value != newValue) {
            flow.value = newValue
        }
    }
}

@JvmName("bindToInput")
fun CompoundButton.bindTo(flow: MutableStateFlow<Input<Boolean>>, scope: CoroutineScope) {
    scope.launch {
        flow.collect { newValue ->
            when (newValue) {
                Input.Disabled -> makeGone()

                is Input.Enabled -> {
                    if (isChecked != newValue.value) {
                        isChecked = newValue.value
                    }

                    makeVisible()
                    isEnabled = newValue.isModifiable
                }
            }
        }
    }

    setOnCheckedChangeListener { _, newValue ->
        if (flow.value.valueOrNull != newValue) {
            flow.modifyInput(newValue)
        }
    }
}

fun <T : Enum<T>> RadioGroup.bindTo(flow: MutableStateFlow<T>, scope: LifecycleCoroutineScope, valueToViewId: Map<T, Int>) {
    val viewIdToValue = valueToViewId.reversed()

    setOnCheckedChangeListener { _, checkedId ->
        val newValue = viewIdToValue.getValue(checkedId)

        if (flow.value != newValue) {
            flow.value = newValue
        }
    }

    scope.launchWhenResumed {
        flow.collect {
            val newCheckedId = valueToViewId.getValue(it)

            if (newCheckedId != checkedRadioButtonId) {
                check(newCheckedId)
            }
        }
    }
}

fun RadioGroup.bindTo(flow: MutableStateFlow<Int>, scope: LifecycleCoroutineScope) {
    setOnCheckedChangeListener { _, checkedId ->
        if (flow.value != checkedId) {
            flow.value = checkedId
        }
    }

    scope.launchWhenResumed {
        flow.collect {
            if (it != checkedRadioButtonId) {
                check(it)
            }
        }
    }
}

fun Seekbar.bindTo(flow: MutableStateFlow<Int>, scope: LifecycleCoroutineScope) {
    setOnProgressChangedListener { progress ->
        if (flow.value != progress) {
            flow.value = progress
        }
    }

    scope.launchWhenResumed {
        flow.collect {
            if (it != progress) {
                progress = it
            }
        }
    }
}

fun <T> Flow<T>.observe(
    scope: LifecycleCoroutineScope,
    collector: FlowCollector<T>,
) {
    scope.launchWhenResumed {
        collect(collector)
    }
}

fun MutableStateFlow<Boolean>.toggle() {
    value = !value
}

fun <T> flowOf(producer: suspend () -> T) = flow {
    emit(producer())
}

inline fun <T> flowOfAll(crossinline producer: suspend () -> Flow<T>): Flow<T> = flow {
    emitAll(producer())
}

inline fun <reified T> Iterable<Flow<T>>.combine(): Flow<List<T>> {
    return combineIdentity(this)
}

inline fun <reified T> combineIdentity(flows: Iterable<Flow<T>>): Flow<List<T>> {
    return combine(flows) { it.toList() }
}

fun <T> Collection<Flow<T>>.accumulate(): Flow<List<T>> {
    return accumulate(*this.toTypedArray())
}

fun <T> accumulate(vararg flows: Flow<T>): Flow<List<T>> {
    val flowsList = flows.mapIndexed { index, flow -> flow.map { index to flow } }
    val resultOfFlows = MutableList<T?>(flowsList.size) { null }
    return flowsList
        .merge()
        .map {
            resultOfFlows[it.first] = it.second.first()
            resultOfFlows.filterNotNull()
        }
}

fun <T> accumulateFlatten(vararg flows: Flow<List<T>>): Flow<List<T>> {
    return accumulate(*flows).map { it.flatten() }
}

fun <A, B, R> unite(flowA: Flow<A>, flowB: Flow<B>, transform: suspend (A?, B?) -> R): Flow<R> {
    var aResult: A? = null
    var bResult: B? = null

    return merge(
        flowA.onEach { aResult = it },
        flowB.onEach { bResult = it },
    ).map { transform(aResult, bResult) }
}

fun <A, B, C, R> unite(flowA: Flow<A>, flowB: Flow<B>, flowC: Flow<C>, transform: (A?, B?, C?) -> R): Flow<R> {
    var aResult: A? = null
    var bResult: B? = null
    var cResult: C? = null

    return merge(
        flowA.onEach { aResult = it },
        flowB.onEach { bResult = it },
        flowC.onEach { cResult = it },
    ).map { transform(aResult, bResult, cResult) }
}

fun <T> firstNonEmpty(
    vararg sources: Flow<List<T>>
): Flow<List<T>> = accumulate(*sources)
    .transform { collected ->
        val isAllLoaded = collected.size == sources.size
        val flattenResult: List<T> = collected.flatten()

        if (isAllLoaded || flattenResult.isNotEmpty()) {
            emit(flattenResult)
        }
    }

fun <T> Flow<T>.observeInLifecycle(
    lifecycleCoroutineScope: LifecycleCoroutineScope,
    observer: FlowCollector<T>,
) {
    lifecycleCoroutineScope.launchWhenResumed {
        collect(observer)
    }
}

fun <T> Map<out T, MutableStateFlow<Boolean>>.checkEnabled(key: T) = get(key)?.value ?: false

suspend inline fun <reified T> Flow<T?>.firstNotNull(): T = first { it != null } as T

inline fun <T, R> Flow<IndexedValue<T>>.mapLatestIndexed(crossinline transform: suspend (T) -> R): Flow<IndexedValue<R>> {
    return mapLatest { IndexedValue(it.index, transform(it.value)) }
}

/**
 * Emits first element from upstream and then emits last element emitted by upstream during specified time window
 *
 * ```
 * flow {
 *  for (num in 1..15) {
 *      emit(num)
 *      delay(25)
 *  }
 * }.throttleLast(100)
 *  .onEach { println(it) }
 *  .collect()  // Prints 1, 5, 9, 13, 15
 *
 * ```
 */
fun <T> Flow<T>.throttleLast(delay: Duration): Flow<T> = this
    .conflate()
    .transform {
        emit(it)
        delay(delay)
    }
