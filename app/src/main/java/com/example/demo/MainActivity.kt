package com.example.demo

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val REQUEST_IMAGE_CAPTURE = 1
    private lateinit var imageClassifier: ImageClassifier
    private lateinit var imageView: ImageView
    private lateinit var button: Button
    private lateinit var score: TextView
    private lateinit var label: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        button = findViewById(R.id.button)
        score = findViewById(R.id.score)
        label = findViewById(R.id.label)

        button.setOnClickListener {
            dispatchTakePictureIntent()
        }

    }

    override fun onStart() {
        super.onStart()
        setupClassifier()
    }

    lateinit var currentPhotoPath: String

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }


    val REQUEST_TAKE_PHOTO = 1

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.android.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                }
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageUri = Uri.parse(currentPhotoPath)
            val file = File(imageUri.path)
            try {
                val ims = FileInputStream(file)
                val bitmap = BitmapFactory.decodeStream(ims)
                val matrix = Matrix().apply {
                    postRotate(90F)
                }
                val imageBitmap =
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                runOnUiThread {
                    imageView.setImageBitmap(imageBitmap)
                }
                val results = inference(imageBitmap)
                // Se accede al primer resultado para obtener su información
                val result = results[0].categories[0]

                runOnUiThread {
                    // El score se pasa a porcentaje
                    score.text = "%.2f".format(result.score * 100)
                    label.text = result.label
                }
                Log.d("Result", result.toString())
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setupClassifier() {
        // Se añaden las opciones del modelo
        // Este modelo se ejecuta con CPU por temas de compatibilidad
        // En modelos posteriores se debe de cambiar setNumThreads(4) por useNnapi()
        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(BaseOptions.builder().setNumThreads(4).build())
            .setMaxResults(1)
            .build()

        // El modelo se encuentra en la carpeta de assets
        val modelPath = "model.tflite"

        // Se carga el modelo con las opciones especificadas
        imageClassifier = ImageClassifier.createFromFileAndOptions(this, modelPath, options)
    }

    fun inference(bitmap: Bitmap): List<Classifications> {
        // Se recibe un bitmap desde la cámara
        val inputImage = TensorImage.fromBitmap(bitmap)
        val width: Int = bitmap.width
        val height: Int = bitmap.height

        // Preprocesamiento de la imagen
        // Se recorta la imagen por el centro con el tamaño deseado, esto puede cambiar en el futuro
        val cropWidth: Int = 800
        val cropHeigh: Int = 600

        // Se recorta la imagen en el centro con el tamaño deseado
        val imageOptions = ImageProcessingOptions.builder()
            .setRoi(
                Rect(
                    (width - cropWidth) / 2,
                    (height - cropHeigh) / 2,
                    (width + cropWidth) / 2,
                    (height + cropHeigh) / 2
                )
            )
            .build()

        // Se clasifica la imagen obteniendo los resultados
        val results: List<Classifications> = imageClassifier.classify(
            inputImage,
            imageOptions
        )

        return results
    }


}