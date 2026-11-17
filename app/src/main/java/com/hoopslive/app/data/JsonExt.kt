package com.hoopslive.app.data

import org.json.JSONArray
import org.json.JSONObject

/** Small null-safe helpers over org.json, which ships with Android. */

internal fun JSONObject?.obj(key: String): JSONObject? = this?.optJSONObject(key)

internal fun JSONObject?.arr(key: String): JSONArray? = this?.optJSONArray(key)

/** Returns a non-blank string value, or null. Numbers and booleans are stringified. */
internal fun JSONObject?.str(key: String): String? {
    if (this == null || !has(key) || isNull(key)) return null
    val v = opt(key)
    val s = when (v) {
        is String -> v
        is Number, is Boolean -> v.toString()
        else -> null
    }
    return s?.takeIf { it.isNotBlank() }
}

internal fun JSONObject?.int(key: String): Int? {
    if (this == null || !has(key) || isNull(key)) return null
    return when (val v = opt(key)) {
        is Number -> v.toInt()
        is String -> v.toDoubleOrNull()?.toInt()
        else -> null
    }
}

internal fun JSONObject?.long(key: String): Long? {
    if (this == null || !has(key) || isNull(key)) return null
    return when (val v = opt(key)) {
        is Number -> v.toLong()
        is String -> v.toDoubleOrNull()?.toLong()
        else -> null
    }
}

internal fun JSONObject?.dbl(key: String): Double? {
    if (this == null || !has(key) || isNull(key)) return null
    return when (val v = opt(key)) {
        is Number -> v.toDouble()
        is String -> v.toDoubleOrNull()
        else -> null
    }
}

internal fun JSONObject?.bool(key: String): Boolean {
    if (this == null || !has(key) || isNull(key)) return false
    return when (val v = opt(key)) {
        is Boolean -> v
        is String -> v.equals("true", ignoreCase = true)
        is Number -> v.toInt() != 0
        else -> false
    }
}

internal fun JSONArray?.objects(): List<JSONObject> {
    if (this == null) return emptyList()
    val out = ArrayList<JSONObject>(length())
    for (i in 0 until length()) optJSONObject(i)?.let { out.add(it) }
    return out
}

internal fun JSONArray?.strings(): List<String> {
    if (this == null) return emptyList()
    val out = ArrayList<String>(length())
    for (i in 0 until length()) {
        val v = opt(i)
        out.add(
            when (v) {
                null, JSONObject.NULL -> ""
                is JSONObject -> v.str("displayValue") ?: ""
                else -> v.toString()
            }
        )
    }
    return out
}

internal fun JSONArray?.doubles(): List<Double?> {
    if (this == null) return emptyList()
    val out = ArrayList<Double?>(length())
    for (i in 0 until length()) {
        out.add(
            when (val v = opt(i)) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull()
                else -> null
            }
        )
    }
    return out
}

internal fun JSONObject.keyList(): List<String> {
    val out = ArrayList<String>()
    val it = keys()
    while (it.hasNext()) out.add(it.next())
    return out
}
