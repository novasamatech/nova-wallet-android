package io.novafoundation.nova.common.utils

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.io.InputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val PERCENTAGE_MULTIPLIER = 100.toBigDecimal()

fun BigDecimal.fractionToPercentage() = this * PERCENTAGE_MULTIPLIER

fun Float.percentageToFraction() = this / 100f
fun Double.percentageToFraction() = this / 100

infix fun Int.floorMod(divisor: Int) = Math.floorMod(this, divisor)

/**
 * Compares two BigDecimals taking into account only values but not scale unlike `==` operator
 */
infix fun BigDecimal.hasTheSaveValueAs(another: BigDecimal) = compareTo(another) == 0

val BigDecimal.isNonNegative: Boolean
    get() = signum() >= 0

fun BigInteger?.orZero(): BigInteger = this ?: BigInteger.ZERO

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

inline fun <T> Iterable<T>.filterToSet(predicate: (T) -> Boolean): Set<T> = filterTo(mutableSetOf(), predicate)

fun String.nullIfEmpty(): String? = if (isEmpty()) null else this

fun String.ensureSuffix(suffix: String) = if (endsWith(suffix)) this else this + suffix

private val NAMED_PATTERN_REGEX = "\\{([a-zA-z]+)\\}".toRegex()

fun String.formatNamed(vararg values: Pair<String, String>) = formatNamed(values.toMap())

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

fun <T> List<T>.modified(index: Int, modification: T): List<T> {
    val newList = this.toMutableList()

    newList[index] = modification

    return newList
}

inline fun <T, R> List<T>.mapToSet(mapper: (T) -> R): Set<R> = mapTo(mutableSetOf(), mapper)

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
