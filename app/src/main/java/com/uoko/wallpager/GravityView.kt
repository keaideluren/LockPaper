package com.uoko.wallpager

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.*
import android.util.AttributeSet
import android.util.Log
import android.view.View

/**
 * Created by 拇指 on 2020/6/12.
 * Email:muzhi@uoko.com
 * 重力感应的壁纸
 */
class GravityView : View {
    private var sWidth: Int = 0
    private var sHeight: Int = 0
    var sensorCallback: (() -> Unit)? = null
    private lateinit var sensorManager: SensorManager
    private lateinit var rectPaint: Paint
    private lateinit var gravityListener: SensorEventListener
    private var xGravtiy: Float = 0F
    private var yGravtiy: Float = 0F
    private var zGravtiy: Float = 1F

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        initSensor()
    }

    fun pauseSensor() {
        sensorManager.unregisterListener(gravityListener)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        initWidthHeight(measuredWidth, measuredHeight)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pauseSensor()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        resumeSensor()
    }

    fun resumeSensor() {
        sensorManager.registerListener(
            gravityListener,
            sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
            SensorManager.SENSOR_DELAY_UI
        )

    }

    fun initSensor() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gravityListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }

            override fun onSensorChanged(event: SensorEvent) {
                xGravtiy = event.values[0]
                yGravtiy = event.values[1]
                zGravtiy = event.values[2]
                //嘿，重力加速度为9.8066，还挺精确
                sensorCallback?.invoke()
                sensorCallback?:invalidate()
            }
        }

        rectPaint = Paint()
        rectPaint.style = Paint.Style.STROKE
        rectPaint.color = Color.RED
        rectPaint.strokeWidth = 1F
    }

    fun initWidthHeight(width:Int, height:Int){
        sWidth = width
        sHeight = height
    }

    var squarSize = 541F//1000个像素大小,短轴方向
    var zSpaceDistance = 500F//每个框之间的距离
    var zSapceExtra = 1500F//观察者距第一个框的距离
    var count = 20

    /**
     * 画十个框框，近大远小,框大小为squarSize，假设第0个距离为zSapceExtra，依次为n * zSpaceDistance + zSapceExtra
     * 那么，实际框的大小为一样大的话，对于视角来说为arctan(squarSize / (n * zSpaceDistance + zSapceExtra))
     * 屏幕只是二维的，那么，后面的框的投影大小为tan(arctan(squarSize / (n * zSpaceDistance + zSapceExtra))) * zSapceExtra
     * = squarSize * zSapceExtra/(n * zSpaceDistance + zSapceExtra)
     */
    override fun onDraw(canvas: Canvas) {
        var longAxisSize = squarSize / (Math.min(sWidth, sHeight)) * Math.max(sWidth, sHeight)

        val numerator = zSapceExtra

        if (sWidth > sHeight) {
            //横屏
            val sTemp = squarSize
            squarSize = longAxisSize
            longAxisSize = sTemp
        }
        canvas.translate((sWidth / 2).toFloat(), (sHeight / 2).toFloat())
        val tranX = xGravtiy / 9.8F
        val tranY = yGravtiy / 9.8F
//        val tranX = 0
//        val tranY = 0
        for (i in 0..count) {
            val squarSizeAct = numerator / (i * zSpaceDistance + zSapceExtra)
            canvas.drawCircle(-tranX * (1-squarSizeAct) * squarSize,tranY * (1-squarSizeAct) * longAxisSize, squarSize * squarSizeAct, rectPaint)
//            canvas.drawRect(
//                -squarSize * squarSizeAct - tranX * (1-squarSizeAct) * squarSize,
//                -longAxisSize * squarSizeAct + tranY * (1-squarSizeAct) * longAxisSize,
//                squarSize * squarSizeAct - tranX * (1-squarSizeAct) * squarSize,
//                longAxisSize * squarSizeAct + tranY * (1-squarSizeAct) * longAxisSize,
//                rectPaint
//            )
        }
    }
}