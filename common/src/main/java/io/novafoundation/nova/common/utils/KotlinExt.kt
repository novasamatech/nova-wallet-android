package io.novafoundation.nova.common.utils

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.runningFold
import org.web3j.utils.Numeric
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.sqrt

private val PERCENTAGE_MULTIPLIER = 100.toBigDecimal()

fun BigDecimal.fractionToPercentage() = this * PERCENTAGE_MULTIPLIER

fun Double.percentageToFraction() = this / PERCENTAGE_MULTIPLIER.toDouble()
fun BigDecimal.percentageToFraction() = this.divide(PERCENTAGE_MULTIPLIER, MathContext.DECIMAL64)

infix fun Int.floorMod(divisor: Int) = Math.floorMod(this, divisor)

fun Double.ceil(): Double = kotlin.math.ceil(this)

@Suppress("UNCHECKED_CAST")
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> {
    return fold(
        onSuccess = { transform(it) },
        onFailure = { this as Result<R> }
    )
}

inline fun <R> Result<R>.finally(transform: () -> Unit): Result<R> {
    onSuccess { transform() }
    onFailure { transform() }
    return this
}

@OptIn(ExperimentalContracts::class)
inline fun <T> Result<T>.mapError(transform: (throwable: Throwable) -> Throwable): Result<T> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }
    return when (val exception = this.exceptionOrNull()) {
        null -> this
        else -> Result.failure(transform(exception))
    }
}

inline fun <T, reified E : Throwable> Result<T>.mapErrorNotInstance(transform: (throwable: Throwable) -> Throwable): Result<T> {
    return mapError { throwable ->
        if (throwable !is E) {
            transform(throwable)
        } else {
            throwable
        }
    }
}

fun <K, V> List<Flow<Pair<K, V>>>.toMultiSubscription(expectedSize: Int): Flow<Map<K, V>> {
    return mergeIfMultiple()
        .runningFold(emptyMap<K, V>()) { accumulator, tokenIdWithBalance ->
            accumulator + tokenIdWithBalance
        }
        .filter { it.size == expectedSize }
}

inline fun <reified E : Enum<E>> enumValueOfOrNull(raw: String): E? = runCatching { enumValueOf<E>(raw) }.getOrNull()

inline fun <K, V> List<V>.associateByMultiple(keysExtractor: (V) -> Iterable<K>): Map<K, V> {
    val destination = LinkedHashMap<K, V>()

    for (element in this) {
        val keys = keysExtractor(element)

        for (key in keys) {
            destination[key] = element
        }
    }

    return destination
}

suspend fun <T, R> Iterable<T>.mapAsync(operation: suspend (T) -> R): List<R> {
    return coroutineScope {
        map { async { operation(it) } }
    }.awaitAll()
}

fun ByteArray.startsWith(prefix: ByteArray): Boolean {
    if (prefix.size > size) return false

    prefix.forEachIndexed { index, byte ->
        if (get(index) != byte) return false
    }

    return true
}

fun ByteArray.windowed(windowSize: Int): List<ByteArray> {
    require(windowSize > 0) {
        "Window size should be positive"
    }

    val result = mutableListOf<ByteArray>()

    var i = 0

    while (i < size) {
        val copyStart = i
        val copyEnd = (i + windowSize).coerceAtMost(size)

        result.add(copyOfRange(copyStart, copyEnd))

        i += windowSize
    }

    return result
}

/**
 * Compares two BigDecimals taking into account only values but not scale unlike `==` operator
 */
infix fun BigDecimal.hasTheSaveValueAs(another: BigDecimal) = compareTo(another) == 0

fun BigInteger.intSqrt() = sqrt(toDouble()).toLong().toBigInteger()

fun <T> unsafeLazy(initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)

fun <T> MutableSet<T>.toImmutable(): Set<T> = Collections.unmodifiableSet(this)

operator fun BigInteger.times(double: Double): BigInteger = toBigDecimal().multiply(double.toBigDecimal()).toBigInteger()

val BigDecimal.isZero: Boolean
    get() = signum() == 0

val BigDecimal.isPositive: Boolean
    get() = signum() > 0

val BigDecimal.isNonNegative: Boolean
    get() = signum() >= 0

val BigInteger.isNonPositive: Boolean
    get() = signum() <= 0

