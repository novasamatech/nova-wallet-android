package io.novafoundation.nova.common.base.errors

class CompoundException(val nested: List<Throwable>) : Exception()
