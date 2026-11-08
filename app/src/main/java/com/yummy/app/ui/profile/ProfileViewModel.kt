package com.yummy.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yummy.app.data.AuthRepository
import com.yummy.app.data.ProfileRepository
import com.yummy.app.data.model.Profile
import com.yummy.app.data.model.ProfileStats
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileState(
    val loading: Boolean = true,
    val profile: Profile = Profile(),
    val email: String = "",
    val stats: ProfileStats = ProfileStats(),
    val showError: Boolean = false
)

class ProfileViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()
    private var job: Job? = null

    fun refresh() = launchSafely { load() }

    fun setNotifyDays(days: Int) = launchSafely {
        ProfileRepository.setNotifyDays(days); load()
    }

    fun setUnits(units: String) = launchSafely {
        ProfileRepository.setUnits(units); load()
    }

    fun setHousehold(size: Int) = launchSafely {
        ProfileRepository.setHousehold(size); load()
    }

    fun errorShown() = _state.update { it.copy(showError = false) }

    private fun launchSafely(block: suspend () -> Unit) {
        job?.cancel()
        job = viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, showError = true) }
            }
        }
    }

    private suspend fun load() = coroutineScope {
        val profile = async { ProfileRepository.profile() }
        val stats = async { ProfileRepository.stats() }
        _state.value = ProfileState(
            loading = false,
            profile = profile.await(),
            email = AuthRepository.currentEmail(),
            stats = stats.await()
        )
    }
}