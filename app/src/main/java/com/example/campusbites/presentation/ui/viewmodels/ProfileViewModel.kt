package com.example.campusbites.presentation.ui.viewmodels

import androidx.compose.runtime.saveable.autoSaver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.domain.usecase.user.GetUserByIdUseCase
import com.example.campusbites.domain.usecase.tag.GetDietaryTagByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val getDietaryTagByIdUseCase: GetDietaryTagByIdUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }


    private fun loadUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val user: UserDomain = getUserByIdUseCase("ps8ntqSGvzgilhqlXKNP")
                val dietaryNames = coroutineScope {
                    user.dietaryPreferencesTagIds.map { tagId ->
                        async { getDietaryTagByIdUseCase(tagId).name }
                    }.awaitAll()
                }
                _uiState.update {
                    it.copy(
                        user = user,
                        dietaryPreferenceNames = dietaryNames,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Error al cargar el usuario",
                        isLoading = false
                    )
                }
            }
        }
    }
}

data class ProfileUiState(
    val user: UserDomain? = null,
    val dietaryPreferenceNames: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
