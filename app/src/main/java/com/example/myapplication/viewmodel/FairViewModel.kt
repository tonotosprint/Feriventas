package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.myapplication.model.FairItem
import com.example.myapplication.model.FairRecord
import com.example.myapplication.repository.FairRepository
import com.example.myapplication.repository.IFairRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FairViewModel private constructor(
    application: Application?,
    private val repository: IFairRepository
) : AndroidViewModel(application ?: Application()) {

    constructor(application: Application) : this(
        application = application,
        repository = FairRepository(application)
    )

    constructor() : this(
        application = null,
        repository = object : IFairRepository {
            private val list = mutableListOf<FairRecord>()
            override fun getFairs() = list
            override fun addFair(fair: FairRecord) { list.add(0, fair) }
            override fun updateFair(fair: FairRecord) {
                val idx = list.indexOfFirst { it.id == fair.id }
                if (idx >= 0) list[idx] = fair
            }
            override fun removeFair(fairId: String) { list.removeAll { it.id == fairId } }
            override fun clearFairs() { list.clear() }
            override fun setOnFairsUpdatedListener(listener: (List<FairRecord>) -> Unit) {}
        }
    )

    constructor(repository: IFairRepository) : this(
        application = null,
        repository = repository
    )

    private val _fairs = MutableStateFlow<List<FairRecord>>(repository.getFairs())
    val fairs: StateFlow<List<FairRecord>> = _fairs.asStateFlow()

    init {
        repository.setOnFairsUpdatedListener { newFairs ->
            _fairs.value = newFairs
        }
    }

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _selectedFairForDetails = MutableStateFlow<FairRecord?>(null)
    val selectedFairForDetails: StateFlow<FairRecord?> = _selectedFairForDetails.asStateFlow()

    fun openCreateDialog() {
        _showCreateDialog.value = true
    }

    fun closeCreateDialog() {
        _showCreateDialog.value = false
    }

    fun openFairDetails(fair: FairRecord) {
        _selectedFairForDetails.value = fair
    }

    fun closeFairDetails() {
        _selectedFairForDetails.value = null
    }

    fun addFair(name: String, dateMillis: Long, location: String) {
        val trimmedName = name.trim()
        if (trimmedName.isNotEmpty()) {
            val newFair = FairRecord(
                name = trimmedName,
                dateMillis = dateMillis,
                location = location.trim()
            )
            repository.addFair(newFair)
            _fairs.value = repository.getFairs()
            closeCreateDialog()
        }
    }

    fun updateFairItems(fairId: String, items: List<FairItem>) {
        val current = _fairs.value.find { it.id == fairId } ?: return
        val updated = current.copy(itemsTaken = items)
        repository.updateFair(updated)
        _fairs.value = repository.getFairs()
        if (_selectedFairForDetails.value?.id == fairId) {
            _selectedFairForDetails.value = updated
        }
    }

    fun deleteFair(fairId: String) {
        repository.removeFair(fairId)
        _fairs.value = repository.getFairs()
        if (_selectedFairForDetails.value?.id == fairId) {
            _selectedFairForDetails.value = null
        }
    }
}
