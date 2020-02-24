package org.ndp.dns_scan_ns.bean

import com.squareup.moshi.Json

data class MQResult(
    @Json(name = "task-id") val taskID: Int,
    val result: List<String>,
    val status: Int,
    val desc: String
)