package com.kfix.sample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kfix.sample.patch.SimplePatchManager
import com.kfix.sample.second.SecondActivity
import java.io.File

class MainActivity : ComponentActivity() {

    companion object {
        const val PATCH_FILE_NAME = "patch.zip"
    }

    private val launcher =  registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        it.data?.data?.let { uri ->
            downloadPatch(uri).onSuccess {
                messageState.value = "Download Patch success! Please restart the process!"
            }.onFailure {
                messageState.value = "Download Patch error: ${it.message}"
            }
        }

    }
    private val messageState = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Column(modifier = Modifier.padding(10.dp)) {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "*/*"
                    launcher.launch(intent)
                }) {
                    Text(text = "Download patch")
                }
                Text(text = messageState.value)
                Button(onClick = {
                    startActivity(Intent(this@MainActivity, SecondActivity::class.java))
                }) {
                    Text(text = "Open SecondActivity")
                }
            }
        }
    }

    private fun downloadPatch(uri: Uri): Result<Unit> {
        cacheDir.deleteRecursively()
        val apkPath = File(cacheDir, PATCH_FILE_NAME)

        return runCatching {
            apkPath.outputStream().use { fos ->
                val inputStream = contentResolver.openInputStream(uri)!!
                inputStream.use {
                    val buffer = ByteArray(1024)
                    var byteCount: Int
                    while (inputStream.read(buffer).also { byteCount = it } != -1) {
                        fos.write(buffer, 0, byteCount)
                    }
                }
            }

            SimplePatchManager.savePatch(this@MainActivity, apkPath.path)
        }
    }
}