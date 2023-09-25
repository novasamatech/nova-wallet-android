package io.novafoundation.nova.common.validation

import io.novafoundation.nova.common.utils.requireException
import io.novafoundation.nova.common.utils.requireValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface Validation<T, S> {

    suspend fun validate(value: T): ValidationStatus<S>
}

fun <S> validOrWarning(
    condition: Boolean,
    lazyReason: () -> S,
): ValidationStatus<S> = if (condition) {
    ValidationStatus.Valid()
} else {
    ValidationStatus.NotValid(DefaultFailureLevel.WARNING, lazyReason())
}

inline fun <S> validOrError(
    isValid: Boolean,
    lazyReason: () -> S,
): ValidationStatus<S> = if (isValid) {
    ValidationStatus.Valid()
} else {
    ValidationStatus.NotValid(DefaultFailureLevel.ERROR, lazyReason())
}

fun <S> validationError(reason: S) = ValidationStatus.NotValid(DefaultFailureLevel.ERROR, reason)
fun <S> validationWarning(reason: S) = ValidationStatus.NotValid(DefaultFailureLevel.WARNING, reason)
fun <S> valid() = ValidationStatus.Valid<S>()

inline infix fun <E> Boolean.isTrueOrError(error: () -> E) = validOrError(this, error)
inline infix fun <E> Boolean.isFalseOrError(error: () -> E) = this.not().isTrueOrError(error)

infix fun <E> Boolean.isTrueOrWarning(warning: () -> E) = validOrWarning(this, warning)
infix fun <E> Boolean.isFalseOrWarning(warning: () -> E) = this.not().isTrueOrWarning(warning)

sealed class ValidationStatus<S> {

    class Valid<S> : ValidationStatus<S>()

    class NotValid<S>(val level: Level, val reason: S) : ValidationStatus<S>() {

        interface Level {
            val value: Int

            operator fun compareTo(other: Level): Int = value - other.value
        }
    }
}

@JvmName("validationErrorReceiver")
fun <T> T.validationError(): ValidationStatus.NotValid<T> = ValidationStatus.NotValid(DefaultFailureLevel.ERROR, this)

@JvmName("validationWarningReceiver")
fun <T> T.validationWarning(): ValidationStatus.NotValid<T> = ValidationStatus.NotValid(DefaultFailureLevel.WARNING, this)

enum class DefaultFailureLevel(override val value: Int) : ValidationStatus.NotValid.Level {
    WARNING(1), ERROR(2)
}

class CompositeValidation<T, S>(
    val validations: Collection<Validation<T, S>>,
) : Validation<T, S> {

    /**
     * Finds the most serious failure across supplied validations
     * If exception occurred during any validation it will be ignored if there is any validation that reported [ValidationStatus.NotValid] state
     * If all validations either failed to complete or were valid, then the first exception will be rethrown
     *
     * That is we achieve the following behavior:
     * User does not see exception until he really has to
     */
    override suspend fun validate(value: T): ValidationStatus<S> {
        val validationStatuses = validations.map { runCatching { it.validate(value) } }

        val failureStatuses = validationStatuses.filter { it.isSuccess } // Result.isSuccess -> validation completed w/o exception
            .map { it.requireValue() }
            .filterIsInstance<ValidationStatus.NotValid<S>>()

        val mostSeriousReason = failureStatuses.maxByOrNull { it.level.value }

        return if (mostSeriousReason != null) { // there is at least one NotValid validation
            mostSeriousReason
        } else {
            val firstFailure = validationStatuses.firstOrNull { it.isFailure }

            // rethrow exception if any
            firstFailure?.let { throw it.requireException() }

            ValidationStatus.Valid()
        }
    }
}

class ValidationSystem<T, S>(
    private val validation: Validation<T, S>
) {

    companion object;

    suspend fun validate(
        value: T,
        ignoreUntil: ValidationStatus.NotValid.Level? = null
    ): Result<ValidationStatus<S>> = runCatching {
        withContext(Dispatchers.Default) {
            when (val status = validation.validate(value)) {
                is ValidationStatus.Valid -> status

                is ValidationStatus.NotValid -> {
                    if (ignoreUntil != null && status.level.value <= ignoreUntil.value) {
                        ValidationStatus.Valid()
                    } else {
                        status
                    }
                }
            }
        }
    }

    fun copyTo(validationSystemBuilder: ValidationSystemBuilder<T, S>) {
        validationSystemBuilder.validate(validation)
    }
}

context (ValidationSystemBuilder<P, E>)
fun <P, E> ValidationSystem<P, E>.copyIntoCurrent() = copyTo(this@ValidationSystemBuilder)

fun <T, S> ValidationSystem.Companion.from(validations: Collection<Validation<T, S>>): ValidationSystem<T, S> {
    return ValidationSystem(CompositeValidation(validations))
}

suspend fun <S> ValidationSystem<Unit, S>.validate(
    ignoreUntil: ValidationStatus.NotValid.Level? = null
) = validate(Unit, ignoreUntil)

fun <S> Result<ValidationStatus<S>>.unwrap(
    onValid: () -> Unit,
    onInvalid: (ValidationStatus.NotValid<S>) -> Unit,
    onFailure: (Throwable) -> Unit
) {
    if (isSuccess) {
        when (val status = getOrThrow()) {
            is ValidationStatus.Valid<*> -> onValid()
            is ValidationStatus.NotValid<S> -> onInvalid(status)
        }
    } else {
        onFailure(requireException())
    }
}
