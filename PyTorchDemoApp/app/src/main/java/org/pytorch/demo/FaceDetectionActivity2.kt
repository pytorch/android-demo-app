package org.pytorch.demo
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.valdesekamdem.library.mdtoast.MDToast
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.demo.Utils.*
import org.pytorch.demo.util.Util
import org.pytorch.demo.vision.Helper.GraphicOverlay
import org.pytorch.demo.vision.view.ResultRowView
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.LensFacingConverter
import androidx.camera.core.impl.PreviewConfig
import androidx.camera.core.impl.VideoCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView


private const val REQUEST_CODE_PERMISSIONS = 10
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
private val tag = FaceDetectionActivity2::class.java.simpleName

@SuppressLint("RestrictedApi, ClickableViewAccessibility")
class FaceDetectionActivity2 : AppCompatActivity(), LifecycleOwner {

    private var lensFacing: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var preview: Preview
    private lateinit var cameraSelector: CameraSelector
    private lateinit var namedboxpool: ArrayList<NamedBox>
    private lateinit var viewFinder: PreviewView
    private lateinit var captureButton: Button
    private lateinit var videoCapture: VideoCapture
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var btn_getImage: Button
    private lateinit var imageView: ImageView
    private lateinit var graphicOverlay: GraphicOverlay
    private lateinit var namedEmbeddings: ArrayList<NamedEmbedding>
    private lateinit var switch_cam: ImageView

    private var mResultRowViews = arrayOfNulls<ResultRowView>(Utils.TOP_K)
    private lateinit var mFpsText: TextView
    private lateinit var mMsText: TextView
    private lateinit var mMsAvgText: TextView

    //    private String deliminator = "\\$\\$\\$\\$\\$\\$\\$\\$\\$\\$";
    private fun update_embeddings(str: String) {
        val strings = str.split(Util(this).deliminator.toRegex()).toTypedArray()
        for (s in strings) {
            if (s.length > 100) {
                namedEmbeddings.add(NamedEmbedding(s))
            }
        }
        val namedEmbeddings1 = java.util.ArrayList<NamedEmbedding>()
        for (namedEmbedding in namedEmbeddings) {
            if (namedEmbedding.id == null) continue else namedEmbeddings1.add(namedEmbedding)
        }
        namedEmbeddings = namedEmbeddings1
        println("namedEmbeddings.size() " + namedEmbeddings.size)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_face_detection2)

        viewFinder = findViewById(R.id.view_finder)
        captureButton = findViewById(R.id.capture_button)
        btn_getImage = findViewById(R.id.btn_getImage)
        imageView = findViewById(R.id.imageView)
        graphicOverlay = findViewById(R.id.graphicOverlay)
        graphicOverlay.bringToFront()
        switch_cam = findViewById(R.id.img_view_switch)
        switch_cam.bringToFront()
//        setBounds(R.mipmap.switch_camera, switch_cam)
        namedEmbeddings = ArrayList()
        namedboxpool = ArrayList()


        mResultRowViews[0] = findViewById(R.id.image_classification_top1_result_row)
        mResultRowViews[1] = findViewById(R.id.image_classification_top2_result_row)
        mResultRowViews[2] = findViewById(R.id.image_classification_top3_result_row)



        val embds = Util()._embeddings_from_files
        update_embeddings(embds)

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        val file = File(externalMediaDirs.first(),
                "${System.currentTimeMillis()}.mp4")

        btn_getImage.setOnClickListener { view : View ->
            var bitmap = viewFinder.bitmap
            imageView.setImageBitmap(bitmap)
            runOnUiThread {
                if (bitmap != null) {
                    analyzeImage(bitmap)
                }
            }
        }

        switch_cam.setOnClickListener{ view : View ->
            if(lensFacing == CameraSelector.DEFAULT_BACK_CAMERA) {
                lensFacing = CameraSelector.DEFAULT_FRONT_CAMERA
                bindCameraUseCases()
            }
            else{
                lensFacing = CameraSelector.DEFAULT_BACK_CAMERA
                bindCameraUseCases()
            }
        }

