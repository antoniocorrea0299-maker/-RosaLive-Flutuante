package com.rosalive.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
    private val names = arrayOf(
        "Presentes 1 a 99 moedas",
        "Presentes 100+ moedas",
        "Agradecer elogio",
        "Dar boas-vindas",
        "Convidar para a meta"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 35, 30, 25)
        }

        root.addView(TextView(this).apply {
            text = "🌹 RosaLive"
            textSize = 26f
        })

        root.addView(TextView(this).apply {
            text = "Configure os cinco áudios da sua LIVE."
            textSize = 16f
        })

        names.forEachIndexed { index, name ->
            root.addView(Button(this).apply {
                text = "🎵 Selecionar: $name"
                setOnClickListener { pick(index) }
            })
        }

        root.addView(Button(this).apply {
            text = "Permitir janela flutuante"
            setOnClickListener {
                startActivity(Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                ))
            }
        })

        root.addView(Button(this).apply {
            text = "🌹 Abrir RosaLive flutuante"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    startForegroundService(
                        Intent(this@MainActivity, FloatingService::class.java)
                    )
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Permita a janela flutuante primeiro",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        })

        root.addView(Button(this).apply {
            text = "Fechar RosaLive"
            setOnClickListener {
                stopService(Intent(this@MainActivity, FloatingService::class.java))
            }
        })

        root.addView(TextView(this).apply {
            text = "O Spotify poderá reduzir o volume durante as falas, se aceitar o pedido de foco de áudio do Android."
            textSize = 14f
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun pick(index: Int) {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "audio/*"
            },
            100 + index
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK || requestCode !in 100..104) return

        val uri = data?.data ?: return

        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {}

        getSharedPreferences("audios", MODE_PRIVATE)
            .edit()
            .putString("audio_${requestCode - 100}", uri.toString())
            .apply()

        Toast.makeText(this, "Áudio salvo!", Toast.LENGTH_SHORT).show()
    }
}
