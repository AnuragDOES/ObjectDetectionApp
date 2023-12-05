package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent


import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions



import com.google.mlkit.vision.label.defaults.ImageLabelerOptions


class MainActivity : AppCompatActivity() {


    private lateinit var tvLabel: ImageLabel
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()

    }

    private fun requestPermissions() {
        requestCameraPermissionIfMissing{granted->
            if(granted)
                startScanner()
            else {
                Toast.makeText(this, "please allow permissions", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    private fun requestCameraPermissionIfMissing(onResult: ((Boolean)->Unit)) {
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)
            onResult(true)
        else
            registerForActivityResult(ActivityResultContracts.RequestPermission()){
                onResult(it)
            }.launch(android.Manifest.permission.CAMERA)
    }


    private fun startScanner() {

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindScannerPreview(cameraProvider = cameraProvider)
        },ContextCompat.getMainExecutor(this))



    }

    private fun bindScannerPreview(cameraProvider: ProcessCameraProvider){


        //preview use case
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        val pvScan = findViewById<PreviewView>(R.id.pvScan)
        preview.setSurfaceProvider(pvScan.surfaceProvider)

        //imageAnalysis use case
        val imageAnalysis = buildImageAnalysisUseCase()

        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)

    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun buildImageAnalysisUseCase(): ImageAnalysis{

        val localModel = LocalModel.Builder().setAssetFilePath("objectModel.tflite").build()

        val customImageLabelerOptions = CustomImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(0.5f)
//            .setMaxResultCount(5)
            .build()

        val labeler = ImageLabeling.getClient(customImageLabelerOptions)

        val tvLabel = findViewById<TextView>(R.id.tvLabel)
        val searchButton = findViewById<Button>(R.id.searchButton)
        val addButton = findViewById<ImageButton>(R.id.addButton)
        return ImageAnalysis.Builder()
            .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
            .build().also{analysis->
                analysis.setAnalyzer(ContextCompat.getMainExecutor(this)){image ->
                    val inputImage = InputImage.fromMediaImage(image.image!!,image.imageInfo.rotationDegrees)
                    labeler.process(inputImage)
                        .addOnSuccessListener { labels ->
                            val sortedLabels = labels.sortedByDescending { it.confidence }
                            if (sortedLabels.isNotEmpty()){
                                val highestConfidenceLabel = sortedLabels[0].text
                                tvLabel.text = highestConfidenceLabel
                            }
                            searchButton.setOnClickListener {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${tvLabel.text}"))
                                startActivity(intent)
                            }
                            addButton.setOnClickListener {
                                val label = tvLabel.text.toString()
                                val intent = Intent(this, EntryActivity::class.java)
                                intent.putExtra("label", label)
                                startActivity(intent)
                            }



                        }
                        .addOnFailureListener { }
                        .addOnCompleteListener {
                            // Close the imageProxy
                            image.close()
                        }
                }
            }
    }
}