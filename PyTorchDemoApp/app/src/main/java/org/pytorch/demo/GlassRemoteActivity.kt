package org.pytorch.demo

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants.*
import android.hardware.usb.UsbDevice
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import kotlinx.android.synthetic.main.activity_glass_remote.*
import net.ossrs.rtmp.ConnectCheckerRtmp
import org.pytorch.demo.streamlib.RtmpUSB
import org.pytorch.demo.util.Util


class GlassRemoteActivity : Activity(), SurfaceHolder.Callback, ConnectCheckerRtmp {

//  private val RCCAMERA = 2002;
//
//  private fun requestPermission() {
//    //1. 检查是否已经有该权限
//    if (Build.VERSION.SDK_INT >= 23 && ((ActivityCompat.checkSelfPermission(
//        this,
//        Manifest.permission.CAMERA
//      )
//              !== PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(
//        this,
//        Manifest.permission.RECORD_AUDIO
//      )
//              !== PackageManager.PERMISSION_GRANTED) || (ActivityCompat.checkSelfPermission(
//        this,
//        Manifest.permission.WRITE_EXTERNAL_STORAGE
//      )
//              !== PackageManager.PERMISSION_GRANTED))
//    ) {
//      //2. 权限没有开启，请求权限
//      ActivityCompat.requestPermissions(
//        this,
//        arrayOf(
//          Manifest.permission.CAMERA,
//          Manifest.permission.RECORD_AUDIO,
//          Manifest.permission.WRITE_EXTERNAL_STORAGE
//        ),
//        RCCAMERA
//      )
//    } else {
//      //权限已经开启，做相应事情
//      println("in request permission before init")
//    }
//  }
//
//  //3. 接收申请成功或者失败回调
//  override fun onRequestPermissionsResult(
//    requestCode: Int,
//    permissions: Array<String?>?,
//    grantResults: IntArray
//  ) {
//    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//    if (requestCode == RCCAMERA) {
//      if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//        //权限被用户同意,做相应的事情
//      } else {
//        //权限被用户拒绝，做相应的事情
//        finish()
//      }
//    }
//  }
  override fun onAuthSuccessRtmp() {
    Log.e("Pedro", "auth success")
  }

