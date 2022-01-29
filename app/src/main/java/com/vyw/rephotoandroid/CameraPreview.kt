package com.vyw.rephotoandroid

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.camera.lifecycle.ProcessCameraProvider
import android.os.Bundle
import com.vyw.rephotoandroid.R
import android.content.pm.PackageManager
import android.graphics.*
import com.vyw.rephotoandroid.CameraPreview
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.CameraSelector
import android.media.Image.Plane
import android.media.Image
import android.renderscript.*
import android.util.Log
import android.util.Size
import android.widget.ImageView
import androidx.camera.core.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import org.opencv.core.Mat
import com.vyw.rephotoandroid.OpenCVNative
import org.opencv.android.Utils
import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer
import java.util.concurrent.ExecutionException

// https://medium.com/swlh/introduction-to-androids-camerax-with-java-ca384c522c5
class CameraPreview : AppCompatActivity() {
    private var previewView: PreviewView? = null
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null

    //    private TextView textView;
    private var bProcessing = false
    var countFrames = 1
    private var MyCameraPreview: ImageView? = null
    private val bitmap: Bitmap? = null
    private var PreviewSizeWidth = 0
    private var PreviewSizeHeight = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        previewView = findViewById(R.id.previewView)
        MyCameraPreview = findViewById(R.id.arrow)
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        //        textView = findViewById(R.id.orientation2);
        if (!hasCameraPermission()) {
            requestPermission()
        }
        cameraProviderFuture!!.addListener({
            try {
                val cameraProvider = cameraProviderFuture!!.get()
                bindImageAnalysis(cameraProvider)
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            CAMERA_PERMISSION,
            CAMERA_REQUEST_CODE
        )
    }

    private fun bindImageAnalysis(cameraProvider: ProcessCameraProvider) {
        val imageAnalysis =
            ImageAnalysis.Builder() // enable the following line if RGBA output is needed.
                //                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
                .build()
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
//                ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
//                byte[] data = buffer.array();
//                int[] pixels = new int[data.length];
//                for (int i = 0; i < data.length; i++) {
//                    pixels[i] = new Byte(data[i]).intValue();
//                }
//            val imageBitmap = imageProxyToBitmap(imageProxy)
            val image = imageProxy.image
            if (image != null) {
                val imageBitmap = image.toBitmap();
                Log.i(TAG, "Run analysis")
                MyCameraPreview!!.setImageBitmap(imageBitmap)
                //                    run(imageBitmap);
            } else {
                Log.d(TAG, "imageBitmap is null, cannot run analysis")
            }
            imageProxy.close()
        }
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        preview.setSurfaceProvider(previewView!!.surfaceProvider)
        cameraProvider.bindToLifecycle(
            (this as LifecycleOwner), cameraSelector,
            imageAnalysis, preview
        )
    }

    fun Image.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer // Y
        val vuBuffer = planes[2].buffer // VU

        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()

        val nv21 = ByteArray(ySize + vuSize)

        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    //    Coppied from androidx/camera/core/internal/utils/ImageUtil.class
    private fun yuv_420_888toNv21(image: ImageProxy): ByteArray {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        yBuffer.rewind()
        uBuffer.rewind()
        vBuffer.rewind()
        val ySize = yBuffer.remaining()
        var position = 0
        // TODO(b/115743986): Pull these bytes from a pool instead of allocating for every image.
        val nv21 = ByteArray(ySize + image.width * image.height / 2)

        // Add the full y buffer to the array. If rowStride > 1, some padding may be skipped.
        for (row in 0 until image.height) {
            yBuffer[nv21, position, image.width]
            position += image.width
            yBuffer.position(
                Math.min(ySize, yBuffer.position() - image.width + yPlane.rowStride)
            )
        }
        val chromaHeight = image.height / 2
        val chromaWidth = image.width / 2
        val vRowStride = vPlane.rowStride
        val uRowStride = uPlane.rowStride
        val vPixelStride = vPlane.pixelStride
        val uPixelStride = uPlane.pixelStride

        // Interleave the u and v frames, filling up the rest of the buffer. Use two line buffers to
        // perform faster bulk gets from the byte buffers.
        val vLineBuffer = ByteArray(vRowStride)
        val uLineBuffer = ByteArray(uRowStride)
        for (row in 0 until chromaHeight) {
            vBuffer[vLineBuffer, 0, Math.min(vRowStride, vBuffer.remaining())]
            uBuffer[uLineBuffer, 0, Math.min(uRowStride, uBuffer.remaining())]
            var vLineBufferPosition = 0
            var uLineBufferPosition = 0
            for (col in 0 until chromaWidth) {
                nv21[position++] = vLineBuffer[vLineBufferPosition]
                nv21[position++] = uLineBuffer[uLineBufferPosition]
                vLineBufferPosition += vPixelStride
                uLineBufferPosition += uPixelStride
            }
        }
        return nv21
    }

