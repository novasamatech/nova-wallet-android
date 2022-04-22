package io.novafoundation.nova.common.validation

interface ValidationSystemBuilder<P, E> {

    fun validate(validation: Validation<P, E>)

    fun build(): ValidationSystem<P, E>
}

private class Builder<P, E> : ValidationSystemBuilder<P, E> {

    private val validations = mutableListOf<Validation<P, E>>()

    override fun validate(validation: Validation<P, E>) {
        validations += validation
    }

    override fun build(): ValidationSystem<P, E> {
        return ValidationSystem.from(validations)
    }
}

fun <P, E> ValidationSystem(builderBlock: ValidationSystemBuilder<P, E>.() -> Unit): ValidationSystem<P, E> {
    val builder = Builder<P, E>()

    builder.builderBlock()

    return builder.build()
}

fun <P, E> EmptyValidationSystem(): ValidationSystem<P, E> = ValidationSystem.from(emptyList())
