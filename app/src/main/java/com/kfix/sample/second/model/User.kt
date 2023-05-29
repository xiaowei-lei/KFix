package com.kfix.sample.second.model

import androidx.annotation.Keep

@Keep
data class User(
    val id: String,
    val name: String,
    val age: Int,
    val gender: String
)