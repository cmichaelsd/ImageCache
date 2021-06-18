package com.example.imagecaching

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class ImageCache(context: Context) {
    private var mContext = context
    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.Main)

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

    private suspend fun writeToDisk(imageUrl: String, directory: String) {
        val imageName = getFileName(imageUrl)
        // Lint errors will be thrown by the file system functions.
        // This is a known bug and doesn't represent a real issue.
        withContext(Dispatchers.IO) {
            val requestOptions = RequestOptions()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)

            // Use Glide to generate a workable bitmap
            val bitmap = Glide.with(mContext)
                .asBitmap()
                .load(imageUrl)
                .apply(requestOptions)
                .submit()
                .get()

            try {
                var file = mContext.getDir(directory, Context.MODE_PRIVATE)
                file = File(file, imageName)
                val out = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.flush()
                out.close()
                Log.i("ImageCache", "Image saved.")
            } catch (e: Exception) {
                Log.i("ImageCache", "Failed to save image.")
                scope.cancel()
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
            Log.i("doesFileExist", "Failed to check if files exists in internal storage.")
            false
        }
    }

    fun bulkInsert(imageUrls: ArrayList<String>, directory: String) {
        scope.launch { bulkClean(imageUrls, directory) }
        for (url in imageUrls)
            saveOrRetrieve(url, directory)
    }

    private suspend fun bulkClean(imageUrls: ArrayList<String>, directory: String) {
        withContext(Dispatchers.IO) {
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

}