val BigInteger.isNonNegative: Boolean
    get() = signum() >= 0

val BigInteger.isZero: Boolean
    get() = signum() == 0

fun BigInteger?.orZero(): BigInteger = this ?: BigInteger.ZERO
fun BigDecimal?.orZero(): BigDecimal = this ?: 0.toBigDecimal()

fun Double?.orZero(): Double = this ?: 0.0

fun BigInteger.divideToDecimal(divisor: BigInteger, mathContext: MathContext = MathContext.DECIMAL64): BigDecimal {
    return toBigDecimal().divide(divisor.toBigDecimal(), mathContext)
}

fun BigInteger.atLeastZero() = coerceAtLeast(BigInteger.ZERO)

fun BigDecimal.atLeastZero() = coerceAtLeast(BigDecimal.ZERO)

fun Long.daysFromMillis() = TimeUnit.MILLISECONDS.toDays(this)

inline fun <T> Collection<T>.sumByBigInteger(extractor: (T) -> BigInteger) = fold(BigInteger.ZERO) { acc, element ->
    acc + extractor(element)
}

fun Iterable<BigInteger>.sum() = sumOf { it }

fun String.decodeEvmQuantity(): BigInteger {
    return Numeric.decodeQuantity(this)
}

suspend operator fun <T> Deferred<T>.invoke() = await()

inline fun <T> Iterable<T>.sumByBigDecimal(extractor: (T) -> BigDecimal) = fold(BigDecimal.ZERO) { acc, element ->
    acc + extractor(element)
}

inline fun <reified T> Any?.castOrNull(): T? {
    return this as? T
}

fun ByteArray.padEnd(expectedSize: Int, padding: Byte = 0): ByteArray {
    if (size >= expectedSize) return this

    val padded = ByteArray(expectedSize) { padding }
    return copyInto(padded)
}

fun <K, V> Map<K, V>.reversed() = HashMap<V, K>().also { newMap ->
    entries.forEach { newMap[it.value] = it.key }
}

fun <T> Iterable<T>.isAscending(comparator: Comparator<T>) = zipWithNext().all { (first, second) -> comparator.compare(first, second) < 0 }

fun <T> Result<T>.requireException() = exceptionOrNull()!!

fun <T> Result<T>.requireValue() = getOrThrow()!!

/**
 * Given a list finds a partition point in O(log2(N)) given that there is only a single partition point present.
 * That is, there is only a single place in the whole array where the value of [partition] changes from false to true
 *
 * @return index of the first element where invocation of [partition] returns true. Returns null in case there is no such elements in the list
 */
inline fun <T> List<T>.findPartitionPoint(partition: (T) -> Boolean): Int? {
    if (isEmpty()) return null

    var lowIdx = 0
    var highIdx = size - 1

    while (highIdx - lowIdx > 1) {
        val midIdx = (lowIdx + highIdx) / 2

        val midValue = get(midIdx)
        val isPartitionTrue = partition(midValue)

        if (isPartitionTrue) {
            highIdx = midIdx
        } else {
            lowIdx = midIdx + 1
        }
    }

    val isLowTrue = partition(get(lowIdx))
    if (isLowTrue) return lowIdx

    val isHighTrue = partition(get(highIdx))
    if (isHighTrue) return highIdx

    return null
}

/**
 * @see [findPartitionPoint]
 */
fun List<Boolean>.findPartitionPoint(): Int? {
    return findPartitionPoint { it }
}

fun <T> Result<T>.mapFailure(transform: (Throwable) -> Throwable): Result<T> {
    return when {
        isFailure -> Result.failure(transform(requireException()))
        else -> this
    }
}

fun InputStream.readText() = bufferedReader().use { it.readText() }

fun <T> List<T>.second() = get(1)

fun <E : Enum<E>> Collection<Enum<E>>.anyIs(value: E) = any { it == value }

fun Int.quantize(factor: Int) = this - this % factor

@Suppress("UNCHECKED_CAST")
inline fun <K, V, R> Map<K, V>.mapValuesNotNull(crossinline mapper: (Map.Entry<K, V>) -> R?): Map<K, R> {
    return mapValues(mapper)
        .filterNotNull()
}

