package io.github.siddhantpardeshi.aetherplay.lab.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Experiment(
    val name: String,
    val typeWire: Int,
    val kind: Int, // 0 exact, 1 range, 2 unknown
    val valueText: String,
    val minText: String,
    val maxText: String,
    val alignment: Int,
    val regionIds: List<String>,
    val createdAt: Long,
)

/** Saved search recipes, persisted as JSON in app-private storage. */
class ExperimentStore(private val context: Context) {

    private val file: File get() = File(context.filesDir, "experiments.json")

    fun list(): List<Experiment> = try {
        if (!file.exists()) emptyList()
        else {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Experiment(
                    name = o.getString("name"),
                    typeWire = o.getInt("type"),
                    kind = o.getInt("kind"),
                    valueText = o.optString("value", ""),
                    minText = o.optString("min", ""),
                    maxText = o.optString("max", ""),
                    alignment = o.optInt("alignment", 1),
                    regionIds = o.optJSONArray("regions")?.let { r -> (0 until r.length()).map { r.getString(it) } } ?: listOf("state"),
                    createdAt = o.getLong("createdAt"),
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun save(experiment: Experiment) {
        val all = list().filterNot { it.name == experiment.name } + experiment
        write(all.sortedByDescending { it.createdAt })
    }

    fun delete(name: String) {
        write(list().filterNot { it.name == name })
    }

    fun exportJson(): String {
        val arr = JSONArray()
        list().forEach { e ->
            arr.put(
                JSONObject()
                    .put("name", e.name)
                    .put("type", e.typeWire)
                    .put("kind", e.kind)
                    .put("value", e.valueText)
                    .put("min", e.minText)
                    .put("max", e.maxText)
                    .put("alignment", e.alignment)
                    .put("regions", JSONArray(e.regionIds))
                    .put("createdAt", e.createdAt),
            )
        }
        return JSONObject().put("aetherplayExperiments", arr).toString(2)
    }

    private fun write(list: List<Experiment>) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(
                JSONArray().apply {
                    list.forEach { e ->
                        put(
                            JSONObject()
                                .put("name", e.name)
                                .put("type", e.typeWire)
                                .put("kind", e.kind)
                                .put("value", e.valueText)
                                .put("min", e.minText)
                                .put("max", e.maxText)
                                .put("alignment", e.alignment)
                                .put("regions", JSONArray(e.regionIds))
                                .put("createdAt", e.createdAt),
                        )
                    }
                }.toString(),
            )
        } catch (_: Exception) {
        }
    }
}
