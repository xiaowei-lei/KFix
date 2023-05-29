package com.kfix.sample.second.model

sealed class ScreenState<T> {
    class Loading<T>: ScreenState<T>()
    class Default<T>: ScreenState<T>()
    data class Loaded<T>(val data: T): ScreenState<T>()
    data class Error<T>(val throwable: Throwable): ScreenState<T>()
}