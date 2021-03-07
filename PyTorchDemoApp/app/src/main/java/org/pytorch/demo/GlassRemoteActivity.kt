package org.pytorch.demo

import android.app.Activity
import android.graphics.Color
import android.graphics.Rect
import android.hardware.usb.UsbConstants.USB_CLASS_MISC
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import kotlinx.android.synthetic.main.activity_glass_remote.*
import kotlinx.android.synthetic.main.activity_glass_remote.graphicOverlay
import kotlinx.android.synthetic.main.activity_remote_face_detect.*
import net.ossrs.rtmp.ConnectCheckerRtmp
import org.pytorch.demo.streamlib.RtmpUSB
import org.pytorch.demo.util.Util
import org.pytorch.demo.vision.Helper.RectOverlay
import org.pytorch.demo.vision.view.ResultRowView
import java.util.*


class GlassRemoteActivity : Activity(), SurfaceHolder.Callback, ConnectCheckerRtmp {

  override fun onAuthSuccessRtmp() {
    Log.e("Pedro", "auth success")
  }

  override fun onNewBitrateRtmp(bitrate: Long) {
    TODO("Not yet implemented")
  }

  override fun onConnectionSuccessRtmp() {
    runOnUiThread {
      Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
    }
  }

<<<<<<< Updated upstream
=======

>>>>>>> Stashed changes
  override fun onConnectionFailedRtmp(reason: String) {
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
  private val namedboxpool: ArrayList<Utils.NamedBox>? = null
  private lateinit var usbMonitor: USBMonitor
  private var uvcCamera: UVCCamera? = null
  private var isUsbOpen = true
  private val width = 1280
  private val height = 720
  private var defished = false
  private lateinit var rtmpUSB: RtmpUSB
  private var ctrlBlock: USBMonitor.UsbControlBlock? = null
  private val mResultRowViews = arrayOfNulls<ResultRowView>(Utils.TOP_K)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_glass_remote)
//    requestPermission()
    graphicOverlay.bringToFront()
    imageView.bringToFront()
    mResultRowViews[0] = findViewById(R.id.image_classification_top1_result_row)
    mResultRowViews[0]?.bringToFront()
    mResultRowViews[1] = findViewById(R.id.image_classification_top2_result_row)
    mResultRowViews[1]?.bringToFront()
    mResultRowViews[2] = findViewById(R.id.image_classification_top3_result_row)
    mResultRowViews[2]?.bringToFront()
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

    detect.setOnClickListener {

      //send http or websocket to get result

      val w: Int = openglview.width
      val h: Int = openglview.height
      println("in onTextMessage w:$w, h:$h")
      var center: Utils.NamedBox? = drawFaceResults_nbp(w, h)
      runOnUiThread { updateUI(center) }
    }
  }

  private fun updateUI(namedBox: Utils.NamedBox?) {
    if (namedBox != null) {

      for (i in 0 until Utils.TOP_K) {
        val rowView: ResultRowView? = mResultRowViews.get(i)
        rowView?.nameTextView?.text = namedBox.id_k[i]
        rowView?.scoreTextView?.text = String.format(Locale.US, RemoteFaceDetectActivity.SCORES_FORMAT,
                namedBox.prob_k[i])
        rowView?.setProgressState(true)
      }
    }
  }
  private fun drawFaceResults_nbp(width: Int, height: Int): Utils.NamedBox? {
    graphicOverlay.clear()
    println("in draw face results NBP")
    var color: Int
    var least_dist = 100.0
    var least_index: Utils.NamedBox? = null
    if (namedboxpool != null) {
      for (namedBox in namedboxpool) {
        val dist: Double = Utils.distance2middle(namedBox.rect)
        if (dist < least_dist) {
          least_index = namedBox
          least_dist = dist
        }
      }
    }
    if (namedboxpool != null) {
      for (namedBox in namedboxpool) {
        color = if (namedBox === least_index) {
          Color.BLUE
        } else Color.RED
        val xyxy = floatArrayOf(namedBox.rect[0] * width, namedBox.rect[1] * height, namedBox.rect[2] * width, namedBox.rect[3] * height)
        val rect = Rect(xyxy[0].toInt(), xyxy[1].toInt(), xyxy[2].toInt(), xyxy[3].toInt())
        val rectOverlay = RectOverlay(graphicOverlay, rect, namedBox.id, color)
        graphicOverlay.add(rectOverlay)
        graphicOverlay.add(rectOverlay)
      }
    }
    namedboxpool?.clear()
    return least_index
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
