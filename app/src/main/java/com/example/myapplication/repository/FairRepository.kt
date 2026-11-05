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
            jsonArray.put(fair.toJson())
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
                list.add(obj.toFairRecord())
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun FairRecord.toJson(): JSONObject {
        val itemsArray = JSONArray()
        itemsTaken.forEach { item ->
            itemsArray.put(
                JSONObject().apply {
                    put("productName", item.productName)
                    put("quantityTaken", item.quantityTaken)
                }
            )
        }

        val salesArray = JSONArray()
        associatedSaleIds.forEach { salesArray.put(it) }

        val expensesArray = JSONArray()
        associatedExpenseIds.forEach { expensesArray.put(it) }

        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("dateMillis", dateMillis)
            put("location", location)
            put("itemsTaken", itemsArray)
            put("associatedSaleIds", salesArray)
            put("associatedExpenseIds", expensesArray)
        }
    }

    private fun JSONObject.toFairRecord(): FairRecord {
        val itemsArray = optJSONArray("itemsTaken") ?: JSONArray()
        val itemsList = mutableListOf<FairItem>()
        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.getJSONObject(i)
            itemsList.add(
                FairItem(
                    productName = obj.optString("productName"),
                    quantityTaken = obj.optInt("quantityTaken", 0)
                )
            )
        }

        val salesArray = optJSONArray("associatedSaleIds") ?: JSONArray()
        val salesList = mutableListOf<String>()
        for (i in 0 until salesArray.length()) {
            salesList.add(salesArray.getString(i))
        }

        val expensesArray = optJSONArray("associatedExpenseIds") ?: JSONArray()
        val expensesList = mutableListOf<String>()
        for (i in 0 until expensesArray.length()) {
            expensesList.add(expensesArray.getString(i))
        }

        return FairRecord(
            id = getString("id"),
            name = getString("name"),
            dateMillis = optLong("dateMillis", System.currentTimeMillis()),
            location = optString("location", ""),
            itemsTaken = itemsList,
            associatedSaleIds = salesList,
            associatedExpenseIds = expensesList
        )
    }
}
