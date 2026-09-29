package br.com.linear.energyar.data
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
class InspectionStore(context: Context) {
    private val prefs = context.getSharedPreferences("energyar_inspections_v2", Context.MODE_PRIVATE)
    fun load(): InspectionState = runCatching {
        val j = JSONObject(prefs.getString("state", "{}") ?: "{}")
        val records = j.optJSONArray("records") ?: JSONArray()
        InspectionState(scenario = Scenario.valueOf(j.optString("scenario", "LEAK")),
            hours = j.optDouble("hours", 8.0).coerceIn(1.0, 24.0), days = j.optInt("days", 22).coerceIn(1, 31), tariff = j.optDouble("tariff", .85).coerceIn(.1, 3.0),
            records = (0 until records.length()).map { i -> val r=records.getJSONObject(i)
                InspectionRecord(r.getString("id"), r.getLong("timestamp"), Scenario.valueOf(r.getString("scenario")), r.getDouble("kw"), r.getDouble("saving"), r.getDouble("tariff"), r.getDouble("hours"), r.getInt("days"), r.optBoolean("resolved"))
            })
    }.getOrDefault(InspectionState())
    fun save(s: InspectionState) {
        val records=JSONArray()
        s.records.take(100).forEach { r -> records.put(JSONObject().put("id",r.id).put("timestamp",r.timestamp).put("scenario",r.scenario.name).put("kw",r.observedKw).put("saving",r.projectedMonthlySaving).put("tariff",r.tariff).put("hours",r.hours).put("days",r.days).put("resolved",r.resolved)) }
        prefs.edit().putString("state",JSONObject().put("scenario",s.scenario.name).put("hours",s.hours).put("days",s.days).put("tariff",s.tariff).put("records",records).toString()).apply()
    }
}
