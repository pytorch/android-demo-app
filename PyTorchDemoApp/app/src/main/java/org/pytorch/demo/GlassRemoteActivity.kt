package org.pytorch.demo

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.hardware.usb.UsbConstants.USB_CLASS_MISC
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.neovisionaries.ws.client.*
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import com.valdesekamdem.library.mdtoast.MDToast
import kotlinx.android.synthetic.main.activity_glass_remote.*
import kotlinx.android.synthetic.main.activity_glass_remote.graphicOverlay
import kotlinx.android.synthetic.main.activity_remote_face_detect.*
import net.ossrs.rtmp.ConnectCheckerRtmp
import org.json.JSONException
import org.json.JSONObject
import org.pytorch.demo.Utils.TOP_K
import org.pytorch.demo.Utils.cropBitmap
import org.pytorch.demo.streamlib.RtmpUSB
import org.pytorch.demo.util.Util
import org.pytorch.demo.vision.Helper.RectOverlay
import org.pytorch.demo.vision.view.ResultRowView
import java.io.IOException
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
      MDToast.makeText(this, "连接成功", MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show()
    }
  }


  override fun onConnectionFailedRtmp(reason: String) {
    runOnUiThread {
      MDToast.makeText(this, "连接失败，由于$reason", MDToast.LENGTH_SHORT, MDToast.TYPE_WARNING).show()
      rtmpUSB.stopStream(uvcCamera)
    }
  }

  override fun onAuthErrorRtmp() {
    Log.e("Pedro", "auth error")
  }

  override fun onDisconnectRtmp() {
    runOnUiThread {
      MDToast.makeText(this, "断开连接", MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show()
    }
  }

  override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
  }

  override fun surfaceDestroyed(p0: SurfaceHolder?) {

  }

  override fun surfaceCreated(p0: SurfaceHolder?) {

  }
  private var namedboxpool: ArrayList<Utils.NamedBox>? = null
  private lateinit var usbMonitor: USBMonitor
  private var uvcCamera: UVCCamera? = null
  private var isUsbOpen = true
  private val width = 1280
  private val height = 720
  private var defished = false
  private lateinit var rtmpUSB: RtmpUSB
  private var ctrlBlock: USBMonitor.UsbControlBlock? = null
  private val mResultRowViews = arrayOfNulls<ResultRowView>(Utils.TOP_K)
  private var webSocketFactory: WebSocketFactory? = null
  private var webSocket: WebSocket? = null
  private var server_state_ready = false
  private var imageView2: ImageView? = null
  private var detect: Button? = null

  //    private Button offline_loginbtn;
  //    @Override
  //    public void onPointerCaptureChanged(boolean hasCapture) {
  //    }
  //
  //    private static class PageData {
  //        private int titleTextResId;
  //        private int imageResId;
  //        private int descriptionTextResId;
  //
  //        public PageData(int titleTextResId, int imageResId, int descriptionTextResId) {
  //            this.titleTextResId = titleTextResId;
  //            this.imageResId = imageResId;
  //            this.descriptionTextResId = descriptionTextResId;
  //        }
  //    }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_glass_remote)
