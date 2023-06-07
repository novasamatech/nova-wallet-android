package io.novafoundation.nova.common.data.network.subquery

object SubqueryExpressions {

    fun or(vararg innerExpressions: String): String {
        return compoundExpression("or", *innerExpressions)
    }

    fun or(innerExpressions: Collection<String>) = or(*innerExpressions.toTypedArray())

    fun anyOf(innerExpressions: Collection<String>) = or(innerExpressions)
    fun anyOf(vararg innerExpressions: String) = or(*innerExpressions)

    fun allOf(vararg innerExpressions: String) = and(*innerExpressions)

    fun and(vararg innerExpressions: String): String {
        return compoundExpression("and", *innerExpressions)
    }

    infix fun String.and(another: String): String {
        return and(this, another)
    }

    fun and(innerExpressions: Collection<String>) = and(*innerExpressions.toTypedArray())

    fun not(expression: String): String {
        return "not: {$expression}"
    }

    private fun compoundExpression(name: String, vararg innerExpressions: String): String {
        if (innerExpressions.isEmpty()) {
            return ""
        }

        if (innerExpressions.size == 1) {
            return innerExpressions.first()
        }

        return innerExpressions.joinToString(
            prefix = "$name: [",
            postfix = "]",
            separator = ","
        ) {
            "{$it}"
        }
    }
}
