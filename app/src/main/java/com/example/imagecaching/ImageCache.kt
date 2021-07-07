package com.example.imagecaching

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.imagecaching.models.Program
import com.example.imagecaching.models.Supplement
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.Serializable

class ImageCache(context: Context) {
    private var mContext = context

    // A supervisor job handles errors better for this hierarchy.
    // If a child process fails sibling and parent processes continue.
    private val job = SupervisorJob()

    // Default is for more CPU heavy actions like file read / writes
    private val scope = CoroutineScope(job + Dispatchers.Default)

    fun saveOrRetrieve(imageUrl: String, directory: String): String {
        val imageName = getFileName(imageUrl)
        return if (doesFileExist(imageName, directory)) {
            readFromDisk(imageName, directory).path
        } else {
            scope.launch { writeToDisk(imageUrl, directory) }
            imageUrl
        }
    }

    private fun readFromDisk(imageName: String, directory: String): File {
        var file = mContext.getDir(directory, Context.MODE_PRIVATE)
        file = File(file, imageName)
        return file
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun writeToDisk(imageUrl: String, directory: String) {
        withContext(Dispatchers.Default) {
            val imageName = getFileName(imageUrl)

            val requestOptions = RequestOptions()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)

            // Use Glide to generate a workable bitmap
            val bitmap = Glide.with(mContext)
                .asBitmap()
                .encodeFormat(Bitmap.CompressFormat.PNG)
                .load(imageUrl)
                .apply(requestOptions)
                .submit()
                .get()

            try {
                var file = mContext.getDir(directory, Context.MODE_PRIVATE)
                file = File(file, imageName)
                val out = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
                out.close()
                Log.i("ImageCache", "Image saved.")
            } catch (e: Exception) {
                Log.i("ImageCache", "Failed to save image.")
            }
        }
    }

    private fun getFileName(url: String): String {
        return url.split('/').last()
    }

    private fun doesFileExist(imageName: String, directory: String): Boolean {
        return try {
            var file = mContext.getDir(directory, Context.MODE_PRIVATE)
            file = File(file, imageName)
            file.exists()
        } catch (e: Exception) {
            Log.i("ImageCache", "Failed to check if files exists in internal storage.")
            false
        }
    }

    private fun bulkInsertPrograms(programs: List<Program>) {
        val imageUrls = programs.map { it.image.url }
        scope.launch { bulkClean(imageUrls, MEMBER_PORTALS_DIRECTORY) }
        for (program in programs)
            program.image.uri = saveOrRetrieve(program.image.url, MEMBER_PORTALS_DIRECTORY)
    }

    private fun bulkInsertSupplements(supplements: List<Supplement>) {
        val imageUrls = supplements.map { it.imageUrl }
        scope.launch { bulkClean(imageUrls, SUPPLEMENTS_DIRECTORY) }
        for (supplement in supplements)
            supplement.imageUri = saveOrRetrieve(supplement.imageUrl, SUPPLEMENTS_DIRECTORY)
    }

    fun <T: Serializable> bulkInsert(collection: List<T>) {
        if (collection.isEmpty()) return

        collection.checkType<Program>()?.let {
            bulkInsertPrograms(it)
        }

        collection.checkType<Supplement>()?.let {
            bulkInsertSupplements(it)
        }
    }

    private suspend fun bulkClean(imageUrls: List<String>, directory: String) {
        withContext(Dispatchers.Default) {
            val cachedFiles = mContext.getDir(directory, Context.MODE_PRIVATE)
                ?.listFiles()
                ?.map { it.name }

            val fileNames = imageUrls.map { getFileName(it) }

            val toRemove = cachedFiles?.filter { !fileNames.contains(it) }

            if (toRemove != null) {
                for (fileName in toRemove)
                    readFromDisk(fileName, directory).delete()
            }
        }
    }

    /*
     * Reified prefix to this type allows inline functions to have type information
     * available at runtime which allows for the type checking.
     */
    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Any> List<*>.checkType() =
        if (all { it is T }) this as List<T> else null

    companion object {
        const val SUPPLEMENTS_DIRECTORY = "supplements"
        const val MEMBER_PORTALS_DIRECTORY = "member_portal"
    }
}