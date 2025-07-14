package com.afif.emotistudy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.face.Face

class FaceOverlayView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var faces: List<Face> = emptyList()
    private var imageWidth = 0
    private var imageHeight = 0
    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    fun setFaces(faces: List<Face>, width: Int, height: Int) {
        this.faces = faces
        imageWidth = width
        imageHeight = height
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (face in faces) {
            val box = face.boundingBox
            val scaleX = width.toFloat() / imageWidth
            val scaleY = height.toFloat() / imageHeight
            val left = box.left * scaleX
            val top = box.top * scaleY
            val right = box.right * scaleX
            val bottom = box.bottom * scaleY
            canvas.drawRect(left, top, right, bottom, paint)
        }
    }
}