@Suppress("UNCHECKED_CAST")
inline fun <K, V> Map<K, V?>.filterNotNull(): Map<K, V> {
    return filterValues { it != null } as Map<K, V>
}

inline fun <T, R> Array<T>.tryFindNonNull(transform: (T) -> R?): R? {
    for (item in this) {
        val transformed = transform(item)

        if (transformed != null) return transformed
    }

    return null
}

fun String.bigIntegerFromHex() = removeHexPrefix().toBigInteger(16)
fun String.intFromHex() = removeHexPrefix().toInt(16)

/**
 * Complexity: O(n * log(n))
 */
// TODO possible to optimize
fun List<Double>.median(): Double = sorted().let {
    val middleRight = it[it.size / 2]
    val middleLeft = it[(it.size - 1) / 2] // will be same as middleRight if list size is odd

    (middleLeft + middleRight) / 2
}

fun Collection<BigInteger>.average(): BigInteger {
    if (isEmpty()) throw NoSuchFieldException("Collection is empty")

    return sum() / size.toBigInteger()
}

fun generateLinearSequence(initial: Int, step: Int) = generateSequence(initial) { it + step }

fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) {
    this - item
} else {
    this + item
}

fun <T> List<T>.cycle(): Sequence<T> {
    if (isEmpty()) return emptySequence()

    var i = 0

    return generateSequence { this[i++ % this.size] }
}

fun <T> List<T>.cycleMultiple(): Sequence<T> {
    if (isEmpty()) return emptySequence()
    if (size == 1) return sequenceOf(single())

    var i = 0

    return generateSequence { this[i++ % this.size] }
}

inline fun <reified R> List<*>.findIsInstanceOrNull(): R? {
    return find { it is R } as? R
}

inline fun <T> CoroutineScope.lazyAsync(context: CoroutineContext = EmptyCoroutineContext, crossinline producer: suspend () -> T) = lazy {
    async(context) { producer() }
}

inline fun CoroutineScope.invokeOnCompletion(crossinline action: () -> Unit) {
    coroutineContext[Job]?.invokeOnCompletion { action() }
}

inline fun <T> Iterable<T>.filterToSet(predicate: (T) -> Boolean): Set<T> = filterTo(mutableSetOf(), predicate)

fun String?.nullIfEmpty(): String? = if (isNullOrEmpty()) null else this

fun String.ensureSuffix(suffix: String) = if (endsWith(suffix)) this else this + suffix

private val NAMED_PATTERN_REGEX = "\\{([a-zA-z]+)\\}".toRegex()

fun String.formatNamed(vararg values: Pair<String, String>) = formatNamed(values.toMap())

fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

private const val CAMEL_CASE_REGEX_STRING = "(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])" // https://stackoverflow.com/a/7599674
private val CAMEL_CASE_REGEX = CAMEL_CASE_REGEX_STRING.toRegex()

private const val SNAKE_CASE_REGEX_STRING = "_"
fun String.splitCamelCase() = CAMEL_CASE_REGEX.split(this)

fun String.splitSnakeCase() = split(SNAKE_CASE_REGEX_STRING)

fun String.splitSnakeOrCamelCase() = if (contains(SNAKE_CASE_REGEX_STRING)) {
    splitSnakeCase()
} else {
    splitCamelCase()
}

/**
 * Replaces all parts in form of '{name}' to the corresponding value from values using 'name' as a key.
 *
 * @return formatted string
 */
fun String.formatNamed(values: Map<String, String>): String {
    return formatNamed(values, onUnknownSecret = { "null" })
}

fun String.formatNamedOrThrow(values: Map<String, String>): String {
    return formatNamed(values, onUnknownSecret = { throw IllegalArgumentException("Unknown secret: $it") })
}

fun String.formatNamed(values: Map<String, String>, onUnknownSecret: (secretName: String) -> String): String {
    return NAMED_PATTERN_REGEX.replace(this) { matchResult ->
        val argumentName = matchResult.groupValues.second()

        values[argumentName] ?: onUnknownSecret(argumentName)
    }
}

inline fun <T> T?.defaultOnNull(lazyProducer: () -> T): T {
    return this ?: lazyProducer()
}

fun <T> List<T>.modified(modification: T, condition: (T) -> Boolean): List<T> {
    return modified(indexOfFirst(condition), modification)
}

