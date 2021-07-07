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
data class Image(
    var mId: Int = 0,
    var mUrl: String = ""
): Serializable {
    val url get() = mUrl

    var uri: String = ""
}
