package com.kfix.sample.second

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kfix.sample.second.model.ScreenState
import com.kfix.sample.second.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SecondViewModel: ViewModel() {

    private val _screenStateFlow: MutableStateFlow<ScreenState<User>> = MutableStateFlow(ScreenState.Default())
    val screenStateFlow = _screenStateFlow.asStateFlow()
    fun loadUser() {
        viewModelScope.launch(Dispatchers.IO) {
            _screenStateFlow.update { ScreenState.Loading() }
            kotlin.runCatching {
                getUser()
            }.onFailure { throwable ->
                _screenStateFlow.update { ScreenState.Error(throwable) }
            }.onSuccess {user ->
                _screenStateFlow.update { ScreenState.Loaded(user) }
            }
        }
    }

    private suspend fun getUser(): User {
        delay(2000)
        return User(
            id = "0000001",
            name = "Tom",
            age = 20,
            gender = "male"
        )
    }

    fun reset() {
        _screenStateFlow.update { ScreenState.Default() }
    }
}