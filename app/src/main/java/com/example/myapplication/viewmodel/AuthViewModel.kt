package com.example.myapplication.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.example.myapplication.model.UserRole
import com.example.myapplication.repository.AuthRepository
import com.example.myapplication.repository.IAuthRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel private constructor(
    application: Application?,
    private val repository: IAuthRepository
) : AndroidViewModel(application ?: Application()) {

    constructor(application: Application) : this(
        application = application,
        repository = AuthRepository(application)
    )

    // Preview constructor
    constructor() : this(
        application = null,
        repository = object : IAuthRepository {
            override fun getUserRole(): UserRole = UserRole.ADMIN
            override fun saveUserRole(role: UserRole) {}
            override fun hasAdminPinSet(): Boolean = true
            override fun validateAdminPin(pin: String): Boolean = pin == "1234"
            override fun saveAdminPin(pin: String) {}
            override fun getVendors(): List<String> = listOf("Admin", "Vendedor")
            override fun saveVendors(vendors: List<String>) {}
            override fun getCurrentVendor(): String = "Vendedor"
            override fun saveCurrentVendor(vendor: String) {}
            override fun getAdminVendors(): Set<String> = setOf("Admin")
            override fun saveAdminVendors(admins: Set<String>) {}
            override fun setOnVendorsUpdatedListener(listener: (List<String>) -> Unit) {}
            override fun setOnAdminVendorsUpdatedListener(listener: (Set<String>) -> Unit) {}
            override fun setOnAdminPinUpdatedListener(listener: (String) -> Unit) {}
        }
    )

    constructor(repository: IAuthRepository) : this(
        application = null,
        repository = repository
    )

    private val _userRole = MutableStateFlow<UserRole>(repository.getUserRole())
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    private val _hasAdminPinSet = MutableStateFlow(repository.hasAdminPinSet())
    val hasAdminPinSet: StateFlow<Boolean> = _hasAdminPinSet.asStateFlow()

    private val _showCreatePinDialog = MutableStateFlow(false)
    val showCreatePinDialog: StateFlow<Boolean> = _showCreatePinDialog.asStateFlow()

    private val _showLoginDialog = MutableStateFlow(false)
    val showLoginDialog: StateFlow<Boolean> = _showLoginDialog.asStateFlow()

    private val _vendors = MutableStateFlow<List<String>>(repository.getVendors())
    val vendors: StateFlow<List<String>> = _vendors.asStateFlow()

    private val _currentVendor = MutableStateFlow<String>(repository.getCurrentVendor())
    val currentVendor: StateFlow<String> = _currentVendor.asStateFlow()

    private val _adminVendors = MutableStateFlow<Set<String>>(repository.getAdminVendors())
    val adminVendors: StateFlow<Set<String>> = _adminVendors.asStateFlow()

    private val _showManageVendorsDialog = MutableStateFlow(false)
    val showManageVendorsDialog: StateFlow<Boolean> = _showManageVendorsDialog.asStateFlow()

    private val _showAdminPinPrompt = MutableStateFlow(false)
    val showAdminPinPrompt: StateFlow<Boolean> = _showAdminPinPrompt.asStateFlow()

    private val _pendingAdminVendor = MutableStateFlow<String?>(null)
    val pendingAdminVendor: StateFlow<String?> = _pendingAdminVendor.asStateFlow()

    private val _showAdminResetNoticeDialog = MutableStateFlow(false)
    val showAdminResetNoticeDialog: StateFlow<Boolean> = _showAdminResetNoticeDialog.asStateFlow()

    private val _resetNoticeEmail = MutableStateFlow("")
    val resetNoticeEmail: StateFlow<String> = _resetNoticeEmail.asStateFlow()

    init {
        val allVendors = repository.getVendors()
        val allAdmins = repository.getAdminVendors()

        val standardVendor = allVendors.firstOrNull { !allAdmins.contains(it) } ?: "Vendedor"
        if (!allVendors.contains(standardVendor)) {
            val updated = allVendors.toMutableList().apply { add(standardVendor) }
            _vendors.value = updated
            repository.saveVendors(updated)
        }

        _currentVendor.value = standardVendor
        repository.saveCurrentVendor(standardVendor)
        _userRole.value = UserRole.USER
        repository.saveUserRole(UserRole.USER)

        repository.setOnVendorsUpdatedListener { newVendors ->
            _vendors.value = newVendors
        }
        repository.setOnAdminVendorsUpdatedListener { newAdmins ->
            _adminVendors.value = newAdmins
            _userRole.value = if (newAdmins.contains(_currentVendor.value)) UserRole.ADMIN else UserRole.USER
        }
        repository.setOnAdminPinUpdatedListener { newPin ->
            if (newPin.isNotBlank()) {
                _hasAdminPinSet.value = true
                _showCreatePinDialog.value = false
            }
        }
    }

    fun openManageVendorsDialog() {
        _showManageVendorsDialog.value = true
    }

    fun closeManageVendorsDialog() {
        _showManageVendorsDialog.value = false
    }

    fun createAdminPin(pin: String) {
        val trimmed = pin.trim()
        if (trimmed.length >= 4) {
            repository.saveAdminPin(trimmed)
            _hasAdminPinSet.value = true
            val target = _pendingAdminVendor.value ?: "Admin"
            applyVendorChange(target)
            _pendingAdminVendor.value = null
            _showCreatePinDialog.value = false
            _showManageVendorsDialog.value = false
        }
    }

    fun verifyAccountPasswordToResetAdminPin(
        accountPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null && !user.email.isNullOrEmpty()) {
            val credential = EmailAuthProvider.getCredential(user.email!!, accountPassword.trim())
            user.reauthenticate(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _showAdminPinPrompt.value = false
                        _showCreatePinDialog.value = true
                        onSuccess()
                    } else {
                        onError("Contraseña de cuenta incorrecta.")
                    }
                }
        } else {
            _showAdminPinPrompt.value = false
            _showCreatePinDialog.value = true
            onSuccess()
        }
    }

    fun requestAdminPinResetEmail(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email ?: ""
        if (email.isNotBlank()) {
            val auth = FirebaseAuth.getInstance()
            auth.setLanguageCode("es")
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _resetNoticeEmail.value = email
                        _showAdminResetNoticeDialog.value = true
                        _showAdminPinPrompt.value = false
                    } else {
                        Toast.makeText(
                            context,
                            "Error al enviar correo de verificación: ${task.exception?.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        } else {
            _showCreatePinDialog.value = true
            _showAdminPinPrompt.value = false
        }
    }

    fun closeAdminResetNoticeDialog() {
        _showAdminResetNoticeDialog.value = false
    }

    fun loginAsAdmin(pin: String): Boolean {
        return if (repository.validateAdminPin(pin)) {
            _userRole.value = UserRole.ADMIN
            repository.saveUserRole(UserRole.ADMIN)
            _showLoginDialog.value = false
            true
        } else {
            false
        }
    }

    fun loginAsUser() {
        _userRole.value = UserRole.USER
        repository.saveUserRole(UserRole.USER)
        _showLoginDialog.value = false
    }

    fun openLoginDialog() {
        _showLoginDialog.value = true
    }

    fun closeLoginDialog() {
        _showLoginDialog.value = false
    }

    fun openCreatePinDialog() {
        _showCreatePinDialog.value = true
    }

    fun closeCreatePinDialog() {
        _showCreatePinDialog.value = false
        if (!_hasAdminPinSet.value) {
            _pendingAdminVendor.value = null
            applyVendorChange("Vendedor")
        }
    }

    fun switchRole(newRole: UserRole) {
        _userRole.value = newRole
        repository.saveUserRole(newRole)
    }

    fun addVendor(name: String, isAdmin: Boolean) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            val currentVendors = _vendors.value.toMutableList()
            if (!currentVendors.contains(trimmed)) {
                currentVendors.add(trimmed)
                _vendors.value = currentVendors
                repository.saveVendors(currentVendors)
            }
            val currentAdmins = _adminVendors.value.toMutableSet()
            if (isAdmin) {
                currentAdmins.add(trimmed)
            } else {
                currentAdmins.remove(trimmed)
            }
            _adminVendors.value = currentAdmins
            repository.saveAdminVendors(currentAdmins)
        }
    }

    fun deleteVendor(name: String) {
        val trimmed = name.trim()
        if (trimmed != "Admin") {
            val currentVendors = _vendors.value.filter { it != trimmed }
            _vendors.value = currentVendors
            repository.saveVendors(currentVendors)

            val currentAdmins = _adminVendors.value.toMutableSet()
            currentAdmins.remove(trimmed)
            _adminVendors.value = currentAdmins
            repository.saveAdminVendors(currentAdmins)

            if (_currentVendor.value == trimmed) {
                setCurrentVendor("Vendedor")
            }
        }
    }

    fun setCurrentVendor(vendor: String) {
        val trimmed = vendor.trim().ifEmpty { "Vendedor" }
        val isAdminVendor = _adminVendors.value.contains(trimmed)

        if (isAdminVendor) {
            _pendingAdminVendor.value = trimmed
            if (_hasAdminPinSet.value) {
                if (_userRole.value == UserRole.USER) {
                    _showAdminPinPrompt.value = true
                } else {
                    applyVendorChange(trimmed)
                }
            } else {
                _showCreatePinDialog.value = true
            }
        } else {
            applyVendorChange(trimmed)
        }
    }

    fun confirmAdminPinPrompt(pin: String): Boolean {
        return if (repository.validateAdminPin(pin)) {
            val target = _pendingAdminVendor.value ?: "Admin"
            applyVendorChange(target)
            _showAdminPinPrompt.value = false
            _pendingAdminVendor.value = null
            _showManageVendorsDialog.value = false
            true
        } else {
            false
        }
    }

    fun cancelAdminPinPrompt() {
        _showAdminPinPrompt.value = false
        _pendingAdminVendor.value = null
    }

    private fun applyVendorChange(vendor: String) {
        _currentVendor.value = vendor
        repository.saveCurrentVendor(vendor)
        val isAdm = _adminVendors.value.contains(vendor)
        val newRole = if (isAdm) UserRole.ADMIN else UserRole.USER
        _userRole.value = newRole
        repository.saveUserRole(newRole)
    }
}
