package com.example.myapplication.util

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object FirestoreSyncHelper {
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    fun clearLocalAppCache(context: Context) {
        context.getSharedPreferences("products_storage_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("sales_storage_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("expenses_storage_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("fairs_storage_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("auth_storage_prefs", Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun syncDataUp(documentKey: String, jsonContent: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("store_data").document(documentKey)
            .set(mapOf("content" to jsonContent, "timestamp" to System.currentTimeMillis()))
            .addOnSuccessListener {
                Log.d("FirestoreSync", "Sincronizado $documentKey exitosamente para UID: $uid")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreSync", "Error al sincronizar $documentKey: ${e.message}", e)
            }
    }

    fun fetchSyncDataDown(documentKey: String, onDataReceived: (String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("store_data").document(documentKey)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val content = document.getString("content")
                    Log.d("FirestoreSync", "Descargado $documentKey exitosamente")
                    onDataReceived(content)
                } else {
                    onDataReceived(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreSync", "Error al descargar $documentKey: ${e.message}", e)
                onDataReceived(null)
            }
    }

    fun listenToData(documentKey: String, onDataReceived: (String?) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid ?: return null
        return db.collection("users").document(uid)
            .collection("store_data").document(documentKey)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreSync", "Error en listener de $documentKey: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val content = snapshot.getString("content")
                    onDataReceived(content)
                }
            }
    }
}
