package com.freeiperf3client.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class RecentServer(
    val hostname: String,
    val port: Int,
    val lastUsedEpochMs: Long,
) {
    val endpoint: String
        get() = if (hostname.contains(':')) "[$hostname]:$port" else "$hostname:$port"
}

internal class RecentServersStore(context: Context) {
    private val preferences = context.getSharedPreferences("recent_servers", Context.MODE_PRIVATE)

    fun load(): List<RecentServer> = runCatching {
        val array = JSONArray(preferences.getString(KEY_SERVERS, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val hostname = item.optString("hostname").trim()
                val port = item.optInt("port", 0)
                if (isValidServerName(hostname) && port in 1..65535) {
                    add(RecentServer(hostname, port, item.optLong("lastUsedEpochMs", 0L)))
                }
            }
        }.sortedByDescending(RecentServer::lastUsedEpochMs).take(MAX_SERVERS)
    }.getOrDefault(emptyList())

    fun record(hostname: String, port: Int): List<RecentServer> {
        val normalized = hostname.trim().removeSurrounding("[", "]")
        val updated = listOf(RecentServer(normalized, port, System.currentTimeMillis())) +
            load().filterNot { it.hostname.equals(normalized, ignoreCase = true) && it.port == port }
        return save(updated.take(MAX_SERVERS))
    }

    fun remove(server: RecentServer): List<RecentServer> = save(
        load().filterNot {
            it.hostname.equals(server.hostname, ignoreCase = true) && it.port == server.port
        },
    )

    fun clear(): List<RecentServer> = save(emptyList())

    private fun save(servers: List<RecentServer>): List<RecentServer> {
        val array = JSONArray()
        servers.forEach { server ->
            array.put(
                JSONObject()
                    .put("hostname", server.hostname)
                    .put("port", server.port)
                    .put("lastUsedEpochMs", server.lastUsedEpochMs),
            )
        }
        preferences.edit().putString(KEY_SERVERS, array.toString()).apply()
        return servers
    }

    private companion object {
        const val KEY_SERVERS = "servers"
        const val MAX_SERVERS = 8
    }
}
