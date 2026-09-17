package com.gdreaction.cam

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat

class CaptureService : Service() {
    companion object { const val START="start"; const val STOP="stop"; const val RESULT_CODE="resultCode"; const val DATA="data"; const val CHANNEL="gd_capture" }
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    override fun onCreate() { super.onCreate(); (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel(CHANNEL,"GD screen capture",NotificationManager.IMPORTANCE_LOW)) }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { projection?.stop(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
        val n = NotificationCompat.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.presence_video_online).setContentTitle("GD Reaction Cam").setContentText("Смотрю экран Geometry Dash 🥶").setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(7,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) else startForeground(7,n)
        val data = if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra(DATA, Intent::class.java) else @Suppress("DEPRECATION") intent?.getParcelableExtra(DATA)
        val code = intent?.getIntExtra(RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        if (data != null && code == Activity.RESULT_OK) {
            projection = (getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(code,data)
            val dm: DisplayMetrics = resources.displayMetrics; val w=dm.widthPixels; val h=dm.heightPixels
            reader = ImageReader.newInstance(w,h,android.graphics.PixelFormat.RGBA_8888,2)
            projection?.createVirtualDisplay("GD",w,h,dm.densityDpi,0,reader!!.surface,null,null)
            // Frames arrive here. Real semantic reactions should be produced by a configured vision backend;
            // never embed a private API key in the APK.
            reader?.setOnImageAvailableListener({ r -> r.acquireLatestImage()?.close() }, null)
        }
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { reader?.close(); projection?.stop(); super.onDestroy() }
}
