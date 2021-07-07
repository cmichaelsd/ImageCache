package com.example.imagecaching.models

import java.io.Serializable

/**
 * V Shred
 *
 * @author Cole Michaels
 * @email cole.m@vshred.com
 * @date 2021-07-08
 *
 * Copyright © 2021 V Shred, LLC. All rights reserved.
 */
data class Supplement(
    var mId: Int = 0,
    var mImageUrl: String = ""
): Serializable {
    val imageUrl get() = mImageUrl

    var imageUri: String = ""
}