    private fun toNv21(image: Image): ByteArray {
        val width = image.width
        val height = image.height

        // Order of U/V channel guaranteed, read more:
        // https://developer.android.com/reference/android/graphics/ImageFormat#YUV_420_888
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        // Full size Y channel and quarter size U+V channels.
        val numPixels = (width * height * 1.5f).toInt()
        val nv21 = ByteArray(numPixels)
        var idY = 0
        var idUV = width * height
        val uvWidth = width / 2
        val uvHeight = height / 2

        // Copy Y & UV channel.
        // NV21 format is expected to have YYYYVU packaging.
        // The U/V planes are guaranteed to have the same row stride and pixel stride.
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        for (y in 0 until height) {
            val yOffset = y * yRowStride
            val uvOffset = y * uvRowStride
            for (x in 0 until width) {
                nv21[idY++] = yBuffer[yOffset + x * yPixelStride]
                if (y < uvHeight && x < uvWidth) {
                    val bufferIndex = uvOffset + x * uvPixelStride
                    // V channel.
                    nv21[idUV++] = vBuffer[bufferIndex]
                    // U channel.
                    nv21[idUV++] = uBuffer[bufferIndex]
                }
            }
        }
        return nv21
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null
        require(image.format == ImageFormat.YUV_420_888) { "Only supports YUV_420_888." }
        val width = image.width
        val height = image.height
        val context = applicationContext
        val rs = RenderScript.create(context)
        val yuvByteArrayLength = (width * height * 1.5f).toInt()
        val yuvType = Type.Builder(rs, Element.U8(rs))
            .setX(yuvByteArrayLength)
        val `in` = Allocation.createTyped(
            rs, yuvType.create(), Allocation.USAGE_SCRIPT
        )
        val rgbaType = Type.Builder(rs, Element.RGBA_8888(rs))
            .setX(width)
            .setY(height)
        val out = Allocation.createTyped(
            rs, rgbaType.create(), Allocation.USAGE_SCRIPT
        )
        val script = ScriptIntrinsicYuvToRGB.create(
            rs, Element.U8_4(rs)
        )
        val yuvByteArray = toNv21(image)
        `in`.copyFrom(yuvByteArray)
        script.setInput(`in`)
        script.forEach(out)

        // Allocate memory for the bitmap to return. If you have a reusable Bitmap
        // I recommending using that.
        val bitmap = Bitmap.createBitmap(
            image.width, image.height, Bitmap.Config.ARGB_8888
        )
        out.copyTo(bitmap)
        return bitmap


//        byte[] nv21 = this.yuv_420_888toNv21(imageProxy);
//        int width = imageProxy.getWidth();
//        int height = imageProxy.getHeight();
//
//        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
//        ByteArrayOutputStream os = new ByteArrayOutputStream();
//        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, os);
//        byte[] jpegByteArray = os.toByteArray();
//        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
//        return bitmap;


//        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//        return rotatedBitmap;


//        Image image = imageProxy.getImage();
//        if (image == null) {
//            return null;
//        }
//
//        Image.Plane[] planes = image.getPlanes();
//        ByteBuffer yBuffer = planes[0].getBuffer();
//        ByteBuffer uBuffer = planes[1].getBuffer();
//        ByteBuffer vBuffer = planes[2].getBuffer();
//
//        int ySize = yBuffer.remaining();
//        int uSize = uBuffer.remaining();
//        int vSize = vBuffer.remaining();
//
//        byte[] nv21 = new byte[ySize + uSize + vSize];
//        //U and V are swapped
//        yBuffer.get(nv21, 0, ySize);
//        vBuffer.get(nv21, ySize, vSize);
//        uBuffer.get(nv21, ySize + vSize, uSize);
//
//        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);
//
//        byte[] imageBytes = out.toByteArray();
//        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);


//        Image.Plane[] planes = image.getPlanes();
//        ByteBuffer buffer = planes[0].getBuffer();
//        int pixelStride = planes[0].getPixelStride();
//        int rowStride = planes[0].getRowStride();
//        int rowPadding = rowStride - pixelStride * image.getWidth();
//        Bitmap bitmap = Bitmap.createBitmap(image.getWidth() + rowPadding / pixelStride,
//                image.getHeight(), Bitmap.Config.ARGB_8888);
//        bitmap.copyPixelsFromBuffer(buffer);
//
//        int rotation = imageProxy.getImageInfo().getRotationDegrees();
//        Matrix matrix = new Matrix();
//        matrix.postRotate(rotation);
//
//        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//        return rotatedBitmap;


//        Image.Plane[] planes = image.getPlanes();
//        ByteBuffer rBuffer = planes[0].getBuffer();
//        ByteBuffer gBuffer = planes[1].getBuffer();
//        ByteBuffer bBuffer = planes[2].getBuffer();
//        ByteBuffer aBuffer = planes[3].getBuffer();
//
//        int width = image.getWidth();
//        int height = image.getHeight();
//        int stride = planes[0].getPixelStride();
//
//        int j = 0;
//        int[] colors = new int[image.getWidth() * image.getHeight()];
////        for (int i = 0; i < rBuffer.)
////        colors[y * STRIDE + x] = (aBuffer << 24) | (rBuffer << 16) | (gBuffer << 8) | bBuffer;
//        return null;
    }

