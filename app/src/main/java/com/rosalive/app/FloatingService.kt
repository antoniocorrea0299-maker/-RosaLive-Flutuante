package com.rosalive.app

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*

class FloatingService : Service() {
    private var wm: WindowManager? = null
    private var view: LinearLayout? = null
    private var player: MediaPlayer? = null
    private var volume = 1f
    private var expanded = true
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        val channelId = "rosalive_audio"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(channelId, "RosaLive", NotificationManager.IMPORTANCE_LOW))
        val intent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val note = Notification.Builder(this, channelId).setContentTitle("RosaLive ativo").setContentText("Botões de áudio flutuantes").setSmallIcon(android.R.drawable.ic_media_play).setContentIntent(intent).build()
        startForeground(1, note)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showOverlay()
    }
    private fun showOverlay() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            setBackgroundColor(0xEE222222.toInt())
        }
        view = root
        val toggle = Button(this).apply { text = "🌹 RosaLive  −" }
        root.addView(toggle)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(content)
        fun btn(label: String, action: () -> Unit) { content.addView(Button(this).apply { text = label; setOnClickListener { action() } }) }
        btn("🌹 1–99 moedas") { play("low") }
        btn("🎁 100+ moedas") { play("high") }
        btn("⏹ Parar") { stopAudio() }
        content.addView(TextView(this).apply { text = "Volume da fala"; setTextColor(-1) })
        content.addView(SeekBar(this).apply {
            max = 100; progress = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) { volume = progress / 100f; player?.setVolume(volume, volume) }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })
        btn("✕ Fechar") { stopSelf() }
        toggle.setOnClickListener { expanded = !expanded; content.visibility = if (expanded) android.view.View.VISIBLE else android.view.View.GONE; toggle.text = if (expanded) "🌹 RosaLive  −" else "🌹 +" }
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 10; params.y = 180
        var lastX = 0; var lastY = 0; var downX = 0f; var downY = 0f
        toggle.setOnTouchListener { v, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> { lastX = params.x; lastY = params.y; downX = event.rawX; downY = event.rawY; true }
                MotionEvent.ACTION_MOVE -> { params.x = lastX - (event.rawX - downX).toInt(); params.y = lastY + (event.rawY - downY).toInt(); wm?.updateViewLayout(root, params); true }
                MotionEvent.ACTION_UP -> { if (kotlin.math.abs(event.rawX - downX) < 12 && kotlin.math.abs(event.rawY - downY) < 12) v.performClick(); true }
                else -> false
            }
        }
        wm?.addView(root, params)
    }
    private fun play(key: String) {
        val saved = getSharedPreferences("audios", MODE_PRIVATE).getString(key, null)
        if (saved == null) { Toast.makeText(this, "Selecione o MP3 no RosaLive primeiro", Toast.LENGTH_SHORT).show(); return }
        stopAudio()
        try {
            val p = MediaPlayer()
            p.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            p.setDataSource(this, Uri.parse(saved))
            p.setOnPreparedListener { it.setVolume(volume, volume); it.start() }
            p.setOnCompletionListener { it.reset(); it.release(); if (player === it) player = null }
            p.prepareAsync()
            player = p
        } catch (e: Exception) { stopAudio(); Toast.makeText(this, "Erro ao abrir MP3", Toast.LENGTH_SHORT).show() }
    }
    private fun stopAudio() { try { player?.stop(); player?.release() } catch (_: Exception) {}; player = null }
    override fun onDestroy() { stopAudio(); view?.let { wm?.removeView(it) }; view = null; super.onDestroy() }
}
