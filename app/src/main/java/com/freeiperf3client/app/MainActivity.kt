package com.freeiperf3client.app

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb

private const val REPOSITORY_URL = "https://github.com/Anders0lesen/free-iperf3-client"

class MainActivity : ComponentActivity() {
    private lateinit var engine: IperfEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AppBackground.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(AppBackground.toArgb()),
        )
        engine = IperfEngine(this)
        setContent {
            IperfTheme {
                IperfApp(
                    engine = engine,
                    openRepository = ::openRepository,
                    copyText = ::copyText,
                    shareText = ::shareText,
                )
            }
        }
    }

    private fun openRepository() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPOSITORY_URL)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No browser is available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyText(label: String, value: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(this, "$label copied", Toast.LENGTH_SHORT).show()
    }

    private fun shareText(subject: String, value: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, value)
        }
        startActivity(Intent.createChooser(intent, "Share iperf3 results"))
    }

    override fun onDestroy() {
        engine.cancel()
        engine.stopServer()
        super.onDestroy()
    }
}