fun <T> List<T>.removed(condition: (T) -> Boolean): List<T> {
    return toMutableList().apply { removeAll(condition) }
}

fun <T> List<T>.added(toAdd: T): List<T> {
    return toMutableList().apply { add(toAdd) }
}

fun <T> List<T>.prepended(toPrepend: T): List<T> {
    return toMutableList().apply { add(0, toPrepend) }
}

fun <T> List<T>.modified(index: Int, modification: T): List<T> {
    val newList = this.toMutableList()

    newList[index] = modification

    return newList
}

fun <T> Set<T>.added(toAdd: T): Set<T> {
    return toMutableSet().apply { add(toAdd) }
}

fun <K, V> Map<K, V>.inserted(key: K, value: V): Map<K, V> {
    return toMutableMap().apply { put(key, value) }
}

inline fun <T, R> Iterable<T>.mapToSet(mapper: (T) -> R): Set<R> = mapTo(mutableSetOf(), mapper)

inline fun <T, R> Iterable<T>.foldToSet(mapper: (T) -> Iterable<R>): Set<R> = fold(mutableSetOf()) { acc, value ->
    acc += mapper(value)
    acc
}

inline fun <T, R : Any> Iterable<T>.mapNotNullToSet(mapper: (T) -> R?): Set<R> = mapNotNullTo(mutableSetOf(), mapper)

fun <T> List<T>.indexOfFirstOrNull(predicate: (T) -> Boolean) = indexOfFirst(predicate).takeIf { it >= 0 }

fun <T> List<T>.indexOfOrNull(value: T) = indexOf(value).takeIf { it >= 0 }

@Suppress("IfThenToElvis")
fun ByteArray?.optionalContentEquals(other: ByteArray?): Boolean {
    return if (this == null) {
        other == null
    } else {
        this.contentEquals(other)
    }
}

fun Uri.Builder.appendNullableQueryParameter(name: String, value: String?) = apply {
    value?.let { appendQueryParameter(name, value) }
}

fun ByteArray.dropBytes(count: Int) = copyOfRange(count, size)
fun ByteArray.dropBytesLast(count: Int) = copyOfRange(0, size - count)

fun ByteArray.chunked(count: Int): List<ByteArray> = toList().chunked(count).map { it.toByteArray() }

fun buildByteArray(block: (ByteArrayOutputStream) -> Unit): ByteArray = ByteArrayOutputStream().apply {
    block(this)
}.toByteArray()

fun String.toUuid() = UUID.fromString(this)

val Int.kilobytes: BigInteger
    get() = this.toBigInteger() * 1024.toBigInteger()

operator fun ByteArray.compareTo(other: ByteArray): Int {
    if (size != other.size) {
        return size - other.size
    }

    for (i in 0 until size) {
        val result = this[i].compareTo(other[i])

        if (result != 0) {
            return result
        }
    }

    return 0
}

fun ByteArrayComparator() = Comparator<ByteArray> { a, b -> a.compareTo(b) }

inline fun CoroutineScope.withChildScope(action: CoroutineScope.() -> Unit) {
    val childScope = childScope()

    action(childScope)

    childScope.cancel()
}

fun <T> List<T>.associateWithIndex() = withIndex().associateBy(keySelector = { it.value }, valueTransform = { it.index })

/**
 * @return true if action returned true at least once, false otherwise
 */
inline fun repeatUntil(maxTimes: Int?, action: () -> Boolean): Boolean {
    var times = 0

    while (maxTimes == null || times < maxTimes) {
        if (action()) return true

        times++
    }

    return false
}

fun Date.atTheBeginningOfTheDay(): Date {
    val calendar = Calendar.getInstance().apply {
        time = this@atTheBeginningOfTheDay
        resetDay()
    }

    return calendar.toDate()
}

fun Date.atTheEndOfTheDay(): Date {
    val calendar = Calendar.getInstance().apply {
        time = this@atTheEndOfTheDay
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    return calendar.toDate()
}

fun Date.atTheNextDay(): Date {
    val calendar = Calendar.getInstance().apply {
        time = this@atTheNextDay
        add(Calendar.DAY_OF_MONTH, 1)
        resetDay()
    }

    return calendar.toDate()
}

fun Calendar.toDate(): Date = Date(time.time)

fun Calendar.resetDay() {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}