  override fun onConnectionSuccessRtmp() {
    runOnUiThread {
      Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onConnectionFailedRtmp(reason: String?) {
    runOnUiThread {
      Toast.makeText(this, "Failed $reason", Toast.LENGTH_SHORT).show()
      rtmpUSB.stopStream(uvcCamera)
    }
  }

  override fun onAuthErrorRtmp() {
    Log.e("Pedro", "auth error")
  }

  override fun onDisconnectRtmp() {
    runOnUiThread {
      Toast.makeText(this, "Disconnect", Toast.LENGTH_SHORT).show()
    }
  }

  override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
  }

  override fun surfaceDestroyed(p0: SurfaceHolder?) {

  }

  override fun surfaceCreated(p0: SurfaceHolder?) {

  }

  private lateinit var usbMonitor: USBMonitor
  private var uvcCamera: UVCCamera? = null
  private var isUsbOpen = true
  private val width = 1280
  private val height = 720
  private var defished = false
  private lateinit var rtmpUSB: RtmpUSB
  private var ctrlBlock: USBMonitor.UsbControlBlock? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_glass_remote)
//    requestPermission()
    rtmpUSB = RtmpUSB(openglview, this)
//    Toast.makeText(this, "rtmp usb done", Toast.LENGTH_SHORT).show()
    usbMonitor = USBMonitor(this, onDeviceConnectListener)
//    Toast.makeText(this, "usb monitor done", Toast.LENGTH_SHORT).show()
    isUsbOpen = false
    usbMonitor.register()
//    Toast.makeText(this, "usb register done", Toast.LENGTH_SHORT).show()
    val util: Util = Util()
    et_url.setText(util.rtmpurl)
//    et_url.setText("rtmp://10.138.116.66/live/livestream")
    start_stop.text = "开始推流"
    start_stop.setOnClickListener {
      if (uvcCamera != null) {
        if (!rtmpUSB.isStreaming) {
          Toast.makeText(this@GlassRemoteActivity, "开始推流，地址"+et_url.text.toString(), Toast.LENGTH_SHORT).show()
          startStream(et_url.text.toString())
          start_stop.text = "停止推流"
        } else {
          rtmpUSB.stopStream(uvcCamera)
          start_stop.text = "开始推流"
        }
      }
    }
//    camera.setOnClickListener {
//      if (uvcCamera == null) {
//        val camera = UVCCamera()
//        var deviceCount = usbMonitor.deviceCount
//        if (deviceCount > 0)
//        {
//          Toast.makeText(this@GlassRemoteActivity, "usb has $deviceCount devices ", Toast.LENGTH_SHORT).show()
//          var usbDevice = usbMonitor.deviceList.get(0)
//          ctrlBlock = usbMonitor.openDevice(usbDevice)
//
//        }else{
//          Toast.makeText(this@GlassRemoteActivity, "usb detected no devices", Toast.LENGTH_SHORT).show()
//        }
//
//
//        if (ctrlBlock != null){
//          camera.open(ctrlBlock, false)
//          Toast.makeText(this@GlassRemoteActivity, "uvc camera done", Toast.LENGTH_SHORT).show()
//          try {
//            camera.setPreviewSize(width, height, UVCCamera.FRAME_FORMAT_MJPEG)
//          } catch (e: IllegalArgumentException) {
//            camera.destroy()
//            try {
//              camera.setPreviewSize(width, height, UVCCamera.DEFAULT_PREVIEW_MODE)
//            } catch (e1: IllegalArgumentException) {
//
//            }
//          }
//          uvcCamera = camera
//          rtmpUSB.startPreview(uvcCamera, width, height)
//          Toast.makeText(this@GlassRemoteActivity, "uvc camera start previewing", Toast.LENGTH_SHORT).show()
//
//        }
//        else{
//          Toast.makeText(this@GlassRemoteActivity, "ctrl block null", Toast.LENGTH_SHORT).show()
//
//        }
//      }
//      else{
//        Toast.makeText(this@GlassRemoteActivity, "camera not null", Toast.LENGTH_SHORT).show()
//      }
//    }
  }

  private fun startStream(url: String) {
    if (rtmpUSB.prepareVideo(width, height, 30, 4000 * 1024, false, 0,
        uvcCamera) && rtmpUSB.prepareAudio()) {
      rtmpUSB.startStream(uvcCamera, url)
    }
  }

  private val onDeviceConnectListener = object : USBMonitor.OnDeviceConnectListener {
    override fun onAttach(device: UsbDevice?) {
      if (device != null) {
        Toast.makeText(this@GlassRemoteActivity, "usb设备已接入with class " + device.getDeviceClass(), Toast.LENGTH_SHORT).show()

        if(device.deviceClass == USB_CLASS_MISC)
          usbMonitor.requestPermission(device)
      }
//      usbMonitor.processConnect(device)

    }

    override fun onConnect(
      device: UsbDevice?, controlBlock: USBMonitor.UsbControlBlock?,
      createNew: Boolean
    ) {
//      Toast.makeText(this@GlassRemoteActivity, "dev3ce connected", Toast.LENGTH_SHORT).show()

      val camera = UVCCamera()
      ctrlBlock = controlBlock
      Toast.makeText(this@GlassRemoteActivity, "打开usb相机完成", Toast.LENGTH_SHORT).show()
      camera.open(ctrlBlock, false)

      try {
        camera.setPreviewSize(width, height, UVCCamera.FRAME_FORMAT_MJPEG)
      } catch (e: IllegalArgumentException) {
        camera.destroy()
        try {
          camera.setPreviewSize(width, height, UVCCamera.DEFAULT_PREVIEW_MODE)
        } catch (e1: IllegalArgumentException) {
          return
        }
      }
      uvcCamera = camera
      rtmpUSB.startPreview(uvcCamera, width, height)
      Toast.makeText(this@GlassRemoteActivity, "开始预览", Toast.LENGTH_SHORT).show()
    }

    override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
      if (uvcCamera != null) {
        uvcCamera?.close()
        uvcCamera = null
        isUsbOpen = false
      }
    }

    override fun onDettach(device: UsbDevice?) {
      if (uvcCamera != null) {
        uvcCamera?.close()
        uvcCamera = null
        isUsbOpen = false
      }
    }

    override fun onCancel(device: UsbDevice?) {

    }
  }

  override fun onDestroy() {
    super.onDestroy()
      if (rtmpUSB.isStreaming && uvcCamera != null) rtmpUSB.stopStream(uvcCamera)
    if (rtmpUSB.isOnPreview && uvcCamera != null) rtmpUSB.stopPreview(uvcCamera)
    if (isUsbOpen) {
      uvcCamera?.close()
      usbMonitor.unregister()
    }
  }
}
