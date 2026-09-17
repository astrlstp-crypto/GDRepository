package com.gdreaction.cam

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.gdreaction.cam.databinding.ActivityMainBinding
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) startCamera() }
    private val captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK && r.data != null) {
            ContextCompat.startForegroundService(this, Intent(this, CaptureService::class.java).apply {
                action = CaptureService.START
                putExtra(CaptureService.RESULT_CODE, r.resultCode)
                putExtra(CaptureService.DATA, r.data)
            })
            b.reaction.text = "ТРАНСЛЯЦИЯ ПОШЛА 🥶🔥"
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); b = ActivityMainBinding.inflate(layoutInflater); setContentView(b.root)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
        b.startCapture.setOnClickListener { val m = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager; captureLauncher.launch(m.createScreenCaptureIntent()) }
        b.stopCapture.setOnClickListener { startService(Intent(this, CaptureService::class.java).setAction(CaptureService.STOP)); b.reaction.text = "ТРАНСЛЯЦИЯ ОСТАНОВЛЕНА" }
    }
    private fun startCamera() {
        val f = ProcessCameraProvider.getInstance(this)
        f.addListener({ val p = f.get(); val preview = Preview.Builder().build().also { it.setSurfaceProvider(b.cameraPreview.surfaceProvider) }; p.unbindAll(); p.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview) }, ContextCompat.getMainExecutor(this))
    }
    override fun onDestroy() { cameraExecutor.shutdown(); super.onDestroy() }
}
