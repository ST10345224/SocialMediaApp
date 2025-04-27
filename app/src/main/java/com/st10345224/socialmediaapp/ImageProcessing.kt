package com.st10345224.socialmediaapp

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import android.util.Base64

// Compresses Bitmap
fun compressBitmap(bitmap: Bitmap, quality: Int = 70): ByteArray {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(CompressFormat.JPEG, quality, outputStream) // Or PNG
    return outputStream.toByteArray()
}

// Encodes to Base64 string
fun encodeToBase64(byteArray: ByteArray): String {
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
}

// Decodes from Base64
fun decodeBase64(imageString: String): ByteArray {
    return Base64.decode(imageString, Base64.DEFAULT)
}

// Converts ByteArray to Bitmap
fun bytesToBitmap(imageBytes: ByteArray): Bitmap? {
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}