//    requestPermission()
    graphicOverlay.bringToFront()
    imageView2 = findViewById(R.id.imageView2)
    imageView2!!.bringToFront()
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
          MDToast.makeText(this@GlassRemoteActivity, "开始推流，地址"+et_url.text.toString(), MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show()
          startStream(et_url.text.toString())
          start_stop.text = "停止推流"
        } else {
          rtmpUSB.stopStream(uvcCamera)
          start_stop.text = "开始推流"
        }
      }
    }

    detect = findViewById(R.id.btn_detect)
    detect!!.setOnClickListener {

      //send http or websocket to get result
      val msg = "{\"event\": \"detect\"}"
      if (server_state_ready) {
        webSocket!!.sendText(msg)
        println("detect sent")
      } else {
        MDToast.makeText(this@GlassRemoteActivity, "server还没有准备好", MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show()
      }
    }



    namedboxpool = ArrayList<Utils.NamedBox>()
    webSocketFactory = WebSocketFactory()
    var serverUri = util.websocket_TEMPLATE;
    val token = util.GetToken()
    try {

//      if (token != null)
//        serverUri = serverUri.replace("{TOKEN}", Utils.token)
      serverUri = serverUri.replace("{RTMP}", "livestream")
      println("in rfda, serveruri $serverUri")
      webSocket = webSocketFactory!!.createSocket(serverUri)
      val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
      StrictMode.setThreadPolicy(policy)
      webSocket!!.connect()
      webSocket!!.addListener(object : WebSocketAdapter() {
        override fun onTextMessage(websocket: WebSocket, message: String) {
          // Received a text message.
          println("in listener, text message received $message")

          //TODO info can be retrived here
          // need further operation
          // put box and info into boxpool

          if (message != null) {
            if (message.contains("ready")) {
              server_state_ready = true
              MDToast.makeText(this@GlassRemoteActivity, "ws连接成功", MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show()
            }
            if(server_state_ready)
              updateNamedboxpool(message)
          } else println("in onTextMessage message is null")
          val w: Int = openglview.width
          val h: Int = openglview.height
          println("in onTextMessage w:$w, h:$h")
          var center: Utils.NamedBox? = drawFaceResults_nbp(w, h)
          runOnUiThread { updateUI(center, null) }
        }
      })
      webSocket!!.addListener(object : WebSocketAdapter() {
        override fun onBinaryMessage(webSocket: WebSocket, bytes: ByteArray) {
          println("in binary message listener received bytes of size " + bytes.size)
        }
      })
    } catch (ioe: IOException) {
      println(ioe.toString())
    } catch (e: OpeningHandshakeException) {
      // A violation against the WebSocket protocol was detected
      // during the opening handshake.
    } catch (e: HostnameUnverifiedException) {
      // The certificate of the peer does not match the expected hostname.
    } catch (e: WebSocketException) {
      // Failed to establish a WebSocket connection.
    } catch (re: RuntimeException) {
      MDToast.makeText(this@GlassRemoteActivity, "远程服务器错误", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
      finishActivity(11)
    }

  }
  private fun updateNamedboxpool(jsonString: String) {
    try {
      val jsonObject = JSONObject(jsonString)
      val count = jsonObject.getInt("count")
      //            int count = jsonObject.length();
      val id_array = jsonObject.getJSONArray("id")
      val id_k_array = jsonObject.getJSONArray("id_k")
      val prob_array = jsonObject.getJSONArray("prob")
      val box_array = jsonObject.getJSONArray("box")
      //TODO change career to contain more info ALSO: format of career not right.
      val info = jsonObject.getString("career")
      for (i in 0 until count) {
        val id_k = arrayOfNulls<String>(TOP_K)
        val prob = FloatArray(TOP_K)
        val box = FloatArray(4)
        for (j in 0 until TOP_K) {
          id_k[j] = id_k_array.getJSONArray(i).getString(j)
          prob[j] = prob_array.getJSONArray(i).getDouble(j).toFloat()
        }
        for (j in 0..3) {
          box[j] = box_array.getJSONArray(i).getDouble(j).toFloat()
        }
        val namedBox: Utils.NamedBox = Utils.NamedBox(
                id_array.getString(i),
                id_k,
                prob,
                box,
                info
        )
        namedboxpool!!.add(namedBox)
      }
    } catch (jsonException: JSONException) {
      jsonException.printStackTrace()
    }
  }
  private fun updateUI(namedBox: Utils.NamedBox?, bitmap: Bitmap?) {
    if (namedBox != null) {

      for (i in 0 until Utils.TOP_K) {
        val rowView: ResultRowView? = mResultRowViews[i]
        rowView?.nameTextView?.text = namedBox.id_k[i]
        rowView?.scoreTextView?.text = String.format(Locale.US, RemoteFaceDetectActivity.SCORES_FORMAT,
                namedBox.prob_k[i])
        rowView?.setProgressState(true)
      }
      if(bitmap != null){
        var bitmap_c: Bitmap? = null
        bitmap_c = cropBitmap(bitmap, namedBox.rect)
        imageView2!!.setImageBitmap(bitmap_c)
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
      for (namedBox in namedboxpool!!) {
        val dist: Double = Utils.distance2middle(namedBox.rect)
        if (dist < least_dist) {
          least_index = namedBox
          least_dist = dist
        }
      }
    }
    if (namedboxpool != null) {
      for (namedBox in namedboxpool!!) {
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
        MDToast.makeText(this@GlassRemoteActivity, "usb设备已接入with class " + device.getDeviceClass(), MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show()

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
      MDToast.makeText(this@GlassRemoteActivity, "打开usb相机完成", MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show()
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
      MDToast.makeText(this@GlassRemoteActivity, "开始预览", MDToast.LENGTH_SHORT, MDToast.TYPE_INFO).show()
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
