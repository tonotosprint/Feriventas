package com.example.myapplication.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.model.FairItem
import com.example.myapplication.model.FairRecord
import com.example.myapplication.util.FirestoreSyncHelper
import org.json.JSONArray
import org.json.JSONObject

interface IFairRepository {
    fun getFairs(): List<FairRecord>
    fun addFair(fair: FairRecord)
    fun updateFair(fair: FairRecord)
    fun removeFair(fairId: String)
    fun clearFairs()
    fun setOnFairsUpdatedListener(listener: (List<FairRecord>) -> Unit)
}

class FairRepository(context: Context) : IFairRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("fairs_storage_prefs", Context.MODE_PRIVATE)

    private var onFairsUpdated: ((List<FairRecord>) -> Unit)? = null

    companion object {
        private const val KEY_FAIRS = "key_fairs_records"
    }

    init {
        FirestoreSyncHelper.listenToData("fairs_records") { json ->
            if (!json.isNullOrEmpty()) {
                prefs.edit().putString(KEY_FAIRS, json).apply()
                onFairsUpdated?.invoke(parseFairsJson(json))
            } else {
                prefs.edit().remove(KEY_FAIRS).apply()
                onFairsUpdated?.invoke(emptyList())
            }
        }
    }

    override fun setOnFairsUpdatedListener(listener: (List<FairRecord>) -> Unit) {
        onFairsUpdated = listener
    }

    override fun getFairs(): List<FairRecord> {
        val jsonString = prefs.getString(KEY_FAIRS, null) ?: return emptyList()
        return parseFairsJson(jsonString)
    }

    override fun addFair(fair: FairRecord) {
        val current = getFairs().toMutableList()
        current.add(0, fair)
        saveFairsList(current)
    }

    override fun updateFair(fair: FairRecord) {
        val current = getFairs().map { if (it.id == fair.id) fair else it }
        saveFairsList(current)
    }

    override fun removeFair(fairId: String) {
        val current = getFairs().filter { it.id != fairId }
        saveFairsList(current)
    }

    override fun clearFairs() {
        prefs.edit().remove(KEY_FAIRS).apply()
        FirestoreSyncHelper.syncDataUp("fairs_records", "[]")
        onFairsUpdated?.invoke(emptyList())
    }

    private fun saveFairsList(fairs: List<FairRecord>) {
        val jsonArray = JSONArray()
        fairs.forEach { fair ->
            val obj = JSONObject().apply {
                put("id", fair.id)
                put("name", fair.name)
                put("location", fair.location)
                put("dateMillis", fair.dateMillis)

                val itemsArray = JSONArray()
                fair.itemsTaken.forEach { item ->
                    val itemObj = JSONObject().apply {
                        put("productName", item.productName)
                        put("quantityTaken", item.quantityTaken)
                    }
                    itemsArray.put(itemObj)
                }
                put("itemsTaken", itemsArray)
            }
            jsonArray.put(obj)
        }
        val jsonString = jsonArray.toString()
        prefs.edit().putString(KEY_FAIRS, jsonString).apply()
        FirestoreSyncHelper.syncDataUp("fairs_records", jsonString)
        onFairsUpdated?.invoke(fairs)
    }

    private fun parseFairsJson(jsonString: String): List<FairRecord> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<FairRecord>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id")
                val name = obj.optString("name")
                val location = obj.optString("location")
                val dateMillis = obj.optLong("dateMillis", System.currentTimeMillis())

                val itemsList = mutableListOf<FairItem>()
                val itemsArray = obj.optJSONArray("itemsTaken")
                if (itemsArray != null) {
                    for (j in 0 until itemsArray.length()) {
                        val itemObj = itemsArray.getJSONObject(j)
                        itemsList.add(
                            FairItem(
                                productName = itemObj.optString("productName"),
                                quantityTaken = itemObj.optInt("quantityTaken", 0)
                            )
                        )
                    }
                }

                list.add(
                    FairRecord(
                        id = id,
                        name = name,
                        location = location,
                        dateMillis = dateMillis,
                        itemsTaken = itemsList
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