        captureButton.setOnClickListener {
            if (captureButton.text.equals("录像")) {
                captureButton.setBackgroundColor(Color.GREEN)
                captureButton.setText("录像中...")
                switch_cam.isEnabled = false
                videoCapture.startRecording(file, ContextCompat.getMainExecutor(this), object: VideoCapture.OnVideoSavedCallback{
                    override fun onVideoSaved(file: File) {
                        MDToast.makeText(this@FaceDetectionActivity2,"文件保存到$file", MDToast.LENGTH_LONG, MDToast.TYPE_INFO).show()
                        Log.i(tag, "Video File : $file")
                    }

                    override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                        Log.i(tag, "Video Error: $message")
                    }

//                    override fun onError(videoCaptureError: VideoCapture.VideoCaptureError, message: String, cause: Throwable?) {
////                        TODO("Not yet implemented")
//                        Log.i(tag, "Video Error: $message")
//                    }
                })

            } else if (captureButton.text.equals("录像中...")) {
                captureButton.setBackgroundColor(Color.WHITE)
                captureButton.setText("录像")
                videoCapture.stopRecording()
                switch_cam.isEnabled = true
                Log.i(tag, "Video File stopped")
            }
            false
        }

//        captureButton.setOnTouchListener { _, event ->
//            if (event.action == MotionEvent.ACTION_DOWN) {
//                captureButton.setBackgroundColor(Color.GREEN)
//                videoCapture.startRecording(file, ContextCompat.getMainExecutor(this), object: VideoCapture.OnVideoSavedCallback{
//                    override fun onVideoSaved(file: File) {
//                        MDToast.makeText(this@FaceDetectionActivity2,"文件保存到$file", MDToast.LENGTH_LONG, MDToast.TYPE_INFO)
//                        Log.i(tag, "Video File : $file")
//                    }
//
//                    override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
//                        Log.i(tag, "Video Error: $message")
//                    }
//
////                    override fun onError(videoCaptureError: VideoCapture.VideoCaptureError, message: String, cause: Throwable?) {
//////                        TODO("Not yet implemented")
////                        Log.i(tag, "Video Error: $message")
////                    }
//                })
//
//            } else if (event.action == MotionEvent.ACTION_UP) {
//                captureButton.setBackgroundColor(Color.RED)
//                videoCapture.stopRecording()
//                Log.i(tag, "Video File stopped")
//            }
//            false
//        }
    }
    private fun bindCameraUseCases() {
        // Make sure that there are no other use cases bound to CameraX
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll();
            // Preview
            preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(viewFinder.createSurfaceProvider())
                    }

            // Select back camera as a default
            cameraSelector = lensFacing


            videoCapture = VideoCapture.Builder()
                    .setVideoFrameRate(35)
