package com.example.myapplication.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.model.UserRole
import com.example.myapplication.util.FirestoreSyncHelper
import org.json.JSONArray
import org.json.JSONObject

interface IAuthRepository {
    fun getUserRole(): UserRole
    fun saveUserRole(role: UserRole)
    fun hasAdminPinSet(): Boolean
    fun validateAdminPin(pin: String): Boolean
    fun saveAdminPin(pin: String)
    fun getVendors(): List<String>
    fun saveVendors(vendors: List<String>)
    fun getCurrentVendor(): String
    fun saveCurrentVendor(vendor: String)
    fun getAdminVendors(): Set<String>
    fun saveAdminVendors(admins: Set<String>)
    fun setOnVendorsUpdatedListener(listener: (List<String>) -> Unit)
    fun setOnAdminVendorsUpdatedListener(listener: (Set<String>) -> Unit)
    fun setOnAdminPinUpdatedListener(listener: (String) -> Unit)
}

class AuthRepository(context: Context) : IAuthRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_storage_prefs", Context.MODE_PRIVATE)

    private var onVendorsUpdated: ((List<String>) -> Unit)? = null
    private var onAdminVendorsUpdated: ((Set<String>) -> Unit)? = null
    private var onAdminPinUpdated: ((String) -> Unit)? = null

    companion object {
        private const val KEY_ROLE = "key_user_role"
        private const val KEY_ADMIN_PIN = "key_admin_pin"
        private const val KEY_VENDORS_LIST = "key_vendors_list"
        private const val KEY_CURRENT_VENDOR = "key_current_vendor"
        private const val KEY_ADMIN_VENDORS = "key_admin_vendors"
    }

    init {
        FirestoreSyncHelper.listenToData("admin_pin") { json ->
            val cleanPin = parsePinString(json)
            if (cleanPin.isNotEmpty()) {
                prefs.edit().putString(KEY_ADMIN_PIN, cleanPin).apply()
                onAdminPinUpdated?.invoke(cleanPin)
            }
        }
        FirestoreSyncHelper.listenToData("vendors_list") { json ->
            if (!json.isNullOrEmpty()) {
                prefs.edit().putString(KEY_VENDORS_LIST, json).apply()
                onVendorsUpdated?.invoke(parseStringListJson(json))
            }
        }
        FirestoreSyncHelper.listenToData("admin_vendors") { json ->
            if (!json.isNullOrEmpty()) {
                prefs.edit().putString(KEY_ADMIN_VENDORS, json).apply()
                onAdminVendorsUpdated?.invoke(parseStringSetJson(json))
            }
        }
    }

    private fun parsePinString(raw: String?): String {
        if (raw.isNullOrEmpty()) return ""
        val trimmed = raw.trim()
        if (trimmed.startsWith("{") && trimmed.contains("pin")) {
            try {
                return JSONObject(trimmed).optString("pin", "").trim()
            } catch (e: Exception) {
            }
        }
        return trimmed.removeSurrounding("\"").trim()
    }

    override fun setOnVendorsUpdatedListener(listener: (List<String>) -> Unit) {
        onVendorsUpdated = listener
    }

    override fun setOnAdminVendorsUpdatedListener(listener: (Set<String>) -> Unit) {
        onAdminVendorsUpdated = listener
    }

    override fun setOnAdminPinUpdatedListener(listener: (String) -> Unit) {
        onAdminPinUpdated = listener
    }

    override fun getUserRole(): UserRole {
        val roleName = prefs.getString(KEY_ROLE, UserRole.USER.name) ?: UserRole.USER.name
        return try {
            UserRole.valueOf(roleName)
        } catch (e: Exception) {
            UserRole.USER
        }
    }

    override fun saveUserRole(role: UserRole) {
        prefs.edit().putString(KEY_ROLE, role.name).apply()
    }

    override fun hasAdminPinSet(): Boolean {
        val savedPin = parsePinString(prefs.getString(KEY_ADMIN_PIN, null))
        return savedPin.isNotEmpty()
    }

    override fun validateAdminPin(pin: String): Boolean {
        val savedPin = parsePinString(prefs.getString(KEY_ADMIN_PIN, null))
        val inputPin = pin.trim()
        return savedPin.isNotEmpty() && inputPin == savedPin
    }

    override fun saveAdminPin(pin: String) {
        val trimmed = pin.trim()
        prefs.edit().putString(KEY_ADMIN_PIN, trimmed).apply()
        FirestoreSyncHelper.syncDataUp("admin_pin", trimmed)
        onAdminPinUpdated?.invoke(trimmed)
    }

    override fun getVendors(): List<String> {
        val jsonString = prefs.getString(KEY_VENDORS_LIST, null) ?: return listOf("Admin", "Vendedor")
        return parseStringListJson(jsonString)
    }

    override fun saveVendors(vendors: List<String>) {
        val jsonArray = JSONArray()
        vendors.forEach { jsonArray.put(it) }
        val jsonString = jsonArray.toString()
        prefs.edit().putString(KEY_VENDORS_LIST, jsonString).apply()
        FirestoreSyncHelper.syncDataUp("vendors_list", jsonString)
        onVendorsUpdated?.invoke(vendors)
    }

    override fun getCurrentVendor(): String {
        return prefs.getString(KEY_CURRENT_VENDOR, "Vendedor") ?: "Vendedor"
    }

    override fun saveCurrentVendor(vendor: String) {
        prefs.edit().putString(KEY_CURRENT_VENDOR, vendor.trim()).apply()
    }

    override fun getAdminVendors(): Set<String> {
        val jsonString = prefs.getString(KEY_ADMIN_VENDORS, null) ?: return setOf("Admin")
        return parseStringSetJson(jsonString)
    }

    override fun saveAdminVendors(admins: Set<String>) {
        val jsonArray = JSONArray()
        admins.forEach { jsonArray.put(it) }
        val jsonString = jsonArray.toString()
        prefs.edit().putString(KEY_ADMIN_VENDORS, jsonString).apply()
        FirestoreSyncHelper.syncDataUp("admin_vendors", jsonString)
        onAdminVendorsUpdated?.invoke(admins)
    }

    private fun parseStringListJson(jsonString: String): List<String> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            if (list.isEmpty()) listOf("Admin", "Vendedor") else list
        } catch (e: Exception) {
            listOf("Admin", "Vendedor")
        }
    }

    private fun parseStringSetJson(jsonString: String): Set<String> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val set = mutableSetOf<String>()
            for (i in 0 until jsonArray.length()) {
                set.add(jsonArray.getString(i))
            }
            if (set.isEmpty()) setOf("Admin") else set
        } catch (e: Exception) {
            setOf("Admin")
        }
    }
}
