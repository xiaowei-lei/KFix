package com.kfix.sample.second

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.kfix.sample.second.model.ScreenState
import com.kfix.sample.second.model.User

class SecondActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = ViewModelProvider(this)[SecondViewModel::class.java]
        setContent {
            val state = viewModel.screenStateFlow.collectAsState().value
            Column(modifier = Modifier.padding(10.dp)) {
                UserSelection(
                    loadAction = viewModel::loadUser,
                    userState = state
                )
                ResetSection(
                    resetAction = viewModel::reset
                )
            }
        }
    }

    @Composable
    fun UserSelection(
        loadAction: () -> Unit,
        userState: ScreenState<User>,
    ) {
        Column {
            Text(text = "User Profile")
            when (userState) {
                is ScreenState.Default -> Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { loadAction() }) {
                    Text(text = "LOAD")
                }

                is ScreenState.Loading -> Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(50.dp),
                    textAlign = TextAlign.Center,
                    text = "LOADING..."
                )

                is ScreenState.Error -> Text(text = userState.throwable.stackTraceToString())
                is ScreenState.Loaded -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            with(userState.data) {
                                Text(text = "Name: $name")
                                Text(text = "Age: $age")
                                Text(text = "Gender: $gender")
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ResetSection(resetAction: () -> Unit) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = resetAction
        ) {
            Text(text = "RESET")
        }
    }
}
