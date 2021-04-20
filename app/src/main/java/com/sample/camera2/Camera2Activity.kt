package com.sample.camera2

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Bundle
import android.util.Log
import android.util.Range
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sample.camera2.FormatUtil.supportedHardwareLevelToString

class Camera2Activity : Activity() {

    companion object {
        private val TAG = Camera2Activity::class.simpleName
        private const val PermissionRequestCode = 1234
    }

    lateinit var surfaceView : SurfaceView

    lateinit var cameraManager : CameraManager
    lateinit var defaultCameraId : String
    lateinit var activeCameraDevice : CameraDevice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        // Connect to camera service
        Log.i(TAG, "Connecting to CameraManager service...")
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        probeAvailableCameraModes()

        // Preview surface lifecycle callbacks
        val surfaceHolderCallback = object: SurfaceHolder.Callback {
            override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
                Log.i(TAG, "surfaceChanged")
            }
            override fun surfaceDestroyed(p0: SurfaceHolder?) {
                Log.i(TAG, "surfaceDestroyed")
            }
            override fun surfaceCreated(p0: SurfaceHolder?) {
                Log.i(TAG, "surfaceCreated")
                // Resolution can be set here to influence the camera mode selection
                surfaceView.holder.setFixedSize(640, 480)
                connectCameraService()
            }
        }
        surfaceView.holder.addCallback(surfaceHolderCallback)

        // Check for runtime permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "CAMERA runtime permission not granted. Requesting...")
            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PermissionRequestCode)
        } else {
            Log.i(TAG, "CAMERA runtime permission is granted")
        }
    }

    fun probeAvailableCameraModes() {
        // List all available cameras
        val cameraList = cameraManager.cameraIdList
        Log.i(TAG, "CameraManager has ${cameraList.size} cameras")
        for(cameraId in cameraManager.cameraIdList) {
            Log.i(TAG, "- CameraID = $cameraId")
        }
        // Interrogate the default camera
        defaultCameraId = cameraList.first()
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(defaultCameraId)

        val supportedHardwareLevel = cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)!!
        Log.i(TAG, "- Supported hardware level ${supportedHardwareLevelToString(supportedHardwareLevel)} ($supportedHardwareLevel)")

        val streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!

        // All mode FPS ranges
        val fpsRanges = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)!!
        Log.i(TAG, "Default camera has ${fpsRanges.size} FPS ranges")
        for(fpsRange in fpsRanges) {
            Log.i(TAG, "- FPS ${fpsRange.lower}..${fpsRange.upper}")
        }

        // All mode resolutions
        val imageFormats = streamConfigurationMap.outputFormats
        Log.i(TAG, "Default camera supports ${imageFormats.size} formats")
        for(imageFormat in imageFormats) {
            val outputSizes = streamConfigurationMap.getOutputSizes(imageFormat)
            Log.i(TAG, "- Image format ${FormatUtil.imageFormatToString(imageFormat)} ($imageFormat). Size count ${outputSizes.size}")
            for(outputSize in outputSizes) {
                val minFrameDurationNanos = streamConfigurationMap.getOutputMinFrameDuration(imageFormat, outputSize)
                val maxFPS = if(minFrameDurationNanos > 0) 1_000_000_000 / minFrameDurationNanos else -1
                Log.i(TAG, "-- Size ${outputSize.width} x ${outputSize.height}; MaxFPS $maxFPS; Supports Surface? ${StreamConfigurationMap.isOutputSupportedFor(SurfaceHolder::class.java)}")
            }
        }

        // Surface compatible resolutions
        val outputSizes = streamConfigurationMap.getOutputSizes(SurfaceHolder::class.java)
        Log.i(TAG, "- Preview Surface")
        for(outputSize in outputSizes) {
            val minFrameDurationNanos = streamConfigurationMap.getOutputMinFrameDuration(SurfaceHolder::class.java, outputSize)
            val maxFPS = if(minFrameDurationNanos > 0) 1_000_000_000 / minFrameDurationNanos else -1
            Log.i(TAG, "-- Size ${outputSize.width} x ${outputSize.height}; MaxFPS $maxFPS")
        }

    }

    @SuppressLint("MissingPermission")
    fun connectCameraService() {
        // Frame capture callbacks
        val captureCallback = object: CameraCaptureSession.CaptureCallback() {
            private var frameCount = 0L
            private var lastReportedFPSNanos = System.nanoTime()
            private var reportedFrameResult = false
            override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                super.onCaptureCompleted(session, request, result)
                ++frameCount
                val nowNanos = System.nanoTime()
                if(nowNanos - lastReportedFPSNanos > 1_000_000_000) {
                    Log.i(TAG, "FPS: $frameCount")
                    lastReportedFPSNanos = nowNanos
                    frameCount = 0
                }
                if(!reportedFrameResult) {
                    for(key in result.keys) {
                        val value = result.get(key)
                        if(value != null) {
                            Log.i(TAG, "First Frame: ${key.name} = $value")
                        }
                    }
                    reportedFrameResult = true
                }
            }
        }

        // Camera session callbacks
        val captureSessionCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e(TAG, "onConfigureFailed")
            }
            override fun onConfigured(session: CameraCaptureSession) {
                Log.i(TAG, "onConfigured")
                val previewRequest = activeCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    .apply {
                        // Add surface as target
                        addTarget(surfaceView.holder.surface)
                        // FPS range can be set here to influence camera mode selection
                        set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(30, 30))
                        set(CaptureRequest.CONTROL_CAPTURE_INTENT, CameraMetadata.CONTROL_CAPTURE_INTENT_PREVIEW)
                        set (CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
                        set (CaptureRequest.LENS_FOCUS_DISTANCE, 0f)

                    }
                    .build()
                session.setRepeatingRequest(previewRequest, captureCallback,null)
            }
        }

        // Callback for camera open/close events
        val stateCallback = object : CameraDevice.StateCallback() {
            override fun onDisconnected(p0: CameraDevice) {
                Log.i(TAG, "Camera $p0 disconnected")
            }
            override fun onError(p0: CameraDevice, p1: Int) {
                Log.e(TAG, "Camera $p0 error $p1")
            }

            override fun onOpened(cameraDevice: CameraDevice) {
                Log.i(TAG, "Camera ${cameraDevice.id} opened")
                activeCameraDevice = cameraDevice
                cameraDevice.createCaptureSession(mutableListOf(surfaceView.holder.surface), captureSessionCallback, null)
            }
        }

        // Open the default camera
        cameraManager.openCamera(defaultCameraId, stateCallback, null)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PermissionRequestCode -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    connectCameraService()
                } else {
                    Log.w(TAG, "Camera permission denied so Activity must finish")
                    finish()
                }
                return
            }
            else -> Log.w(TAG, "Unexpected permission request code")
        }
    }
}
