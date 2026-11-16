package com.rosalive.app

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.view.*
import android.widget.*

class FloatingService : Service() {
    private var wm: WindowManager? = null
    private var root: LinearLayout? = null
    private var player: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var volume = 1f
    private var expanded = true

    private val names = arrayOf(
        "🌹 Presente 1–99",
        "🎁 Presente 100+",
        "💖 Agradecer elogio",
        "👋 Boas-vindas",
        "🎯 Meta da LIVE"
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        val channelId = "rosalive_audio"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                channelId,
                "RosaLive",
                NotificationManager.IMPORTANCE_LOW
            )
        )

        val intent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val note = Notification.Builder(this, channelId)
            .setContentTitle("RosaLive ativo")
            .setContentText("Botões de áudio flutuantes")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(intent)
            .build()

        startForeground(1, note)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        showOverlay()
    }

    private fun showOverlay() {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 10, 12, 10)
            setBackgroundColor(0xEE222222.toInt())
        }
        root = panel

        val toggle = Button(this).apply {
            text = "🌹 RosaLive −"
        }
        panel.addView(toggle)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        panel.addView(content)

        names.forEachIndexed { index, name ->
            content.addView(Button(this).apply {
                text = name
                setOnClickListener { play(index) }
            })
        }

        content.addView(Button(this).apply {
            text = "⏹ PARAR ÁUDIO"
            setOnClickListener { stopAudio() }
        })

        content.addView(TextView(this).apply {
            text = "Volume da voz: 0–100%"
            setTextColor(-1)
        })

        content.addView(SeekBar(this).apply {
            max = 100
            progress = 100
            setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    s: SeekBar?, progress: Int, fromUser: Boolean
                ) {
                    volume = progress / 100f
                    player?.setVolume(volume, volume)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })

        content.addView(Button(this).apply {
            text = "✕ Fechar RosaLive"
            setOnClickListener { stopSelf() }
        })

        toggle.setOnClickListener {
            expanded = !expanded
            content.visibility =
                if (expanded) View.VISIBLE else View.GONE
            toggle.text = if (expanded) "🌹 RosaLive −" else "🌹 RosaLive +"
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.END
        params.x = 10
        params.y = 180

        var lastX = 0
        var lastY = 0
        var downX = 0f
        var downY = 0f

        toggle.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = params.x
                    lastY = params.y
                    downX = event.rawX
                    downY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = lastX - (event.rawX - downX).toInt()
                    params.y = lastY + (event.rawY - downY).toInt()
                    wm?.updateViewLayout(panel, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (kotlin.math.abs(event.rawX - downX) < 12 &&
                        kotlin.math.abs(event.rawY - downY) < 12) {
                        v.performClick()
                    }
                    true
                }
                else -> false
            }
        }

        wm?.addView(panel, params)
    }

    private fun requestDuckFocus() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        focusRequest = AudioFocusRequest.Builder(
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        )
            .setAudioAttributes(attrs)
            .setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener { }
            .build()

        audioManager?.requestAudioFocus(focusRequest!!)
    }

    private fun play(index: Int) {
        val saved = getSharedPreferences("audios", MODE_PRIVATE)
            .getString("audio_$index", null)

        if (saved == null) {
            Toast.makeText(
                this,
                "Selecione esse MP3 no aplicativo primeiro",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        stopAudio()
        requestDuckFocus()

        try {
            val p = MediaPlayer()
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            p.setDataSource(this, Uri.parse(saved))
            p.setOnPreparedListener {
                it.setVolume(volume, volume)
                it.start()
            }
            p.setOnCompletionListener {
                it.release()
                if (player === it) player = null
                abandonFocus()
            }
            p.setOnErrorListener { mp, _, _ ->
                mp.release()
                if (player === mp) player = null
                abandonFocus()
                true
            }
            player = p
            p.prepareAsync()
        } catch (e: Exception) {
            stopAudio()
            Toast.makeText(this, "Erro ao abrir MP3", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun stopAudio() {
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {}
        player = null
        abandonFocus()
    }

    override fun onDestroy() {
        stopAudio()
        root?.let { wm?.removeView(it) }
        root = null
        super.onDestroy()
    }
}
