package com.example.faceswap

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face


class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"
    private val face1Tab = 0
    private val face2Tab = 1
    private val pickImage = 100
    private var selectedTab = 0

    private val desiredWidth = 800
    private val desiredHeight = 800

    private var imageUriFace1: Uri? = null
    private var imageUriFace2: Uri? = null

    private lateinit var bitmap1: Bitmap
    private lateinit var bitmap2: Bitmap
    private lateinit var bitmap1Swapped: Bitmap
    private lateinit var bitmap2Swapped: Bitmap

    private lateinit var imageView1: ImageView
    private lateinit var imageView2: ImageView
    private lateinit var swapBt: Button
    private lateinit var undoBt: Button

    private lateinit var image1Bt: Button
    private lateinit var image2Bt: Button

    private lateinit var faces1: List<Face>
    private lateinit var faces2: List<Face>
    private val faceDetectorEngine = FaceDetectorEngine()

    private var face1Done = false
    private var face2Done = false
    private var okToSwap = false
    private var hasSwapped = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        image1Bt = findViewById(R.id.image1)
        image2Bt = findViewById(R.id.image2)

        swapBt = findViewById(R.id.swapBt)
        undoBt = findViewById(R.id.undoBt)
        swapBt.isEnabled = false
        imageView1 = findViewById(R.id.imageView1)
        imageView2 = findViewById(R.id.imageView2)

        savedInstanceState?.let {
            imageUriFace1 = it.getParcelable(KEY_IMAGE_URI_1)
            imageUriFace2 = it.getParcelable(KEY_IMAGE_URI_2)
        }
        image1Bt.setOnClickListener {
            val gallery =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
            Log.d(tag, "Open gallery to select image 1.")
            selectedTab = face1Tab
        }
        image2Bt.setOnClickListener {
            val gallery =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
            Log.d(tag, "Open gallery to select image 2.")
            selectedTab = face2Tab
        }

        // Click listener for action button, should result in face swap.

        undoBt.setOnClickListener {
            Log.d(tag, "Undo clicked.")
            if (hasSwapped) {
                val landmarksForFaces1 = Landmarks.arrangeLandmarksForFaces(faces1)
                val landmarksForFaces2 = Landmarks.arrangeLandmarksForFaces(faces2)

                bitmap1Swapped =
                    Swap.faceSwapAll(bitmap2, bitmap1, landmarksForFaces2, landmarksForFaces1)

                bitmap2Swapped =
                    Swap.faceSwapAll(bitmap1, bitmap2, landmarksForFaces1, landmarksForFaces2)

                imageView1.setImageBitmap(bitmap1)

                imageView2.setImageBitmap(bitmap2)

                hasSwapped = false;
            }
        }
        swapBt.setOnClickListener {
            Log.d(tag, "Action button clicked.")

            if (okToSwap) {
                Log.d(tag, "Ready to swap!")

                val landmarksForFaces1 = Landmarks.arrangeLandmarksForFaces(faces1)
                val landmarksForFaces2 = Landmarks.arrangeLandmarksForFaces(faces2)

                bitmap2Swapped =
                    Swap.faceSwapAll(bitmap1, bitmap2, landmarksForFaces1, landmarksForFaces2)
                bitmap1Swapped =
                    Swap.faceSwapAll(bitmap2, bitmap1, landmarksForFaces2, landmarksForFaces1)

                imageView1.setImageBitmap(bitmap1Swapped)

                imageView2.setImageBitmap(bitmap2Swapped)

                hasSwapped = true

//                imageUriFace1?.let { it1 -> drawLandmarks(it1, landmarksForFaces1) }
//                imageUriFace2?.let { it2 -> drawLandmarks(it2, landmarksForFaces2) }
            }
        }
    }

    // Gallery
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(tag, "onActivityResult: Image selected.")

        if (resultCode == RESULT_OK && requestCode == pickImage) {

            swapBt.isEnabled = false

            if (selectedTab == face1Tab) {
                imageUriFace1 = data?.data
                imageView1.setImageURI(imageUriFace1)
                imageUriFace1?.let { prepareImage(it, 0) }
            }
            if (selectedTab == face2Tab) {
                imageUriFace2 = data?.data
                imageView2.setImageURI(imageUriFace2)
                imageUriFace2?.let { prepareImage(it, 1) }
            }
        }
    }

    /**
     * Prepares chosen image for face detection.
     *
     * @param uri Location to image.
     */
    private fun prepareImage(uri: Uri, faceIndex: Int) {
        Log.d(tag, "prepareImage: Preparing image for face detection.")

        Glide.with(this)
            .asBitmap()
            .load(uri)
            .into(object : CustomTarget<Bitmap>(desiredWidth, desiredHeight) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {

                    val inputImage = InputImage.fromBitmap(resource, 0)
                    hasSwapped = false

                    when (faceIndex) {
                        0 -> bitmap1 = resource
                        else -> bitmap2 = resource
                    }

                    faceDetectorEngine.detectInImage(inputImage)
                        .addOnSuccessListener { faces ->
                            when (faceIndex) {
                                0 -> faces1 = faces
                                else -> faces2 = faces
                            }

                            val notEmpty = faces.isNotEmpty()
                            if (notEmpty && faceIndex == 0) {
                                face1Done = true
                            }
                            if (notEmpty && faceIndex == 1) {
                                face2Done = true
                            }

                            okToSwap = face1Done && face2Done
                            swapBt.isEnabled = okToSwap
                        }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }

    /**
     * Draws landmarks for a face. Only for debugging.
     *
     * @param uri Image source.
     * @param landmarksForFaces All landmarks extracted in source image.
     */
//    private fun drawLandmarks(uri: Uri, landmarksForFaces: ArrayList<ArrayList<PointF>>) {
//        Log.v(tag, "Draw landmarks for faces.")
//
//        Glide.with(this)
//            .asBitmap()
//            .load(uri)
//            .into(object : CustomTarget<Bitmap>(desiredWidth, desiredHeight) {
//                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                    val bitmapWithLandmarks =
//                        ImageUtils.drawLandmarksOnBitmap(resource, landmarksForFaces)
//
//                    imageView1.setImageBitmap(bitmapWithLandmarks)
//                }
//
//                override fun onLoadCleared(placeholder: Drawable?) {
//                }
//            })
//    }
    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        with(outState) { putParcelable(KEY_IMAGE_URI_1, imageUriFace1)
            putParcelable(KEY_IMAGE_URI_2, imageUriFace2)
        }
    }
    companion object {
        private const val KEY_IMAGE_URI_1 = "123"
        private const val KEY_IMAGE_URI_2 = "456"
    }


}