//                    .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                    .setTargetResolution(Size(960, 1280))
                    .build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, videoCapture)

            } catch(exc: Exception) {
                Log.e("tag", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
    /**
     *
     * @param drawableId  drawableLeft  drawableTop drawableBottom 所用的选择器 通过R.drawable.xx 获得
     * @param button  需要限定图片大小的ImageButton
     */
    private fun setBounds(drawableId: Int, button: ImageButton) {
        //定义底部标签图片大小和位置
        val drawable_news = resources.getDrawable(drawableId)
        //当这个图片被绘制时，给他绑定一个矩形 ltrb规定这个矩形  (这里的长和宽写死了 自己可以可以修改成 形参传入)
        drawable_news.setBounds(0, 0, 50, 50)
        button.setImageDrawable(drawable_news);
    }
    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                            this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

//    private fun startCamera() {
//        // Create configuration object for the viewfinder use case
////        val previewConfig = PreviewConfig.Builder().build()
//        // Build the viewfinder use case
//        val preview = Preview.Builder()
//        val lensfacing: CameraSelector.LensFacing
//        val cameraSelector = CameraSelector.Builder().requireLensFacing().build()
//
////        val videoCaptureConfig = VideoCaptureConfig.
////        // Create a configuration object for the video use case
////        val videoCaptureConfig = VideoCaptureConfig.Builder().apply {
////            setTargetRotation(viewFinder.display.rotation)
//////            setTargetAspectRatio(Rational(4,3))
//////            setTargetResolution(Size(1280,960))
////                    .setLensFacing(CameraX.LensFacing.BACK)
////                    .setVideoFrameRate(35)
//////                    .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
////                    .setTargetResolution(Size(960, 1280))
////                    .setTargetAspectRatio(Rational(3,4))
////        }.build()
//        videoCapture = VideoCapture.Builder()
//                .setVideoFrameRate(35)
////                    .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
//                .setTargetResolution(Size(960, 1280))
//                .setTargetAspectRatio(Rational(3,4))
//                .build()
//
//        preview.set
////        preview.setOnPreviewOutputUpdateListener {
////            viewFinder.surfaceTexture = it.surfaceTexture
////        }
//
////        val imageAnalysisConfig = ImageAnalysisConfig.Builder().apply {
////            setTargetRotation(viewFinder.display.rotation)
////        }.build()
////        imageAnalysis = ImageAnalysis(imageAnalysisConfig)
////        imageAnalysis.setAnalyzer(ImageAnalysis.Analyzer { image, rotation->
////            Log.i("image analysis", "analyzing " + image.height + " " + image.width)
////            // insert your code here.
////        })
//        // Bind use cases to lifecycle
//        CameraX.bindToLifecycle(this, preview, videoCapture)
//    }
    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//
//        cameraProviderFuture.addListener(Runnable {
//            // Used to bind the lifecycle of cameras to the lifecycle owner
//            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
//
//            // Preview
//            preview = Preview.Builder()
//                    .build()
//                    .also {
//                        it.setSurfaceProvider(viewFinder.createSurfaceProvider())
//                    }
//
//            // Select back camera as a default
//            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//
//            videoCapture = VideoCapture.Builder()
//                .setVideoFrameRate(35)
////                    .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
//                .setTargetResolution(Size(960, 1280))
//                .build()
//
//            try {
//                // Unbind use cases before rebinding
//                cameraProvider.unbindAll()
//
//                // Bind use cases to camera
//                cameraProvider.bindToLifecycle(
//                        this, cameraSelector, preview, videoCapture)
//
//            } catch(exc: Exception) {
//                Log.e("tag", "Use case binding failed", exc)
//            }
//
//        }, ContextCompat.getMainExecutor(this))

    bindCameraUseCases()
    }

    public var mModule: Module? = null
    public var encoder: Module? = null

    public fun analyzeImage(bitmap: Bitmap){
        var midbox: NamedBox? = null
        var bitmap2show: Bitmap? = null
        var moduleForwardDuration: Long = 0
        var moduleAnalysisDuration: Long = 0
        try {
            if (mModule == null || encoder == null) {
                try {
                    val moduleFileAbsoluteFilePath = File(
                            Utils.assetFilePath(this, "mobile_model2.pt")).absolutePath
                    mModule = Module.load(moduleFileAbsoluteFilePath)
                    val encoderFileAbsoluteFilePath = File(
                            Utils.assetFilePath(this, "encoder1.pt")).absolutePath
                    encoder = Module.load(encoderFileAbsoluteFilePath)
                } catch (e: Exception) {
                    MDToast.makeText(this, "读取模型时出错", MDToast.LENGTH_LONG, MDToast.TYPE_ERROR).show()
                    e.printStackTrace()
                }
            }
            bitmap2show = bitmap
            val startTime = SystemClock.elapsedRealtime()
            val mean = floatArrayOf(0.49804f, 0.49804f, 0.49804f)
            val std = floatArrayOf(0.501960f, 0.501960f, 0.501960f)
            val nms_threshold = 0.4f
            val w1 = bitmap.width
            val h1 = bitmap.height
            val bitmap1 = Bitmap.createScaledBitmap(bitmap, 480, 360, true)
            //            if (bitmap.getWidth() > bitmap.getHeight())
//                bitmap = Bitmap.createScaledBitmap(bitmap,480,360,true);
//            else
//                bitmap = Bitmap.createScaledBitmap(bitmap,360,480,true);
            val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap1, mean, std)
            println(Arrays.toString(inputTensor.shape()))
            //Normalize input
//        inputTensor = Normalize_tensor(inputTensor, 127, 128);
            // running the model
            val moduleForwardStartTime = SystemClock.elapsedRealtime()
            val output: Array<IValue> = mModule?.forward(IValue.from(inputTensor))!!.toTuple()
            moduleForwardDuration = SystemClock.elapsedRealtime() - moduleForwardStartTime

//            final Tensor outputTensor = mModule.forward(IValue.from(mInputTensor)).toTensor();
            val scores = output[0].toTensor()
            val boxes = output[1].toTensor()
            val threshold = 0.8f
            val possible_indexes: ArrayList<Float> = possible_score(scores, boxes, threshold)
            println("in onCreate len possible_indexes " + possible_indexes.size)
            var nms_boxes: ArrayList<FloatArray?> = nms(boxes, scores, possible_indexes, nms_threshold)
            val ratio = 0.1f
            nms_boxes = expand_box(nms_boxes, ratio)
            if (nms_boxes.size > 0) {

//
//                drawOnBitmap(bitmap, nms_boxes);
                val width: Int = viewFinder.width
                val height = viewFinder.height
                println("in if width is $width height is $height")

                //TODO make http request here to get identity of picture
//                    send_unrecognized_image(bitmap, nms_boxes);
                get_unrecognized_face_embedding(bitmap, nms_boxes, encoder, namedEmbeddings, namedboxpool)
                midbox = drawFaceResults(nms_boxes, width, height - 100, graphicOverlay, namedboxpool)
                //                set_prediction(bitmap, midbox);
                update_namedboxpool()
                println("nms boxes " + nms_boxes.size)
            } else {
                graphicOverlay.clear()
                update_namedboxpool()
            }
            moduleAnalysisDuration = SystemClock.elapsedRealtime() - moduleForwardStartTime
            println("inference time is $moduleForwardDuration")


        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error during image analysis", e)
            finish()

        }
        if (midbox == null) {
        } else {
            var bitmap_c: Bitmap? = null
            bitmap_c = cropBitmap(bitmap2show, midbox.rect)
            updateUI(arrayOf(midbox.id_k[0], midbox.id_k[1], midbox.id_k[2]), floatArrayOf(midbox.prob_k[0], midbox.prob_k[1], midbox.prob_k[2]), bitmap_c,
                    moduleForwardDuration, moduleAnalysisDuration)
        }
    }

    private fun updateUI(ids: Array<String>, probs: FloatArray, bitmap_c: Bitmap?, moduleForwardDuration: Long, moduleAnalysisDuration: Long) {

        if (bitmap_c != null) {
            imageView.setImageBitmap(bitmap_c)
            convertDis2Prob(ids, probs)
            for (i in 0 until Utils.TOP_K) {
                val rowView:ResultRowView? = mResultRowViews[i]
                rowView?.nameTextView?.setText(ids.get(i))
                rowView?.scoreTextView?.text = String.format(Locale.US, SCORES_FORMAT,
                        probs.get(i))
                rowView?.setProgressState(false)
            }
        }

//        mMsText.setText(String.format(Locale.US, Utils.FORMAT_MS, moduleForwardDuration))
//        if (mMsText.getVisibility() != View.VISIBLE) {
//            mMsText.setVisibility(View.VISIBLE)
//        }
//        mFpsText.setText(String.format(Locale.US, Utils.FORMAT_FPS, 1000f / moduleAnalysisDuration))
//        if (mFpsText.getVisibility() != View.VISIBLE) {
//            mFpsText.setVisibility(View.VISIBLE)
//        }

    }


    //    public void send_croped_Image(Bitmap bitmap_c, float[] box_c)
    //    {
    //        System.out.println("in send cropped Image");
    ////                webSocket.sendText("{\"message\": \"on create\"}");
    //        if (bitmap_c != null)
    //        {
    //            try {
    //                String box = "["+box_c[0]+","+box_c[1]+","+box_c[2]+","+box_c[3]+ "]";
    //                String string = "{\"message\": \"cropped image\",\"box\": "+ box +"}";
    //                String deliminater = "$$$$$$$$$$";
    //                string = string + deliminater;
    //                byte[] bytes1 = string.getBytes("UTF-8");
    //                ByteArrayOutputStream baos = new ByteArrayOutputStream();
    //                bitmap_c.compress(Bitmap.CompressFormat.JPEG, 100, baos);
    //                byte[] bytes = baos.toByteArray();
    //                System.out.println("in onclick bytes len is " + bytes.length);
    //                webSocket.sendBinary(byteMerger(bytes1, bytes));
    //            }catch (java.io.UnsupportedEncodingException uee)
    //            {
    //                uee.printStackTrace();
    //            }
    //
    //        }
    //    }
    fun update_namedboxpool() {
        val new_namedboxpool = ArrayList<NamedBox>()
        for (nb in namedboxpool) {
            if (nb.is_valid) new_namedboxpool.add(nb) //注意这个地方
        }
        namedboxpool = new_namedboxpool
    }
}