    private fun run(bitmap: Bitmap) {
        bProcessing = true
        val canvas = Canvas(bitmap)
        val mPaint = Paint()
        mPaint.style = Paint.Style.FILL
        mPaint.color = Color.WHITE
        mPaint.strokeWidth = 4f
        var width = 150f
        var height = 150f
        PreviewSizeWidth = 1280
        PreviewSizeHeight = 720
        var x = PreviewSizeWidth / 2 - width / 2
        var y = (PreviewSizeHeight / 10).toFloat()
        canvas.drawRect(x, y, x + width, y + height, mPaint)
        mPaint.color = Color.BLACK
        width = width - 10f
        height = height - 10f
        x = x + 5f
        y = y + 5f
        canvas.drawRect(x, y, x + width, y + height, mPaint)
        mPaint.color = Color.YELLOW
        width = width - 10f
        height = height - 10f
        x = x + 5f
        y = y + 5f
        val currentFrames = Mat()
        Utils.bitmapToMat(bitmap, currentFrames)
        var direction: Int
        try {
//            OPENCVNATIVECALL
            direction = OpenCVNative.processNavigation(currentFrames.nativeObjAddr, 1)
            Log.d(TAG, "Value of direction is: $direction")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, e.message!!)
            direction = 1
        }


        //1 up, 2 down, 3 right, 4 left;
        val wallpath = Path()
        wallpath.reset()
        if (direction < 10) {
            when (direction) {
                0 -> {
                    wallpath.moveTo(x, y + height / 8)
                    wallpath.lineTo(x + width / 8, y)
                    wallpath.lineTo(x + width, y + height * 7 / 8)
                    wallpath.lineTo(x + width * 7 / 8, y + height)
                    wallpath.moveTo(x + width, y + height / 8)
                    wallpath.lineTo(x + width * 7 / 8, y)
                    wallpath.lineTo(x, y + height * 7 / 8)
                    wallpath.lineTo(x + width / 8, y + height)
                }
                1 -> {
                    wallpath.moveTo(x + width / 4, y + height / 2)
                    wallpath.lineTo(x + width / 4 + width / 2, y + height / 2)
                    wallpath.lineTo(x + width / 4 + width / 2, y + height)
                    wallpath.lineTo(x + width / 4 - 5f, y + height)
                    wallpath.lineTo(x + width / 4 - 5f, y + height / 2)
                    wallpath.moveTo(x, y + height / 2)
                    wallpath.lineTo(x + width, y + height / 2)
                    wallpath.lineTo(x + width / 2, y)
                }
                2 -> {
                    wallpath.moveTo(x + width / 4, y)
                    wallpath.lineTo(x + width / 4 + width / 2, y)
                    wallpath.lineTo(x + width / 4 + width / 2, y + height / 2)
                    wallpath.lineTo(x + width / 4 - 5f, y + height / 2)
                    wallpath.lineTo(x + width / 4 - 5f, y)
                    wallpath.moveTo(x, y + height / 2)
                    wallpath.lineTo(x + width, y + height / 2)
                    wallpath.lineTo(x + width / 2, y + height)
                }
                3 -> {
                    wallpath.moveTo(x, y + height / 4)
                    wallpath.lineTo(x, y + height / 4 + height / 2)
                    wallpath.lineTo(x + width / 2, y + height / 4 + height / 2)
                    wallpath.lineTo(x + width / 2, y + height / 4 - 5f)
                    wallpath.lineTo(x, y + height / 4 - 5f)
                    wallpath.moveTo(x + width / 2, y)
                    wallpath.lineTo(x + width / 2, y + height)
                    wallpath.lineTo(x + width, y + height / 2)
                }
                4 -> {
                    wallpath.moveTo(x + width / 2, y)
                    wallpath.lineTo(x + width / 2, y + height)
                    wallpath.lineTo(x, y + height / 2)
                    wallpath.moveTo(x + width / 2, y + height / 4)
                    wallpath.lineTo(x + width / 2, y + height / 4 + height / 2)
                    wallpath.lineTo(x + width, y + height / 4 + height / 2)
                    wallpath.lineTo(x + width, y + height / 4 - 5f)
                    wallpath.lineTo(x + width / 2, y + height / 4 - 5f)
                }
            }
        } else {
            wallpath.moveTo(x + width / 2, y + height / 8)
            wallpath.lineTo(x + width / 8, y + height / 2)
            wallpath.lineTo(x + width / 2, y + height * 7 / 8)
            wallpath.lineTo(x + width * 7 / 8, y + height / 2)
            wallpath.lineTo(x + width / 2, y + height / 8)
            when (direction) {
                13 -> {
                    wallpath.moveTo(x + width * 1 / 3, y)
                    wallpath.lineTo(x + width, y + height * 2 / 3)
                    wallpath.lineTo(x + width, y)
                    canvas.drawPath(wallpath, mPaint)
                }
                14 -> {
                    wallpath.moveTo(x + width * 2 / 3, y)
                    wallpath.lineTo(x, y + height * 2 / 3)
                    wallpath.lineTo(x, y)
                    canvas.drawPath(wallpath, mPaint)
                }
                23 -> {
                    wallpath.moveTo(x + width * 2 / 3, y + height)
                    wallpath.lineTo(x, y + height * 1 / 3)
                    wallpath.lineTo(x, y + height)
                    canvas.drawPath(wallpath, mPaint)
                }
                24 -> {
                    wallpath.moveTo(x + width * 1 / 3, y + height)
                    wallpath.lineTo(x + width, y + height * 1 / 3)
                    wallpath.lineTo(x + width, y + height)
                    canvas.drawPath(wallpath, mPaint)
                }
            }
        }
        canvas.drawPath(wallpath, mPaint)
        MyCameraPreview!!.setImageBitmap(bitmap)
        bProcessing = false
        countFrames++
    }

    companion object {
        private val CAMERA_PERMISSION = arrayOf(Manifest.permission.CAMERA)
        private const val CAMERA_REQUEST_CODE = 10
        private const val TAG = "CameraPreview"
    }
}