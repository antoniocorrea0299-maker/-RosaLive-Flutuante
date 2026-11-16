package com.rosalive.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
    private val requestLow = 10
    private val requestHigh = 11
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(35, 50, 35, 25) }
        fun add(label: String, action: () -> Unit) { root.addView(Button(this).apply { text = label; setOnClickListener { action() } }) }
        root.addView(TextView(this).apply { text = "🌹 RosaLive — MP3 e botões flutuantes"; textSize = 22f })
        root.addView(TextView(this).apply { text = "Escolha dois áudios MP3 e abra a barra sobre o TikTok. Os presentes são identificados por você, não automaticamente." })
        add("🌹 Selecionar MP3 — 1 a 99 moedas") { pick(requestLow) }
        add("🎁 Selecionar MP3 — 100+ moedas") { pick(requestHigh) }
        add("Permitir janela flutuante") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        add("Abrir botões flutuantes") {
            if (!Settings.canDrawOverlays(this)) Toast.makeText(this, "Permita a janela flutuante primeiro", Toast.LENGTH_LONG).show()
            else startForegroundService(Intent(this, FloatingService::class.java))
        }
        add("Fechar botões flutuantes") { stopService(Intent(this, FloatingService::class.java)) }
        root.addView(TextView(this).apply { text = "Ajuste o volume na barra. A reprodução dos MP3 não garante que o TikTok capture esse som durante a LIVE." })
        setContentView(ScrollView(this).apply { addView(root) })
    }
    private fun pick(code: Int) {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
        }, code)
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
        getSharedPreferences("audios", MODE_PRIVATE).edit().putString(if (requestCode == requestLow) "low" else "high", uri.toString()).apply()
        Toast.makeText(this, "Áudio salvo!", Toast.LENGTH_SHORT).show()
    }
}
