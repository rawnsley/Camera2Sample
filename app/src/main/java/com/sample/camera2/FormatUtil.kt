package com.sample.camera2

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics

object FormatUtil {
    fun imageFormatToString(format : Int) : String {
        return when(format) {
            ImageFormat.DEPTH16 -> "DEPTH16"
            ImageFormat.DEPTH_JPEG -> "DEPTH_JPEG"
            ImageFormat.DEPTH_POINT_CLOUD -> "DEPTH_POINT_CLOUD"
            ImageFormat.FLEX_RGB_888 -> "FLEX_RGB_888"
            ImageFormat.FLEX_RGBA_8888 -> "FLEX_RGBA_8888"
            ImageFormat.HEIC -> "HEIC"
            ImageFormat.JPEG -> "JPEG"
            ImageFormat.NV16 -> "NV16"
            ImageFormat.NV21 -> "NV21"
            ImageFormat.PRIVATE -> "PRIVATE"
            ImageFormat.RAW10 -> "RAW10"
            ImageFormat.RAW12 -> "RAW12"
            ImageFormat.RAW_PRIVATE -> "RAW_PRIVATE"
            ImageFormat.RAW_SENSOR -> "RAW_SENSOR"
            ImageFormat.RGB_565 -> "RGB_565"
            ImageFormat.UNKNOWN -> "UNKNOWN"
            ImageFormat.Y8 -> "Y8"
            ImageFormat.YUV_420_888 -> "YUV_420_888"
            ImageFormat.YUV_422_888 -> "YUV_422_888"
            ImageFormat.YUV_444_888 -> "YUV_422_888"
            ImageFormat.YUY2 -> "YUY2"
            ImageFormat.YV12 -> "YV12"
            else -> "INVALID"
        }
    }

    fun supportedHardwareLevelToString(format : Int) : String {
        return when(format) {
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "3"
            else -> "INVALID"
        }
    }

}