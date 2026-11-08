package com.yummy.app.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object AuthRepository {
    private val auth get() = SupabaseProvider.client.auth
    fun currentUserId(): String? = auth.currentUserOrNull()?.id
    fun currentEmail(): String = auth.currentUserOrNull()?.email.orEmpty()
    /** Devuelve true si ya hay sesión (hay sesión) tras el registro. */
    suspend fun signUp(name: String, email: String, password: String, household: Int) {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
            // El trigger handle_new_user lee estos campos para crear el perfil
            data = buildJsonObject {
                put("name", name)
                put("household_size", household)
            }
        }
        // Si la confirmación de correo está desactivada, Supabase abre sesión solo.
        // La cerramos para que el usuario inicie sesión manualmente.
        if (auth.currentSessionOrNull() != null) {
            auth.signOut()
        }
    }
    suspend fun signIn(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun hasSession(): Boolean {
        auth.awaitInitialization()
        return auth.currentSessionOrNull() != null
    }

    suspend fun signOut() = auth.signOut()

    fun friendlyError(e: Exception): String {
        val m = e.message.orEmpty()
        return when {
            "Invalid login credentials" in m -> "Correo o contraseña incorrectos"
            "already registered" in m || "already been registered" in m -> "Ese correo ya está registrado"
            "Email not confirmed" in m -> "Confirma tu correo antes de iniciar sesión"
            "Password should be" in m -> "La contraseña debe tener al menos 6 caracteres"
            "Unable to resolve host" in m || "timeout" in m.lowercase() -> "Sin conexión a internet"
            else -> "Ocurrió un error. Inténtalo de nuevo"
        }
    }
}