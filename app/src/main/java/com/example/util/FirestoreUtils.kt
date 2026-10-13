package com.example.util

import android.util.Log
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    // Auth is disabled as per user request
    val authInfoJson = JSONObject().apply {
        put("userId", "anonymous_auth_disabled")
        put("status", "Auth disabled by user")
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    // **CRITICAL**: You MUST log every single firestore error.
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}
