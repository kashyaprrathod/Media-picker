package com.media.picker.cropper.helper.cropview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import androidx.core.content.ContextCompat
import com.media.picker.R
import com.media.picker.cropper.helper.util.extensions.animateScaleToPoint
import com.media.picker.cropper.helper.util.extensions.animateTo
import com.media.picker.cropper.helper.util.extensions.animateToMatrix
import com.media.picker.cropper.helper.util.extensions.clone
import com.media.picker.cropper.helper.util.extensions.getCornerTouch
import com.media.picker.cropper.helper.util.extensions.getEdgeTouch
import com.media.picker.cropper.helper.util.extensions.getHypotenus
import com.media.picker.cropper.helper.util.model.AnimatableRectF
import com.media.picker.cropper.helper.util.model.AspectRatio
import com.media.picker.cropper.helper.util.model.Corner
import com.media.picker.cropper.helper.util.model.DraggingState
import com.media.picker.cropper.helper.util.model.Edge
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import com.media.picker.cropper.helper.util.model.DraggingState.*


class CropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val cropRectOnOriginalBitmapMatrix = Matrix()

    /**
     * Touch threshold for corners and edges
     */
    private val touchThreshold = 20f

    /**
     * Main rect which is drawn to canvas.
     */
    private val cropRect: AnimatableRectF = AnimatableRectF()

    /**
     * Temporary rect to animate crop rect to.
     * This value will be set to zero after using.
     */
    private val targetRect: AnimatableRectF = AnimatableRectF()

    /**
     * Minimum scale limitation is dependens on screen
     * and bitmap size. bitmapMinRect is calculated
     * initially. This value holds the miminum rectangle
     * which bitmapMatrix can be.
     */
    private val bitmapMinRect = RectF()

    /**
     * Minimum rectangle for cropRect can be.
     * This value will be only calculated on ACTION_DOWN.
     * Then will be check the crop rect value ACTION_MOVE and
     * override cropRect if it exceed its limit.
     */
    private val minRect = RectF()

    /**
     * Maximum rectangle for cropRect can be.
     * This value will be only calculated on ACTION_DOWN.
     * Then will be check the crop rect value ACTION_MOVE and
     * override cropRect if it exceed its limit.
     */
    private val maxRect = RectF()

    /**
     * Bitmap rect value. Holds original bitmap width
     * and height rectangle.
     */
    private val bitmapRect = RectF()

    /**
     * CropView rectangle. Holds view borders.
     */
    private val viewRect = RectF()

    /**
     * This value is hold view width minus margin between screen sides.
     * So it will be measuredWidth - dimen(R.dimen.default_crop_margin)
     */
    private var viewWidth = 0f

    /**
     * This value is hold view height minus margin between screen sides.
     * So it will be measuredWidth - dimen(R.dimen.default_crop_margin)
     */
    private var viewHeight = 0f

    /**
     * Original bitmap value
     */
    private var bitmap: Bitmap? = null

    /**
     * Bitmap matrix to draw bitmap on canvas
     */
    private val bitmapMatrix: Matrix = Matrix()

    /**
     * Empty paint to draw something on canvas.
     */
    private val emptyPaint = Paint().apply {
        isAntiAlias = true
    }

    /**
     * Default margin for cropRect.
     */
    private val marginInPixelSize = 60.toFloat()

    /**
     * Aspect ratio matters for calculation.
     * It can be ASPECT_FREE or ASPECT_X_X. Default
     * value is ASPECT_FREE
     */
    private var selectedAspectRatio = AspectRatio.ASPECT_INS_1_1

    /**
     * Aspect mode (FREE or ASPECT)
     */
    private var aspectAspectMode: AspectMode = AspectMode.ASPECT

    /**
     * User can drag crop rect from Corner, Edge or Bitmap
     */
    private var draggingState: DraggingState = DraggingState.Idle

    /**
     * Hold value for scaling bitmap with two finger.
     * We initialize this point to avoid memory
     * allocation every time user scale bitmap with fingers.
     */
    private val zoomFocusPoint = FloatArray(2)

    /**
     * This value holds inverted matrix when user scale
     * bitmap image with two finger. This value initialized to
     * avoid memory allocation every time user pinch zoom.
     */
    private val zoomInverseMatrix = Matrix()

    /**
     * Crop rect grid line width
     */
    private val gridLineWidthInPixel = 3f

    /**
     * Crop rect draw paint
     */
    private val cropPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = gridLineWidthInPixel
        style = Paint.Style.STROKE
    }

    /**
     * Corner toggle line width
     */
    private val cornerToggleWidthInPixel = 10f

    /**
     * Corner toggle line length
     */
    private val cornerToggleLengthInPixel = 40f

    private val minRectLength = 60f

    /**
     * Corner toggle paint
     */
    private val cornerTogglePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.black)
        strokeWidth = cornerToggleWidthInPixel
        style = Paint.Style.STROKE
    }

    /**
     * Mask color
     */
    private val maskBackgroundColor = ContextCompat.getColor(context, R.color.black_50)

    /**
     * Mask canvas
     */
    private var maskCanvas: Canvas? = null

    /**
     * Mask bitmap
     */
    private var maskBitmap: Bitmap? = null

    private val bitmapGestureListener = object : BitmapGestureHandler.BitmapGestureListener {
        override fun onDoubleTap(motionEvent: MotionEvent) {

            if (isBitmapScaleExceedMaxLimit(DOUBLE_TAP_SCALE_FACTOR)) {

                val resetMatrix = Matrix()
                val scale = max(
                    cropRect.width() / bitmapRect.width(),
                    cropRect.height() / bitmapRect.height()
                )
                resetMatrix.setScale(scale, scale)

                val translateX = (viewWidth - bitmapRect.width() * scale) / 2f + marginInPixelSize
                val translateY = (viewHeight - bitmapRect.height() * scale) / 2f + marginInPixelSize
                resetMatrix.postTranslate(translateX, translateY)

                bitmapMatrix.animateToMatrix(resetMatrix) {
                    invalidate()
                }

                return
            }

            bitmapMatrix.animateScaleToPoint(
                DOUBLE_TAP_SCALE_FACTOR,
                motionEvent.x,
                motionEvent.y
            ) {
                invalidate()
            }
        }

        override fun onScale(scaleFactor: Float, focusX: Float, focusY: Float) {

            /**
             * Return if new calculated bitmap matrix will exceed scale
             * point then early return.
             * Otherwise continue and do calculation and apply to bitmap matrix.
             */
            if (isBitmapScaleExceedMaxLimit(scaleFactor)) {
                return
            }

            zoomInverseMatrix.reset()
            bitmapMatrix.invert(zoomInverseMatrix)

            /**
             * Inverse focus points
             */
            zoomFocusPoint[0] = focusX
            zoomFocusPoint[1] = focusY
            zoomInverseMatrix.mapPoints(zoomFocusPoint)

            /**
             * Scale bitmap matrix
             */
            bitmapMatrix.preScale(
                scaleFactor,
                scaleFactor,
                zoomFocusPoint[0],
                zoomFocusPoint[1]
            )

            invalidate()
        }

        override fun onScroll(distanceX: Float, distanceY: Float) {
            bitmapMatrix.postTranslate(-distanceX, -distanceY)
            invalidate()
        }

        override fun onEnd() {
            settleDraggedBitmap()
        }
    }

    private val bitmapGestureHandler = BitmapGestureHandler(context, bitmapGestureListener)

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setBackgroundColor(ContextCompat.getColor(context, R.color.white))
    }

    /**
     * Initialize necessary rects, bitmaps, canvas here.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initialize()
    }

    /**
     * Handles touches
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }

        when (event.action) {
            ACTION_DOWN -> {
                val corner = cropRect.getCornerTouch(event, touchThreshold)
                val edge = cropRect.getEdgeTouch(event, touchThreshold)

                draggingState = when {
                    isCorner(corner) -> DraggingState.DraggingCorner(corner)
                    isEdge(edge) -> DraggingState.DraggingEdge(edge)
                    else -> DraggingState.DraggingBitmap
                }

                calculateMinRect()
                calculateMaxRect()
            }

            ACTION_MOVE -> {
                when (val state = draggingState) {
                    is DraggingState.DraggingCorner -> {
                        onCornerPositionChanged(state.corner, event)
                        updateExceedMaxBorders()
                        updateExceedMinBorders()
                    }

                    is DraggingState.DraggingEdge -> {
                        onEdgePositionChanged(state.edge, event)
                        updateExceedMaxBorders()
                        updateExceedMinBorders()
                    }

                    else -> {

                    }
                }
            }

            ACTION_UP -> {
                minRect.setEmpty()
                maxRect.setEmpty()
                when (draggingState) {
                    is DraggingEdge, is DraggingCorner -> {
                        calculateCenterTarget()
                        animateBitmapToCenterTarget()
                        animateCropRectToCenterTarget()
                    }

                    else -> {

                    }
                }
            }
        }

        if (draggingState == DraggingState.DraggingBitmap) {
            bitmapGestureHandler.onTouchEvent(event)
        }

        invalidate()
        return true
    }

    /**
     * Draw bitmap, cropRect, overlay
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        bitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, bitmapMatrix, emptyPaint)
        }

        val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), cropPaint)

        canvas.drawColor(maskBackgroundColor)

        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
//        canvas.clipRect(cropRect, Region.Op.DIFFERENCE)
//        canvas.drawColor(maskBackgroundColor)

        canvas.drawRoundRect(cropRect, 70f, 70f, maskPaint)
        canvas.restoreToCount(layerId)

        drawGrid(canvas)

//        drawCornerToggles(canvas)
    }

    /**
     * Set bitmap from outside of this view.
     * Calculates bitmap rect and bitmap min rect.
     */
    fun setBitmap(bitmap: Bitmap?) {
        this.bitmap = bitmap

        bitmapRect.set(
            0f,
            0f,
            this.bitmap?.width?.toFloat() ?: 0f,
            this.bitmap?.height?.toFloat() ?: 0f
        )

        val bitmapMinRectSize = max(bitmapRect.width(), bitmapRect.height()) / MAX_SCALE
        bitmapMinRect.set(0f, 0f, bitmapMinRectSize, bitmapMinRectSize)

        initialize()

        requestLayout()
        invalidate()
    }

    /*fun setTheme(croppyTheme: CroppyTheme) {
        cornerTogglePaint.color = ContextCompat.getColor(context, croppyTheme.accentColor)
        invalidate()
    }*/

    /**
     * Get cropped bitmap.
     */
    fun getCroppedData(): Bitmap? {
        val croppedBitmapRect = getCropSizeOriginal()

        if (bitmapRect.intersect(croppedBitmapRect).not()) {
            return bitmap
        }

        val cropLeft = if (croppedBitmapRect.left.roundToInt() < bitmapRect.left) {
            bitmapRect.left.toInt()
        } else {
            croppedBitmapRect.left.roundToInt()
        }

        val cropTop = if (croppedBitmapRect.top.roundToInt() < bitmapRect.top) {
            bitmapRect.top.toInt()
        } else {
            croppedBitmapRect.top.roundToInt()
        }

        val cropRight = if (croppedBitmapRect.right.roundToInt() > bitmapRect.right) {
            bitmapRect.right.toInt()
        } else {
            croppedBitmapRect.right.roundToInt()
        }

        val cropBottom = if (croppedBitmapRect.bottom.roundToInt() > bitmapRect.bottom) {
            bitmapRect.bottom.toInt()
        } else {
            croppedBitmapRect.bottom.roundToInt()
        }

        bitmap?.let {
            val croppedBitmap = Bitmap.createBitmap(
                it, cropLeft, cropTop, cropRight - cropLeft, cropBottom - cropTop
            )
            return croppedBitmap
        }

        throw IllegalStateException("Bitmap is null.")
    }

    /**
     * Changes aspect ratio and aspect mode values and
     * call aspectRatioChanged() method to do calculations
     * and animations from current editState.
     */
    fun setAspectRatio(aspectRatio: AspectRatio) {
        this.selectedAspectRatio = aspectRatio

        aspectAspectMode = when (aspectRatio) {
            else -> AspectMode.ASPECT
        }

        aspectRatioChanged()
        invalidate()
    }

    /**
     * Current crop size depending on original bitmap.
     * Returns rectangle as pixel values.
     */
    fun getCropSizeOriginal(): RectF {
        val cropSizeOriginal = RectF()
        cropRectOnOriginalBitmapMatrix.reset()
        bitmapMatrix.invert(cropRectOnOriginalBitmapMatrix)
        cropRectOnOriginalBitmapMatrix.mapRect(cropSizeOriginal, cropRect)
        return cropSizeOriginal
    }

    /**
     * Initialize
     */
    private fun initialize() {
        Log.e("TAG", "initialize: $measuredWidth ::: $measuredHeight")
        viewWidth = measuredWidth.toFloat() - (marginInPixelSize * 2)

        viewHeight = measuredHeight.toFloat() - (marginInPixelSize * 2)

        viewRect.set(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())

        createMaskBitmap()

        initializeBitmapMatrix()

        initializeCropRect()

        invalidate()
    }

    /**
     * Create mask bitmap
     */
    private fun createMaskBitmap() {
        maskBitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
        maskCanvas = Canvas(maskBitmap!!)
    }

    /**
     * Draw crop rect as a grid.
     */
    private fun drawGrid(canvas: Canvas?) {
        cropPaint.strokeWidth = 1f
        cropPaint.color = Color.WHITE
        canvas?.drawLine(
            cropRect.left + cropRect.width() / 3f,
            cropRect.top,
            cropRect.left + cropRect.width() / 3f,
            cropRect.bottom,
            cropPaint
        )

        canvas?.drawLine(
            cropRect.left + cropRect.width() * 2f / 3f,
            cropRect.top,
            cropRect.left + cropRect.width() * 2f / 3f,
            cropRect.bottom,
            cropPaint
        )

        canvas?.drawLine(
            cropRect.left,
            cropRect.top + cropRect.height() / 3f,
            cropRect.right,
            cropRect.top + cropRect.height() / 3f,
            cropPaint
        )

        canvas?.drawLine(
            cropRect.left,
            cropRect.top + cropRect.height() * 2f / 3f,
            cropRect.right,
            cropRect.top + cropRect.height() * 2f / 3f,
            cropPaint
        )

        cropPaint.color = Color.BLACK
        cropPaint.strokeWidth = 5f

        canvas?.drawRoundRect(cropRect, 70f, 70f, cropPaint)
    }

    /**
     * Draw corner lines and toggles
     */
    private fun drawCornerToggles(canvas: Canvas?) {
        /**
         * Top left toggle
         */
        canvas?.drawLine(
            cropRect.left - gridLineWidthInPixel,
            cropRect.top + cornerToggleWidthInPixel / 2f - gridLineWidthInPixel,
            cropRect.left + cornerToggleLengthInPixel,
            cropRect.top + cornerToggleWidthInPixel / 2f - gridLineWidthInPixel,
            cornerTogglePaint
        )

        canvas?.drawLine(
            cropRect.left + cornerToggleWidthInPixel / 2f - gridLineWidthInPixel,
            cropRect.top - gridLineWidthInPixel,
            cropRect.left + cornerToggleWidthInPixel / 2f - gridLineWidthInPixel,
            cropRect.top + cornerToggleLengthInPixel,
            cornerTogglePaint
        )

        /**
         * Top Right toggle
         */

        canvas?.drawLine(
            cropRect.right - cornerToggleLengthInPixel,
            cropRect.top + cornerToggleWidthInPixel / 2f - gridLineWidthInPixel,
            cropRect.right + gridLineWidthInPixel,
            cropRect.top + cornerToggleWidthInPixel / 2f - gridLineWidthInPixel,
            cornerTogglePaint
        )

        canvas?.drawLine(
            cropRect.right - cornerToggleWidthInPixel / 2f + gridLineWidthInPixel,
            cropRect.top - gridLineWidthInPixel,
            cropRect.right - cornerToggleWidthInPixel / 2f + gridLineWidthInPixel,
            cropRect.top + cornerToggleLengthInPixel,
            cornerTogglePaint
        )

        /**
         * Bottom Left toggle
         */

        canvas?.drawLine(
            cropRect.left - gridLineWidthInPixel,
            cropRect.bottom - cornerToggleWidthInPixel / 2f + gridLineWidthInPixel,
            cropRect.left + cornerToggleLengthInPixel,
            cropRect.bottom - cornerToggleWidthInPixel / 2f + gridLineWidthInPixel,
            cornerTogglePaint
        )

        canvas?.drawLine(
            cropRect.left + cornerToggleWidthInPixel / 2f - gridLineWidthInPixel,
            cropRect.bottom + gridLineWidthInPixel,
            cropRect.left + cornerToggleWidthInPixel / 2f - gridLineWidthInPixel,
            cropRect.bottom - cornerToggleLengthInPixel,
            cornerTogglePaint
        )

        /**
         * Bottom Right toggle
         */
        canvas?.drawLine(
            cropRect.right - cornerToggleLengthInPixel,
            cropRect.bottom - cornerToggleWidthInPixel / 2f + gridLineWidthInPixel,
            cropRect.right + gridLineWidthInPixel,
            cropRect.bottom - cornerToggleWidthInPixel / 2f + gridLineWidthInPixel,
            cornerTogglePaint
        )

        canvas?.drawLine(
            cropRect.right - cornerToggleWidthInPixel / 2f + gridLineWidthInPixel,
            cropRect.bottom + gridLineWidthInPixel,
            cropRect.right - cornerToggleWidthInPixel / 2f + gridLineWidthInPixel,
            cropRect.bottom - cornerToggleLengthInPixel,
            cornerTogglePaint
        )
    }

    /**
     * If selected aspect ratio is ASPECT_FREE
     * then bitmap matrix and cropRect should be same.
     * Otherwise calculate scale value for bitmap matrix,
     * and animate bitmap matrix to center crop rect.
     *
     * And also in this method. cropRect will be animate to calculated
     * target rect.
     */
    private fun aspectRatioChanged() {

        val widthRatio: Float = selectedAspectRatio.widthRatio
        val heightRatio: Float = selectedAspectRatio.heightRatio

        val aspectRatio = widthRatio / heightRatio
        val overlayViewRatio = viewWidth / viewHeight

        val cropWidth: Float
        val cropHeight: Float

        when {
            aspectRatio > overlayViewRatio -> {
                cropWidth = viewWidth
                cropHeight = heightRatio * viewWidth / widthRatio
            }

            else -> {
                cropHeight = viewHeight
                cropWidth = widthRatio * viewHeight / heightRatio
            }
        }

        val distanceToCenterX = viewRect.centerX() - cropWidth / 2f
        val distanceToCenterY = viewRect.centerY() - cropHeight / 2f

        targetRect.set(
            0f + distanceToCenterX,
            0f + distanceToCenterY,
            cropWidth + distanceToCenterX,
            cropHeight + distanceToCenterY
        )

        val resetMatrix = Matrix()
        val scale = max(
            targetRect.width() / bitmapRect.width(),
            targetRect.height() / bitmapRect.height()
        )
        resetMatrix.setScale(scale, scale)

        val translateX = (viewWidth - bitmapRect.width() * scale) / 2f + marginInPixelSize
        val translateY = (viewHeight - bitmapRect.height() * scale) / 2f + marginInPixelSize
        resetMatrix.postTranslate(translateX, translateY)

        bitmapMatrix.animateToMatrix(resetMatrix) {
            invalidate()
        }

        cropRect.animateTo(targetRect) {
            invalidate()
        }

        targetRect.setEmpty()
    }

    /**
     * Initializes bitmap matrix
     */
    private fun initializeBitmapMatrix() {
        val scale = min(viewWidth / bitmapRect.width(), viewHeight / bitmapRect.height())
        bitmapMatrix.setScale(scale, scale)

        val translateX = (viewWidth - bitmapRect.width() * scale) / 2f + marginInPixelSize
        val translateY = (viewHeight - bitmapRect.height() * scale) / 2f + marginInPixelSize
        bitmapMatrix.postTranslate(translateX, translateY)
    }

    /**
     * Initializes crop rect with bitmap.
     */
    private fun initializeCropRect() {
        val rect = RectF(0f, 0f, bitmapRect.width(), bitmapRect.height())
        bitmapMatrix.mapRect(cropRect, rect)
    }

    /**
     * Check if corner is touched
     * @return true if corner, false otherwise
     */
    private fun isCorner(corner: Corner) = corner != Corner.NONE

    /**
     * Check if edge is touched
     * @return true if edge, false otherwise
     */
    private fun isEdge(edge: Edge) = edge != Edge.NONE

    /**
     * Move cropRect on user drag cropRect from corners.
     * Corner will be move to opposite side of the selected cropRect's
     * corner. If aspect ratio selected (Not free), then aspect ration shouldn't
     * be change on cropRect is changed.
     */
    private fun onCornerPositionChanged(corner: Corner, motionEvent: MotionEvent) {
        when (aspectAspectMode) {
            AspectMode.FREE -> {
                when (corner) {
                    Corner.TOP_RIGHT -> {
                        cropRect.setTop(motionEvent.y)
                        cropRect.setRight(motionEvent.x)
                    }

                    Corner.TOP_LEFT -> {
                        cropRect.setTop(motionEvent.y)
                        cropRect.setLeft(motionEvent.x)
                    }

                    Corner.BOTTOM_RIGHT -> {
                        cropRect.setBottom(motionEvent.y)
                        cropRect.setRight(motionEvent.x)
                    }

                    Corner.BOTTOM_LEFT -> {
                        cropRect.setBottom(motionEvent.y)
                        cropRect.setLeft(motionEvent.x)
                    }

                    else -> return
                }
            }

            AspectMode.ASPECT -> {
                when (corner) {
                    Corner.TOP_RIGHT -> {

                        if (motionEvent.y > minRect.top && motionEvent.x < minRect.right) {
                            return
                        }

                        val motionHypo = hypot(
                            (motionEvent.y - cropRect.bottom).toDouble(),
                            (motionEvent.x - cropRect.left).toDouble()
                        ).toFloat()

                        val differenceWidth = (cropRect.getHypotenus() - motionHypo) / 2
                        val differenceHeight =
                            selectedAspectRatio.heightRatio * differenceWidth / selectedAspectRatio.widthRatio

                        cropRect.setTop(cropRect.top + differenceHeight)
                        cropRect.setRight(cropRect.right - differenceWidth)
                    }

                    Corner.TOP_LEFT -> {

                        if (motionEvent.y > minRect.top && motionEvent.x > minRect.left) {
                            return
                        }

                        val motionHypo = hypot(
                            (cropRect.bottom - motionEvent.y).toDouble(),
                            (cropRect.right - motionEvent.x).toDouble()
                        ).toFloat()

                        val differenceWidth = (cropRect.getHypotenus() - motionHypo) / 2
                        val differenceHeight =
                            selectedAspectRatio.heightRatio * differenceWidth / selectedAspectRatio.widthRatio

                        cropRect.setTop(cropRect.top + differenceHeight)
                        cropRect.setLeft(cropRect.left + differenceWidth)
                    }

                    Corner.BOTTOM_RIGHT -> {
                        if (motionEvent.y < minRect.bottom && motionEvent.x < minRect.right) {
                            return
                        }

                        val motionHypo = hypot(
                            (cropRect.top - motionEvent.y).toDouble(),
                            (cropRect.left - motionEvent.x).toDouble()
                        ).toFloat()

                        val differenceWidth = (cropRect.getHypotenus() - motionHypo) / 2
                        val differenceHeight =
                            selectedAspectRatio.heightRatio * differenceWidth / selectedAspectRatio.widthRatio

                        cropRect.setBottom(cropRect.bottom - differenceHeight)
                        cropRect.setRight(cropRect.right - differenceWidth)
                    }

                    Corner.BOTTOM_LEFT -> {

                        if (motionEvent.y < minRect.bottom && motionEvent.x > minRect.left) {
                            return
                        }

                        val motionHypo = hypot(
                            (cropRect.top - motionEvent.y).toDouble(),
                            (cropRect.right - motionEvent.x).toDouble()
                        ).toFloat()

                        val differenceWidth = (cropRect.getHypotenus() - motionHypo) / 2
                        val differenceHeight =
                            selectedAspectRatio.heightRatio * differenceWidth / selectedAspectRatio.widthRatio

                        cropRect.setBottom(cropRect.bottom - differenceHeight)
                        cropRect.setLeft(cropRect.left + differenceWidth)
                    }

                    else -> return
                }
            }
        }
    }

    /**
     * Move cropRect on user drag cropRect from edges.
     * Corner will be move to opposite side of the selected cropRect's
     * edge. If aspect ratio selected (Not free), then aspect ration shouldn't
     * be change on cropRect is changed.
     */
    private fun onEdgePositionChanged(edge: Edge, motionEvent: MotionEvent) {
        val bitmapBorderRect = RectF()
        bitmapMatrix.mapRect(bitmapBorderRect, bitmapRect)

        when (aspectAspectMode) {
            AspectMode.FREE -> {
                when (edge) {
                    Edge.LEFT -> cropRect.setLeft(motionEvent.x)
                    Edge.TOP -> cropRect.setTop(motionEvent.y)
                    Edge.RIGHT -> cropRect.setRight(motionEvent.x)
                    Edge.BOTTOM -> cropRect.setBottom(motionEvent.y)
                    else -> return
                }
            }

            AspectMode.ASPECT -> {
                when (edge) {
                    Edge.LEFT -> {
                        val differenceWidth = motionEvent.x - cropRect.left
                        val differenceHeight =
                            selectedAspectRatio.heightRatio * differenceWidth / selectedAspectRatio.widthRatio
                        cropRect.setLeft(cropRect.left + differenceWidth)
                        cropRect.setTop(cropRect.top + differenceHeight / 2f)
                        cropRect.setBottom(cropRect.bottom - differenceHeight / 2f)
                    }

                    Edge.TOP -> {
                        val differenceHeight = motionEvent.y - cropRect.top
                        val differenceWidth =
                            selectedAspectRatio.widthRatio * differenceHeight / selectedAspectRatio.heightRatio
                        cropRect.setTop(cropRect.top + differenceHeight)
                        cropRect.setLeft(cropRect.left + differenceWidth / 2f)
                        cropRect.setRight(cropRect.right - differenceWidth / 2f)
                    }

                    Edge.RIGHT -> {
                        val differenceWidth = cropRect.right - motionEvent.x
                        val differenceHeight =
                            selectedAspectRatio.heightRatio * differenceWidth / selectedAspectRatio.widthRatio
                        cropRect.setRight(cropRect.right - differenceWidth)
                        cropRect.setTop(cropRect.top + differenceHeight / 2f)
                        cropRect.setBottom(cropRect.bottom - differenceHeight / 2f)
                    }

                    Edge.BOTTOM -> {
                        val differenceHeight = cropRect.bottom - motionEvent.y
                        val differenceWidth =
                            selectedAspectRatio.widthRatio * differenceHeight / selectedAspectRatio.heightRatio
                        cropRect.setBottom(cropRect.bottom - differenceHeight)
                        cropRect.setLeft(cropRect.left + differenceWidth / 2f)
                        cropRect.setRight(cropRect.right - differenceWidth / 2f)
                    }

                    else -> return
                }
            }
        }
    }

    /**
     * Calculates minimum possibel rectangle that user can drag
     * cropRect
     */
    private fun calculateMinRect() {
        val mappedBitmapMinRectSize = RectF()
            .apply { bitmapMatrix.mapRect(this, bitmapMinRect) }
            .width()

        val minSize = max(mappedBitmapMinRectSize, minRectLength)

        when (aspectAspectMode) {
            AspectMode.FREE -> {
                when (val state = draggingState) {
                    is DraggingEdge -> {
                        when (state.edge) {
                            Edge.LEFT -> minRect.set(
                                cropRect.right - minSize,
                                cropRect.top,
                                cropRect.right,
                                cropRect.bottom
                            )

                            Edge.TOP -> minRect.set(
                                cropRect.left,
                                cropRect.bottom - minSize,
                                cropRect.right,
                                cropRect.bottom
                            )

                            Edge.RIGHT -> minRect.set(
                                cropRect.left,
                                cropRect.top,
                                cropRect.left + minSize,
                                cropRect.bottom
                            )

                            Edge.BOTTOM -> minRect.set(
                                cropRect.left,
                                cropRect.top,
                                cropRect.right,
                                cropRect.top + minSize
                            )

                            else -> {

                            }
                        }
                    }

                    is DraggingCorner -> {
                        when (state.corner) {
                            Corner.TOP_RIGHT -> minRect.set(
                                cropRect.left,
                                cropRect.bottom - minSize,
                                cropRect.left + minSize,
                                cropRect.bottom
                            )

                            Corner.TOP_LEFT -> minRect.set(
                                cropRect.right - minSize,
                                cropRect.bottom - minSize,
                                cropRect.right,
                                cropRect.bottom
                            )

                            Corner.BOTTOM_RIGHT -> minRect.set(
                                cropRect.left,
                                cropRect.top,
                                cropRect.left + minSize,
                                cropRect.top + minSize
                            )

                            Corner.BOTTOM_LEFT -> minRect.set(
                                cropRect.right - minSize,
                                cropRect.top,
                                cropRect.right,
                                cropRect.top + minSize
                            )

                            else -> {

                            }
                        }
                    }

                    else -> {

                    }
                }
            }

            AspectMode.ASPECT -> {
                val scaleWidth = minSize / cropRect.width()
                val scaleHeight = minSize / cropRect.height()
                val scale = max(scaleWidth, scaleHeight)

                when (val state = draggingState) {
                    is DraggingEdge -> {
                        val matrix = Matrix()
                        when (state.edge) {
                            Edge.LEFT -> matrix.setScale(
                                scale,
                                scale,
                                cropRect.right,
                                cropRect.centerY()
                            )

                            Edge.TOP -> matrix.setScale(
                                scale,
                                scale,
                                cropRect.centerX(),
                                cropRect.bottom
                            )

                            Edge.RIGHT -> matrix.setScale(
                                scale,
                                scale,
                                cropRect.left,
                                cropRect.centerY()
                            )

                            Edge.BOTTOM -> matrix.setScale(
                                scale,
                                scale,
                                cropRect.centerX(),
                                cropRect.top
                            )

                            else -> {

                            }
                        }
                        matrix.mapRect(minRect, cropRect)
                    }

                    is DraggingCorner -> {
                        val matrix = Matrix()
                        when (state.corner) {
                            Corner.TOP_RIGHT -> matrix.setScale(
                                scale,
                                scale,
                                cropRect.left,
                                cropRect.bottom
                            )

                            Corner.TOP_LEFT -> matrix.setScale(
                                scale,
                                scale,
                                cropRect.right,
                                cropRect.bottom
                            )

                            Corner.BOTTOM_RIGHT -> matrix.setScale(
                                scale,
                                scale,
                                cropRect.left,
                                cropRect.top
                            )

                            Corner.BOTTOM_LEFT -> matrix.setScale(
                                scale,
                                scale,
                                cropRect.right,
                                cropRect.top
                            )

                            else -> {

                            }
                        }
                        matrix.mapRect(minRect, cropRect)
                    }

                    else -> {

                    }
                }
            }
        }
    }

    /**
     * Calculates maximum possible rectangle that user can
     * drag cropRect
     */
    private fun calculateMaxRect() {
        when (aspectAspectMode) {
            AspectMode.FREE -> {
                val borderRect = RectF().apply {
                    val bitmapBorderRect = RectF()
                    bitmapMatrix.mapRect(bitmapBorderRect, bitmapRect)
                    top = max(bitmapBorderRect.top, viewRect.top)
                    right = min(bitmapBorderRect.right, viewRect.right)
                    bottom = min(bitmapBorderRect.bottom, viewRect.bottom)
                    left = max(bitmapBorderRect.left, viewRect.left)
                }

                when (val state = draggingState) {
                    is DraggingEdge -> {
                        when (state.edge) {
                            Edge.LEFT -> maxRect.set(
                                borderRect.left,
                                cropRect.top,
                                cropRect.right,
                                cropRect.bottom
                            )

                            Edge.TOP -> maxRect.set(
                                cropRect.left,
                                borderRect.top,
                                cropRect.right,
                                cropRect.bottom
                            )

                            Edge.RIGHT -> maxRect.set(
                                cropRect.left,
                                cropRect.top,
                                borderRect.right,
                                cropRect.bottom
                            )

                            Edge.BOTTOM -> maxRect.set(
                                cropRect.left,
                                cropRect.top,
                                cropRect.right,
                                borderRect.bottom
                            )

                            else -> {

                            }
                        }
                    }

                    is DraggingCorner -> {
                        when (state.corner) {
                            Corner.TOP_RIGHT -> maxRect.set(
                                cropRect.left,
                                borderRect.top,
                                borderRect.right,
                                cropRect.bottom
                            )

                            Corner.TOP_LEFT -> maxRect.set(
                                borderRect.left,
                                borderRect.top,
                                cropRect.right,
                                cropRect.bottom
                            )

                            Corner.BOTTOM_RIGHT -> maxRect.set(
                                cropRect.left,
                                cropRect.top,
                                borderRect.right,
                                borderRect.bottom
                            )

                            Corner.BOTTOM_LEFT -> maxRect.set(
                                borderRect.left,
                                cropRect.top,
                                cropRect.right,
                                borderRect.bottom
                            )

                            else -> {

                            }
                        }
                    }

                    else -> {

                    }
                }

            }

            AspectMode.ASPECT -> {
                val borderRect = RectF().apply {
                    val bitmapBorderRect = RectF()
                    bitmapMatrix.mapRect(bitmapBorderRect, bitmapRect)
                    top = max(bitmapBorderRect.top, viewRect.top)
                    right = min(bitmapBorderRect.right, viewRect.right)
                    bottom = min(bitmapBorderRect.bottom, viewRect.bottom)
                    left = max(bitmapBorderRect.left, viewRect.left)
                }

                when (val state = draggingState) {
                    is DraggingEdge -> {
                        var leftScale =
                            (cropRect.centerX() - borderRect.left) / (cropRect.width() / 2f)
                        var topScale =
                            (cropRect.centerY() - borderRect.top) / (cropRect.height() / 2f)
                        var bottomScale =
                            (borderRect.bottom - cropRect.centerY()) / (cropRect.height() / 2f)
                        var rightScale =
                            (borderRect.right - cropRect.centerX()) / (cropRect.width() / 2f)

                        when (state.edge) {
                            Edge.LEFT -> {
                                leftScale = (cropRect.right - borderRect.left) / cropRect.width()
                                val minScale = min(leftScale, min(topScale, bottomScale))
                                val matrix = Matrix()
                                matrix.setScale(
                                    minScale,
                                    minScale,
                                    cropRect.right,
                                    cropRect.centerY()
                                )
                                matrix.mapRect(maxRect, cropRect)
                            }

                            Edge.TOP -> {
                                topScale = (cropRect.bottom - borderRect.top) / cropRect.height()
                                val minScale = min(topScale, min(leftScale, rightScale))
                                val matrix = Matrix()
                                matrix.setScale(
                                    minScale,
                                    minScale,
                                    cropRect.centerX(),
                                    cropRect.bottom
                                )
                                matrix.mapRect(maxRect, cropRect)
                            }

                            Edge.RIGHT -> {
                                rightScale = (borderRect.right - cropRect.left) / cropRect.width()
                                val minScale = min(rightScale, min(topScale, bottomScale))
                                val matrix = Matrix()
                                matrix.setScale(
                                    minScale,
                                    minScale,
                                    cropRect.left,
                                    cropRect.centerY()
                                )
                                matrix.mapRect(maxRect, cropRect)
                            }

                            Edge.BOTTOM -> {
                                bottomScale = (borderRect.bottom - cropRect.top) / cropRect.height()
                                val minScale =
                                    min(bottomScale, min(leftScale, rightScale))
                                val matrix = Matrix()
                                matrix.setScale(
                                    minScale,
                                    minScale,
                                    cropRect.centerX(),
                                    cropRect.top
                                )
                                matrix.mapRect(maxRect, cropRect)
                            }

                            else -> {

                            }
                        }
                    }

                    is DraggingCorner -> {
                        val leftScale = (cropRect.right - borderRect.left) / cropRect.width()
                        val topScale = (cropRect.bottom - borderRect.top) / cropRect.height()
                        val bottomScale = (borderRect.bottom - cropRect.top) / cropRect.height()
                        val rightScale = (borderRect.right - cropRect.left) / cropRect.width()
                        when (state.corner) {
                            Corner.TOP_RIGHT -> {
                                val minScale = min(rightScale, topScale)
                                val matrix = Matrix()
                                matrix.setScale(minScale, minScale, cropRect.left, cropRect.bottom)
                                matrix.mapRect(maxRect, cropRect)
                            }

                            Corner.TOP_LEFT -> {
                                val minScale = min(leftScale, topScale)
                                val matrix = Matrix()
                                matrix.setScale(minScale, minScale, cropRect.right, cropRect.bottom)
                                matrix.mapRect(maxRect, cropRect)
                            }

                            Corner.BOTTOM_RIGHT -> {
                                val minScale = min(rightScale, bottomScale)
                                val matrix = Matrix()
                                matrix.setScale(minScale, minScale, cropRect.left, cropRect.top)
                                matrix.mapRect(maxRect, cropRect)
                            }

                            Corner.BOTTOM_LEFT -> {
                                val minScale = min(leftScale, bottomScale)
                                val matrix = Matrix()
                                matrix.setScale(minScale, minScale, cropRect.right, cropRect.top)
                                matrix.mapRect(maxRect, cropRect)
                            }

                            else -> {

                            }
                        }
                    }

                    else -> {

                    }
                }
            }
        }
    }

    /**
     * If user exceed its limit we override cropRect borders
     */
    private fun updateExceedMaxBorders() {
        if (cropRect.left < maxRect.left) {
            cropRect.left = maxRect.left
        }

        if (cropRect.top < maxRect.top) {
            cropRect.top = maxRect.top
        }

        if (cropRect.right > maxRect.right) {
            cropRect.right = maxRect.right
        }

        if (cropRect.bottom > maxRect.bottom) {
            cropRect.bottom = maxRect.bottom
        }
    }

    /**
     * If user exceed its limit we override cropRect borders
     */
    private fun updateExceedMinBorders() {
        if (cropRect.left > minRect.left) {
            cropRect.left = minRect.left
        }

        if (cropRect.top > minRect.top) {
            cropRect.top = minRect.top
        }

        if (cropRect.right < minRect.right) {
            cropRect.right = minRect.right
        }

        if (cropRect.bottom < minRect.bottom) {
            cropRect.bottom = minRect.bottom
        }
    }

    /**
     * If user miminize the croprect, we need to
     * calculate target centered rectangle according to
     * current cropRect aspect ratio and size. With this
     * target rectangle, we can animate crop rect to
     * center target. and also we can animate bitmap matrix
     * to selected cropRect using this target rectangle.
     */
    private fun calculateCenterTarget() {
        val heightScale = viewHeight / cropRect.height()
        val widthScale = viewWidth / cropRect.width()
        val scale = min(heightScale, widthScale)

        val targetRectWidth = cropRect.width() * scale
        val targetRectHeight = cropRect.height() * scale

        val targetRectLeft = (viewWidth - targetRectWidth) / 2f + marginInPixelSize
        val targetRectTop = (viewHeight - targetRectHeight) / 2f + marginInPixelSize
        val targetRectRight = targetRectLeft + targetRectWidth
        val targetRectBottom = targetRectTop + targetRectHeight

        targetRect.set(targetRectLeft, targetRectTop, targetRectRight, targetRectBottom)
    }

    /**
     * When user change cropRect size by dragging it, cropRect
     * should be animated to center without changing aspect ratio,
     * meanwhile bitmap matrix should be take selected crop rect to
     * the center. This methods take selected crop rect to the cennter.
     */
    private fun animateBitmapToCenterTarget() {
        val newBitmapMatrix = bitmapMatrix.clone()

        val scale = targetRect.width() / cropRect.width()
        val translateX = targetRect.centerX() - cropRect.centerX()
        val translateY = targetRect.centerY() - cropRect.centerY()

        val matrix = Matrix()
        matrix.setScale(scale, scale, cropRect.centerX(), cropRect.centerY())
        matrix.postTranslate(translateX, translateY)
        newBitmapMatrix.postConcat(matrix)

        bitmapMatrix.animateToMatrix(newBitmapMatrix) {
            invalidate()
        }
    }

    /**
     * Animates current croprect to the center position
     */
    private fun animateCropRectToCenterTarget() {
        cropRect.animateTo(targetRect) {
            invalidate()
        }
    }

    /**
     * when user drag bitmap too much, we need to settle bitmap matrix
     * back to the possible closest edge.
     */
    private fun settleDraggedBitmap() {
        val draggedBitmapRect = RectF()
        bitmapMatrix.mapRect(draggedBitmapRect, bitmapRect)

        /**
         * Scale dragged matrix if it needs to
         */
        val widthScale = cropRect.width() / draggedBitmapRect.width()
        val heightScale = cropRect.height() / draggedBitmapRect.height()
        var scale = 1.0f

        if (widthScale > 1.0f || heightScale > 1.0f) {
            scale = max(widthScale, heightScale)
        }

        /**
         * Calculate new scaled matrix for dragged bitmap matrix
         */
        val scaledRect = RectF()
        val scaledMatrix = Matrix()
        scaledMatrix.setScale(scale, scale)
        scaledMatrix.mapRect(scaledRect, draggedBitmapRect)


        /**
         * Calculate translateX
         */
        var translateX = 0f
        if (scaledRect.left > cropRect.left) {
            translateX = cropRect.left - scaledRect.left
        }

        if (scaledRect.right < cropRect.right) {
            translateX = cropRect.right - scaledRect.right
        }

        /**
         * Calculate translateX
         */
        var translateY = 0f
        if (scaledRect.top > cropRect.top) {
            translateY = cropRect.top - scaledRect.top
        }

        if (scaledRect.bottom < cropRect.bottom) {
            translateY = cropRect.bottom - scaledRect.bottom
        }

        /**
         * New temp bitmap matrix
         */
        val newBitmapMatrix = bitmapMatrix.clone()

        val matrix = Matrix()
        matrix.setScale(scale, scale)
        matrix.postTranslate(translateX, translateY)
        newBitmapMatrix.postConcat(matrix)

        bitmapMatrix.animateToMatrix(newBitmapMatrix) {
            invalidate()
        }
    }

    /**
     * Pretend a bitmap matrix value if scale factor will be applied to
     * bitmap matrix. , then returns
     * true, false otherwise.
     * @return true If pretended value is exceed max scale value, false otherwise
     */
    private fun isBitmapScaleExceedMaxLimit(scaleFactor: Float): Boolean {
        val bitmapMatrixCopy = bitmapMatrix.clone()
        bitmapMatrixCopy.preScale(scaleFactor, scaleFactor)

        val invertedBitmapMatrix = Matrix()
        bitmapMatrixCopy.invert(invertedBitmapMatrix)

        val invertedBitmapCropRect = RectF()

        invertedBitmapMatrix.mapRect(invertedBitmapCropRect, cropRect)
        return min(
            invertedBitmapCropRect.width(),
            invertedBitmapCropRect.height()
        ) <= bitmapMinRect.width()
    }

    companion object {

        /**
         * Maximum scale for given bitmap
         */
        private const val MAX_SCALE = 15f

        /**
         * Use this constant, when user double tap to scale
         */
        private const val DOUBLE_TAP_SCALE_FACTOR = 2f

    }
}