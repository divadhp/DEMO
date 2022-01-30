# DEMO

En este repositorio se encuentra una demo para cargar un modelo de clasificación de imágenes usando TF Lite. En este ejemplo se obtienen imágenes desde la cámara mediante el uso de un Intent.

## Gradle
En [app/build.gradle](app/build.gradle) hay que indicar que los archivos tflite no sean comprimidos para poder cargar los modelos.

```gradle
android {
    // Other settings

    // Specify tflite file should not be compressed for the app apk
    aaptOptions {
        noCompress "tflite"
    }

}

dependencies {
    // Other dependencies

    // Import the Task Vision Library dependency (NNAPI is included)
    implementation 'org.tensorflow:tensorflow-lite-task-vision:0.3.0'
    // Import the GPU delegate plugin Library for GPU inference
    implementation 'org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.3.0'
}
```

## Kotlin

Primero se carga el modelo al iniciar la aplicación. El modelo se debe de encontrar en la ruta [app/src/main/assets](app/src/main/assets).

```kotlin
private lateinit var imageClassifier: ImageClassifier
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

    // Se carga el modelo con las opciones especificadas, this es el contexto de la aplicación
    imageClassifier = ImageClassifier.createFromFileAndOptions(this, modelPath, options)
}
```

Tras cargar el modelo se pueden clasificar imágenes en cualquier momento usando la siguiente función:

```kotlin
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
```

Esta función devuelve una lista de resultados, en este caso devuelve 1 resultado, pudiendo acceder a los valores de la etiqueta y probabilidad de este.
```kotlin
val results = inference(imageBitmap)
// Se accede al primer resultado, siendo en este caso el único
val result = results[0].categories[0]

// Etiqueta de la predicción
val label = result.label
// Score toma un valor entre 0 y 1
val score = result.score
```

