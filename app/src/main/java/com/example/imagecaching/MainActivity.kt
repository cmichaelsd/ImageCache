package com.example.imagecaching

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val url = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/97/The_Earth_seen_from_Apollo_17.jpg/1920px-The_Earth_seen_from_Apollo_17.jpg"
        val path = ImageCache(this).saveOrRetrieve(url, "supplements")
        val imageView: ImageView = findViewById(R.id.imageView)
        Glide.with(this)
            .load(path)
            .into(imageView)
    }
}