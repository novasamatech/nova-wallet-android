package io.novafoundation.nova.common.utils

import android.net.Uri

fun Uri.hasQuery(key: String): Boolean {
    return getQueryParameter(key) != null
}

fun Uri.Builder.appendPathOrSkip(path: String?): Uri.Builder {
    if (path != null) {
        appendPath(path)
    }

    return this
}

fun Uri.Builder.appendQueries(queries: Map<String, String>): Uri.Builder {
    queries.forEach { (key, value) ->
        appendQueryParameter(key, value)
    }

    return this
}
