package io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl

import android.util.Log

internal interface ExtrinsicVisitorLogger {

    fun info(message: String)

    fun error(message: String)
}

internal class IndentVisitorLogger(
    private val tag: String = "ExtrinsicVisitor",
    private val indent: Int = 0
) : ExtrinsicVisitorLogger {

    private val indentPrefix = " ".repeat(indent)

    override fun info(message: String) {
        Log.d(tag, indentPrefix + message)
    }

    override fun error(message: String) {
        Log.e(tag, indentPrefix + message)
    }
}
