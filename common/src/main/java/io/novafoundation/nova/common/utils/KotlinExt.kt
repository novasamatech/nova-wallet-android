package io.novafoundation.nova.common.utils

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.sqrt

private val PERCENTAGE_MULTIPLIER = 100.toBigDecimal()

fun BigDecimal.fractionToPercentage() = this * PERCENTAGE_MULTIPLIER

fun Double.percentageToFraction() = this / PERCENTAGE_MULTIPLIER.toDouble()
fun BigDecimal.percentageToFraction() = this.divide(PERCENTAGE_MULTIPLIER, MathContext.DECIMAL64)

infix fun Int.floorMod(divisor: Int) = Math.floorMod(this, divisor)

/**
 * Compares two BigDecimals taking into account only values but not scale unlike `==` operator
 */
infix fun BigDecimal.hasTheSaveValueAs(another: BigDecimal) = compareTo(another) == 0

fun BigInteger.intSqrt() = sqrt(toDouble()).toLong().toBigInteger()

val BigDecimal.isZero: Boolean
    get() = signum() == 0

val BigDecimal.isPositive: Boolean
    get() = signum() > 0

val BigDecimal.isNonNegative: Boolean
    get() = signum() >= 0

val BigInteger.isZero: Boolean
    get() = signum() == 0

fun BigInteger?.orZero(): BigInteger = this ?: BigInteger.ZERO
fun BigDecimal?.orZero(): BigDecimal = this ?: 0.toBigDecimal()

fun BigInteger.divideToDecimal(divisor: BigInteger, mathContext: MathContext = MathContext.DECIMAL64): BigDecimal {
    return toBigDecimal().divide(divisor.toBigDecimal(), mathContext)
}

fun Long.daysFromMillis() = TimeUnit.MILLISECONDS.toDays(this)

inline fun <T> Collection<T>.sumByBigInteger(extractor: (T) -> BigInteger) = fold(BigInteger.ZERO) { acc, element ->
    acc + extractor(element)
}

suspend operator fun <T> Deferred<T>.invoke() = await()

inline fun <T> List<T>.sumByBigDecimal(extractor: (T) -> BigDecimal) = fold(BigDecimal.ZERO) { acc, element ->
    acc + extractor(element)
}

inline fun <reified T> Any?.castOrNull(): T? {
    return this as? T
}

fun <K, V> Map<K, V>.reversed() = HashMap<V, K>().also { newMap ->
    entries.forEach { newMap[it.value] = it.key }
}

fun <T> Iterable<T>.isAscending(comparator: Comparator<T>) = zipWithNext().all { (first, second) -> comparator.compare(first, second) < 0 }

fun <T> Result<T>.requireException() = exceptionOrNull()!!

fun <T> Result<T>.requireValue() = getOrThrow()!!

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

fun generateLinearSequence(initial: Int, step: Int) = generateSequence(initial) { it + step }

fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) {
    this - item
} else {
    this + item
}

fun <T> List<T>.cycle(): Sequence<T> {
    var i = 0

    return generateSequence { this[i++ % this.size] }
}

inline fun <T> CoroutineScope.lazyAsync(context: CoroutineContext = EmptyCoroutineContext, crossinline producer: suspend () -> T) = lazy {
    async(context) { producer() }
}

inline fun CoroutineScope.invokeOnCompletion(crossinline action: () -> Unit) {
    coroutineContext[Job]?.invokeOnCompletion { action() }
}

inline fun <T> Iterable<T>.filterToSet(predicate: (T) -> Boolean): Set<T> = filterTo(mutableSetOf(), predicate)

fun String.nullIfEmpty(): String? = if (isEmpty()) null else this

fun String.ensureSuffix(suffix: String) = if (endsWith(suffix)) this else this + suffix

private val NAMED_PATTERN_REGEX = "\\{([a-zA-z]+)\\}".toRegex()

fun String.formatNamed(vararg values: Pair<String, String>) = formatNamed(values.toMap())

fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

/**
 * Replaces all parts in form of '{name}' to the corresponding value from values using 'name' as a key.
 *
 * @return formatted string
 */
fun String.formatNamed(values: Map<String, String>): String {
    return NAMED_PATTERN_REGEX.replace(this) { matchResult ->
        val argumentName = matchResult.groupValues.second()

        values[argumentName] ?: "null"
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

fun <K, V> Map<K, V>.inserted(key: K, value: V): Map<K, V> {
    return toMutableMap().apply { put(key, value) }
}

inline fun <T, R> Iterable<T>.mapToSet(mapper: (T) -> R): Set<R> = mapTo(mutableSetOf(), mapper)
inline fun <T, R : Any> Iterable<T>.mapNotNullToSet(mapper: (T) -> R?): Set<R> = mapNotNullTo(mutableSetOf(), mapper)

fun <T> List<T>.indexOfFirstOrNull(predicate: (T) -> Boolean) = indexOfFirst(predicate).takeIf { it >= 0 }

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
