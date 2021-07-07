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
data class Program(
    var mId: Int = 0,
    var mImage: Image = Image()
): Serializable {
    val image get() = mImage
}
