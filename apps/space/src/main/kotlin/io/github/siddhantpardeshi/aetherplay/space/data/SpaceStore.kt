package io.github.siddhantpardeshi.aetherplay.space.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class SpaceInfo(
    val key: String,
    val name: String,
    val createdAt: Long,
    val lastUsedAt: Long,
)

/** Named spaces with separate per-space game data, persisted as JSON. */
class SpaceStore(private val context: Context) {

    private val file: File get() = File(context.filesDir, "spaces.json")
    private val spacesDir: File get() = File(context.filesDir, "spaces").apply { mkdirs() }

    fun list(): List<SpaceInfo> {
        val spaces = readAll()
        return if (spaces.isEmpty()) {
            val main = SpaceInfo("main", "Main space", System.currentTimeMillis(), System.currentTimeMillis())
            writeAll(listOf(main))
            listOf(main)
        } else spaces
    }

    fun create(name: String): SpaceInfo {
        val clean = name.trim().ifEmpty { "Space" }
        val info = SpaceInfo(UUID.randomUUID().toString().substring(0, 8), clean, System.currentTimeMillis(), System.currentTimeMillis())
        writeAll(readAll() + info)
        return info
    }

    fun rename(key: String, name: String) {
        writeAll(readAll().map { if (it.key == key) it.copy(name = name.trim().ifEmpty { it.name }) else it })
    }

    fun delete(key: String) {
        writeAll(readAll().filterNot { it.key == key })
        dirFor(key).deleteRecursively()
    }

    fun touch(key: String) {
        writeAll(readAll().map { if (it.key == key) it.copy(lastUsedAt = System.currentTimeMillis()) else it })
    }

    fun dirFor(key: String): File = File(spacesDir, key).apply { mkdirs() }

    fun saveFileFor(key: String): File = File(dirFor(key), "crystal-sprint.json")

    fun storageBytes(): Long =
        spacesDir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    private fun readAll(): List<SpaceInfo> = try {
        if (!file.exists()) emptyList()
        else {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                SpaceInfo(o.getString("key"), o.getString("name"), o.getLong("createdAt"), o.getLong("lastUsedAt"))
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun writeAll(spaces: List<SpaceInfo>) {
        try {
            val arr = JSONArray()
            spaces.forEach {
                arr.put(JSONObject().put("key", it.key).put("name", it.name).put("createdAt", it.createdAt).put("lastUsedAt", it.lastUsedAt))
            }
            file.writeText(arr.toString())
        } catch (_: Exception) {
        }
    }
}
