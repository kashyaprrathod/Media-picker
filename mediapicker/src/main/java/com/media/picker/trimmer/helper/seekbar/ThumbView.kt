package com.media.picker.trimmer.helper.seekbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View

@SuppressLint("ViewConstructor")
internal class ThumbView(context: Context, private var mThumbWidth: Int, private var mThumbDrawable: Drawable) : View(context) {
    private val mExtendTouchSlop: Int

    private var mPressed = false

    var rangeIndex: Int = 0
        private set

    init {
        mExtendTouchSlop = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            EXTEND_TOUCH_SLOP.toFloat(), context.resources.displayMetrics
        ).toInt()
        setBackgroundDrawable(mThumbDrawable)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(mThumbWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.EXACTLY)
        )

        mThumbDrawable.setBounds(0, 0, mThumbWidth, measuredHeight)
    }

    fun setThumbWidth(thumbWidth: Int) {
        mThumbWidth = thumbWidth
    }

    fun setThumbDrawable(thumbDrawable: Drawable) {
        mThumbDrawable = thumbDrawable
    }

    fun inInTarget(x: Int, y: Int): Boolean {
        val rect = Rect()
        getHitRect(rect)
        rect.left -= mExtendTouchSlop
        rect.right += mExtendTouchSlop
        rect.top -= mExtendTouchSlop
        rect.bottom += mExtendTouchSlop
        return rect.contains(x, y)
    }

    fun setTickIndex(tickIndex: Int) {
        rangeIndex = tickIndex
    }

    override fun isPressed(): Boolean {
        return mPressed
    }

    override fun setPressed(pressed: Boolean) {
        mPressed = pressed
    }

    companion object {
        private const val EXTEND_TOUCH_SLOP = 15
    }
}