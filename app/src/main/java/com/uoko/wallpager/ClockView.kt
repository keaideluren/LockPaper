package com.uoko.wallpager

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat.getSystemService
import java.util.*
import kotlin.concurrent.timerTask

/**
 * Created by 拇指 on 2019/9/16 0016.
 * Email:muzhi@uoko.com
 * 抖音网红时钟
 */
class ClockView : View {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var textSize = dp2px(10F)
        set(value) {
            field = value
            halfTextSize = textSize.toFloat() / 2 - 6
        }
    private var halfTextSize = textSize.toFloat() / 2 - 6
    private val paintNormal = Paint()
    private val paintCurrent = Paint()
    private var hmsTextWidth: Float
    private var weekWidth: Float
    private var monthWidth: Float

    //时间tick时的动画
    private var scrollAnim: ValueAnimator

    //切换模式动画
    private var changeModeAnim: ValueAnimator

    //时间跳前的动画，额外多旋转一个角度
    private var extraRotation = 0F

    //切换动画进度
    private var changeProgress = 0F

    //时间tick一秒一次
    private var ticketTimerTask: TimerTask? = null

    //20秒一次，切换模式
    private var changeModeTask: TimerTask? = null

    //时间循环,这些都是懒得引入协程的东东
    private var timer: Timer? = null

    //模式 0圆盘  1的切换中动画  2表格
    private var mode = 2

    private var currentHour = 0
    private var currentMinute = 0
    private var currentSecont = 0
    private var currentMonth = 0
    private var currentDay = 0

    //几号
    private var currentDate = 0

    //一个月有多少天
    private var dayOfMonth = 0

    private var mWidth: Float? = null
    private var mHeight: Float? = null

    private var tableTopHalf = 0F

    //颜色
    var currentColor: Int = Color.WHITE
        set(value) {
            field = value
            paintCurrent.color = value
        }

    //颜色
    var normalColor: Int = Color.argb(0x66, 255, 255, 255)
        set(value) {
            field = value
            paintNormal.color = value
        }

    //绘制回调
    var drawCallback: (() -> Unit)? = null

    private var newHandler: Handler = Handler { true }

    private var sensorEventListener: SensorEventListener = object : SensorEventListener {
        var isChangeing = false

        override fun onSensorChanged(event: SensorEvent) {
            if (!isChangeing) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val accelerometer = Math.sqrt((x * x + y * y + z * z).toDouble())
                if (accelerometer > 15 || accelerometer < 5) {
                    //大概半个重力加速度，算是
                    isChangeing = true
                    Timer().schedule(timerTask {
                        //3秒后
                        isChangeing = false
                    }, 1000)

                    changeMode()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }
    private var sensorManager: SensorManager

    init {
        setBackgroundColor(Color.BLACK)
        paintNormal.textSize = textSize.toFloat()
        paintNormal.color = normalColor
        paintNormal.isAntiAlias = true

        paintCurrent.textSize = textSize.toFloat()
        paintCurrent.color = currentColor
        paintCurrent.isAntiAlias = true

        hmsTextWidth = paintNormal.measureText("00秒")
        weekWidth = paintNormal.measureText("星期一")
        monthWidth = paintNormal.measureText("00月")

        scrollAnim = ValueAnimator.ofFloat(0F, 1F)
        scrollAnim.duration = 200

        newTime()

        changeModeAnim = ValueAnimator.ofFloat(0F, 1F)
        changeModeAnim.duration = 800

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        initWidthHeight(measuredWidth.toFloat(), measuredHeight.toFloat())
    }

    private fun newTime() {
        val calendar = Calendar.getInstance()
        currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        currentMinute = calendar.get(Calendar.MINUTE)
        currentSecont = calendar.get(Calendar.SECOND)
        currentMonth = calendar.get(Calendar.MONTH)
        currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        currentDate = calendar.get(Calendar.DATE)
        dayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun registerSensor() {
        sensorManager.registerListener(
            sensorEventListener,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun unregisterSensor() {
        sensorManager.unregisterListener(sensorEventListener)
    }

    /**
     * 初始化宽高，供动态壁纸使用
     */
    fun initWidthHeight(width: Float, height: Float) {
        this.mWidth = width
        this.mHeight = height
        tableTopHalf = (height - 34 * textSize * 1.5F - 5 * textSize) / 2
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        create()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        destroy()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCanvas(canvas)
    }

    fun drawCanvas(canvas: Canvas) {
        when (mode) {
            0 -> drawTicket(canvas)
            1 -> drawAnim(canvas)
            2 -> drawTable(canvas)
        }
    }

    /**
     * 普通的，圆盘走时
     */
    private fun drawTicket(canvas: Canvas) {
        val widthUseable = mWidth ?: measuredWidth.toFloat() - paddingStart - paddingEnd
        val heightUseable = mHeight ?: measuredHeight.toFloat() - paddingTop - paddingBottom
        val maxSize = Math.min(widthUseable, heightUseable) / 2
        //画布将0，0移到屏幕中央
        canvas.translate(widthUseable / 2, heightUseable / 2)
        //月
        canvas.save()
        if (currentDate == dayOfMonth && currentHour == 23 && currentMinute == 59 && currentSecont == 59) {
            canvas.rotate(-currentMonth * 30F - 30F * extraRotation)
        } else {
            canvas.rotate(-currentMonth * 30F)
        }
        for (i in 0..11) {
            if (currentMonth == i) {
                canvas.drawText(
                    "${i + 1}月",
                    maxSize - hmsTextWidth * 4 - weekWidth - monthWidth,
                    halfTextSize,
                    paintCurrent
                )
            } else {
                canvas.drawText(
                    "${i + 1}月",
                    maxSize - hmsTextWidth * 4 - weekWidth - monthWidth,
                    halfTextSize,
                    paintNormal
                )
            }
            canvas.rotate(360F / 12)
        }
        canvas.restore()

        //日期
        canvas.save()
        if (currentHour == 23 && currentMinute == 59 && currentSecont == 59) {
            canvas.rotate(-(currentDate - 1) * (360F / dayOfMonth) - (360F / dayOfMonth) * extraRotation)
        } else {
            canvas.rotate(-(currentDate - 1) * (360F / dayOfMonth))
        }
        for (i in 1..dayOfMonth) {
            if (currentDate == i) {
                canvas.drawText(
                    "${i}日",
                    maxSize - hmsTextWidth * 4 - weekWidth,
                    halfTextSize,
                    paintCurrent
                )
            } else {
                canvas.drawText(
                    "${i}日",
                    maxSize - hmsTextWidth * 4 - weekWidth,
                    halfTextSize,
                    paintNormal
                )
            }
            canvas.rotate(360F / dayOfMonth)
        }
        canvas.restore()

        //星期
        canvas.save()
        if (currentHour == 23 && currentMinute == 59 && currentSecont == 59) {
            canvas.rotate(-(currentDay - 1) * (360F / 7) - (360F / 7) * extraRotation)
        } else {
            canvas.rotate(-(currentDay - 1) * (360F / 7))
        }
        for (i in 1..7) {
            if (currentDay == i) {
                canvas.drawText(
                    getWeekDay(i),
                    maxSize - hmsTextWidth * 3 - weekWidth,
                    halfTextSize,
                    paintCurrent
                )
            } else {
                canvas.drawText(
                    getWeekDay(i),
                    maxSize - hmsTextWidth * 3 - weekWidth,
                    halfTextSize,
                    paintNormal
                )
            }
            canvas.rotate(360F / 7)
        }
        canvas.restore()

        //时
        canvas.save()
        if (currentMinute == 59 && currentSecont == 59) {
            canvas.rotate(-currentHour * 15F - extraRotation * 15F)
        } else {
            canvas.rotate(-currentHour * 15F)
        }
        for (i in 0..23) {
            if (currentHour == i) {
                canvas.drawText(
                    "${i}时",
                    maxSize - hmsTextWidth * 3,
                    halfTextSize, paintCurrent
                )
            } else {
                canvas.drawText(
                    "${i}时",
                    maxSize - hmsTextWidth * 3,
                    halfTextSize, paintNormal
                )
            }
            canvas.rotate(15F)
        }
        canvas.restore()

        //分
        canvas.save()
        if (currentSecont == 59) {
            canvas.rotate(-currentMinute * 6F - extraRotation * 6F)
        } else {
            canvas.rotate(-currentMinute * 6F)
        }
        for (i in 0..59) {
            if (currentMinute == i) {
                canvas.drawText(
                    "${i}分",
                    maxSize - hmsTextWidth * 2,
                    halfTextSize, paintCurrent
                )
            } else {
                canvas.drawText(
                    "${i}分",
                    maxSize - hmsTextWidth * 2,
                    halfTextSize,
                    paintNormal
                )
            }
            canvas.rotate(6F)
        }
        canvas.restore()

        //秒
        canvas.save()
        canvas.rotate(-currentSecont * 6F - extraRotation * 6)
        for (i in 0..59) {
            if (currentSecont == i) {
                canvas.drawText(
                    "${i}秒",
                    maxSize - hmsTextWidth,
                    halfTextSize,
                    paintCurrent
                )
            } else {
                canvas.drawText(
                    "${i}秒",
                    maxSize - hmsTextWidth,
                    halfTextSize,
                    paintNormal
                )
            }
            canvas.rotate(6F)
        }
        canvas.restore()
    }

    private fun drawAnim(canvas: Canvas) {
        val widthUseable = mWidth ?: measuredWidth.toFloat() - paddingStart - paddingEnd
        val heightUseable = mHeight ?: measuredHeight.toFloat() - paddingTop - paddingBottom
        val maxSize = Math.min(widthUseable, heightUseable)
        val halfMaxSize = maxSize / 2
        //月F
        canvas.save()
        for (i in 0..11) {
            //表格模式X位置
            val tableX = tableX(i, maxSize)
            //表格模式Y位置
            val tableY = tableY(i) + tableTopHalf
            //转盘模式旋转角度
            var panRotate = 30F * i - 30F * currentMonth
            if (currentDate == dayOfMonth && currentHour == 23 && currentMinute == 59 && currentSecont == 59) {
                panRotate -= 30F * extraRotation
            }
            //文字X偏移，相对
            val textX = halfMaxSize - hmsTextWidth * 4 - weekWidth - monthWidth
            //表盘模式文字Xcanvas中位置,textX应该用sqrt(textx^2,textY^2)
            val panX = panX(halfMaxSize, textX, widthUseable, panRotate)
            //表盘模式Ycanvas中位置
            val panY = panY(panRotate, textX, heightUseable)

            val currentX = currentX(panX, tableX)
            val currentY = currentY(panY, tableY)
            val currentRotate = panRotate * (1F - changeProgress)

            canvas.save()
            canvas.translate(currentX, currentY)
            canvas.rotate(currentRotate)
            if (currentMonth == i) {
                canvas.drawText("${i + 1}月", 0F, 0F, paintCurrent)
            } else {
                canvas.drawText("${i + 1}月", 0F, 0F, paintNormal)
            }
            canvas.restore()
        }
        canvas.restore()


        //日期
        canvas.save()
        for (i in 1..dayOfMonth) {
            //表格模式X位置
            val tableX = tableX(i - 1, maxSize)
            //表格模式Y位置
            val tableY = tableY(i - 1) +
                    tableTopHalf + //固定100
                    2 * textSize * 1.5F + textSize//月份两行+固定一行间距
            //转盘模式旋转角度
            var panRotate = (360F / dayOfMonth) * (i - currentDate)
            if (currentHour == 23 && currentMinute == 59 && currentSecont == 59) {
                panRotate -= 360F / dayOfMonth * extraRotation
            }
            //文字X偏移，相对
            val textX = halfMaxSize - hmsTextWidth * 4 - weekWidth
            //表盘模式文字Xcanvas中位置,textX应该用sqrt(textx^2,textY^2)
            val panX = panX(halfMaxSize, textX, widthUseable, panRotate)
            //表盘模式Ycanvas中位置
            val panY = panY(panRotate, textX, heightUseable)

            val currentX = currentX(panX, tableX)
            val currentY = currentY(panY, tableY)
            val currentRotate = panRotate * (1F - changeProgress)

            canvas.save()
            canvas.translate(currentX, currentY)
            canvas.rotate(currentRotate)
            if (currentDate == i) {
                canvas.drawText("${i}日", 0F, 0F, paintCurrent)
            } else {
                canvas.drawText("${i}日", 0F, 0F, paintNormal)
            }
            canvas.restore()
        }
        canvas.restore()


        //星期
        canvas.save()
        for (i in 1..7) {
            //表格模式X位置
            val tableX = tableX(i - 1, maxSize)
            //表格模式Y位置
            val tableY = tableY(i - 1) +
                    tableTopHalf + //固定100
                    2 * textSize * 1.5F + textSize +//月份两行+固定一行间距
                    6 * textSize * 1.5F + textSize
            //转盘模式旋转角度
            var panRotate = (360F / 7) * (i - currentDay)
            if (currentHour == 23 && currentMinute == 59 && currentSecont == 59) {
                panRotate -= 360F / 7 * extraRotation
            }
            //文字X偏移，相对
            val textX = halfMaxSize - hmsTextWidth * 3 - weekWidth
            //表盘模式文字Xcanvas中位置,textX应该用sqrt(textx^2,textY^2)
            val panX = panX(halfMaxSize, textX, widthUseable, panRotate)
            //表盘模式Ycanvas中位置
            val panY = panY(panRotate, textX, heightUseable)

            val currentX = currentX(panX, tableX)
            val currentY = currentY(panY, tableY)
            val currentRotate = panRotate * (1F - changeProgress)

            canvas.save()
            canvas.translate(currentX, currentY)
            canvas.rotate(currentRotate)
            if (currentDay == i) {
                canvas.drawText(getWeekDay(i), 0F, 0F, paintCurrent)
            } else {
                canvas.drawText(getWeekDay(i), 0F, 0F, paintNormal)
            }
            canvas.restore()
        }
        canvas.restore()


        //时
        canvas.save()
        for (i in 0..23) {
            //表格模式X位置
            val tableX = tableX(i, maxSize)
            //表格模式Y位置
            val tableY = tableY(i) +
                    tableTopHalf + //固定100
                    2 * textSize * 1.5F + textSize +//月份两行+固定一行间距
                    6 * textSize * 1.5F + textSize +//日
                    2 * textSize * 1.5F + textSize//星期
            //转盘模式旋转角度
            var panRotate = 15F * (i - currentHour)
            if (currentMinute == 59 && currentSecont == 59) {
                panRotate -= 15F * extraRotation
            }
            //文字X偏移，相对
            val textX = halfMaxSize - hmsTextWidth * 3
            //表盘模式文字Xcanvas中位置,textX应该用sqrt(textx^2,textY^2)
            val panX = panX(halfMaxSize, textX, widthUseable, panRotate)
            //表盘模式Ycanvas中位置
            val panY = panY(panRotate, textX, heightUseable)

            val currentX = currentX(panX, tableX)
            val currentY = currentY(panY, tableY)
            val currentRotate = panRotate * (1F - changeProgress)

            canvas.save()
            canvas.translate(currentX, currentY)
            canvas.rotate(currentRotate)
            if (currentHour == i) {
                canvas.drawText("${i}时", 0F, 0F, paintCurrent)
            } else {
                canvas.drawText("${i}时", 0F, 0F, paintNormal)
            }
            canvas.restore()
        }
        canvas.restore()

        //分
        canvas.save()
        for (i in 0..59) {
            //表格模式X位置
            val tableX = tableX(i, maxSize)
            //表格模式Y位置
            val tableY = tableY(i) +
                    tableTopHalf + //固定100
                    2 * textSize * 1.5F + textSize +//月份两行+固定一行间距
                    6 * textSize * 1.5F + textSize +//日
                    2 * textSize * 1.5F + textSize +//星期
                    4 * textSize * 1.5F + textSize//时
            //转盘模式旋转角度
            var panRotate = 6F * (i - currentMinute)
            if (currentSecont == 59) {
                panRotate -= 6F * extraRotation
            }
            //文字X偏移，相对
            val textX = halfMaxSize - hmsTextWidth * 2
            //表盘模式文字Xcanvas中位置,textX应该用sqrt(textx^2,textY^2)
            val panX = panX(halfMaxSize, textX, widthUseable, panRotate)
            //表盘模式Ycanvas中位置
            val panY = panY(panRotate, textX, heightUseable)

            val currentX = currentX(panX, tableX)
            val currentY = currentY(panY, tableY)
            val currentRotate = panRotate * (1F - changeProgress)

            canvas.save()
            canvas.translate(currentX, currentY)
            canvas.rotate(currentRotate)
            if (currentMinute == i) {
                canvas.drawText("${i}分", 0F, 0F, paintCurrent)
            } else {
                canvas.drawText("${i}分", 0F, 0F, paintNormal)
            }
            canvas.restore()
        }
        canvas.restore()

        //秒
        canvas.save()
        for (i in 0..59) {
            //表格模式X位置
            val tableX = tableX(i, maxSize)
            //表格模式Y位置
            val tableY = tableY(i) +
                    tableTopHalf + //固定100
                    2 * textSize * 1.5F + textSize +//月份两行+固定一行间距
                    6 * textSize * 1.5F + textSize +//日
                    2 * textSize * 1.5F + textSize +//星期
                    4 * textSize * 1.5F + textSize +//时
                    10 * textSize * 1.5F + textSize//分
            //转盘模式旋转角度
            val panRotate = 6F * (i - currentSecont) - 6F * extraRotation
            //文字X偏移，相对
            val textX = halfMaxSize - hmsTextWidth
            //表盘模式文字Xcanvas中位置,textX应该用sqrt(textx^2,textY^2)
            val panX = panX(halfMaxSize, textX, widthUseable, panRotate)
            //表盘模式Ycanvas中位置
            val panY = panY(panRotate, textX, heightUseable)

            val currentX = currentX(panX, tableX)
            val currentY = currentY(panY, tableY)
            val currentRotate = panRotate * (1F - changeProgress)

            canvas.save()
            canvas.translate(currentX, currentY)
            canvas.rotate(currentRotate)
            if (currentSecont == i) {
                canvas.drawText("${i}秒", 0F, 0F, paintCurrent)
            } else {
                canvas.drawText("${i}秒", 0F, 0F, paintNormal)
            }
            canvas.restore()
        }
        canvas.restore()

    }

    private fun panX(
        halfMaxSize: Float,
        textX: Float,
        widthUseable: Float,
        panRotate: Float
    ): Float {
        //textY heightUseable/2 - halfTextSize
        //表盘模式文字Xcanvas中位置,textX应该用sqrt(textx^2,textY^2)
        return Math.cos(panRotate / 180 * Math.PI).toFloat() * textX + widthUseable / 2
    }

    private fun panY(panRotate: Float, textX: Float, heightUseable: Float): Float {
        return Math.sin(panRotate / 180 * Math.PI)
            .toFloat() * textX + heightUseable / 2 + halfTextSize
    }

    private fun currentX(panX: Float, tableX: Float): Float {
        return panX * (1F - changeProgress) + tableX * changeProgress
    }


    private fun currentY(panY: Float, tableY: Float): Float {
        return panY * (1F - changeProgress) + tableY * changeProgress
    }

    private fun tableX(i: Int, maxSize: Float): Float {
        return (i % 6) * maxSize / 6 + 50
    }

    private fun tableY(i: Int): Float {
        return i / 6 * textSize * 1.5F
    }

    private fun drawTable(canvas: Canvas) {
        val widthUseable = mWidth ?: measuredWidth.toFloat() - paddingStart - paddingEnd
        val heightUseable = mHeight ?: measuredHeight.toFloat() - paddingTop - paddingBottom
        val maxSize = Math.min(widthUseable, heightUseable)
        canvas.translate(0F, tableTopHalf)
        //月
        for (i in 0..11) {
            val currentX = tableX(i, maxSize)
            val currentY = tableY(i)
            if (currentMonth == i) {
                canvas.drawText(
                    "${i + 1}月",
                    currentX,
                    currentY,
                    paintCurrent
                )
            } else {
                canvas.drawText(
                    "${i + 1}月",
                    currentX,
                    currentY,
                    paintNormal
                )
            }
        }

        canvas.translate(0F, textSize * 1.5F * 2 + textSize)
        //日期
        for (i in 1..dayOfMonth) {
            val currentX = tableX(i - 1, maxSize)
            val currentY = tableY(i - 1)
            if (currentDate == i) {
                canvas.drawText(
                    "${i}日",
                    currentX,
                    currentY,
                    paintCurrent
                )
            } else {
                canvas.drawText(
                    "${i}日",
                    currentX,
                    currentY,
                    paintNormal
                )
            }
        }

        canvas.translate(0F, textSize * 1.5F * 6 + textSize)

        //星期
        for (i in 1..7) {
            val currentX = tableX(i - 1, maxSize)
            val currentY = tableY(i - 1)
            if (currentDay == i) {
                canvas.drawText(
                    getWeekDay(i),
                    currentX,
                    currentY,
                    paintCurrent
                )
            } else {
                canvas.drawText(
                    getWeekDay(i),
                    currentX,
                    currentY,
                    paintNormal
                )
            }
        }

        canvas.translate(0F, textSize * 1.5F * 2 + textSize)

        //时
        for (i in 0..23) {
            val currentX = tableX(i, maxSize)
            val currentY = tableY(i)
            if (currentHour == i) {
                canvas.drawText(
                    "${i}时",
                    currentX,
                    currentY, paintCurrent
                )
            } else {
                canvas.drawText(
                    "${i}时",
                    currentX,
                    currentY, paintNormal
                )
            }
        }

        canvas.translate(0F, textSize * 1.5F * 4 + textSize)

        //分
        for (i in 0..59) {
            val currentX = tableX(i, maxSize)
            val currentY = tableY(i)
            if (currentMinute == i) {
                canvas.drawText(
                    "${i}分",
                    currentX,
                    currentY, paintCurrent
                )
            } else {
                canvas.drawText(
                    "${i}分",
                    currentX,
                    currentY,
                    paintNormal
                )
            }
        }

        canvas.translate(0F, textSize * 1.5F * 10 + textSize)

        //秒
        for (i in 0..59) {
            val currentX = tableX(i, maxSize)
            val currentY = tableY(i)
            if (currentSecont == i) {
                canvas.drawText(
                    "${i}秒",
                    currentX,
                    currentY,
                    paintCurrent
                )
            } else {
                canvas.drawText(
                    "${i}秒",
                    currentX,
                    currentY,
                    paintNormal
                )
            }
        }
    }

    private fun View.dp2px(dpValue: Float): Int {
        val scale = resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    private fun getWeekDay(day: Int): String {
        return when (day) {
            Calendar.MONDAY -> "星期一"

            Calendar.TUESDAY -> "星期二"

            Calendar.WEDNESDAY -> "星期三"

            Calendar.THURSDAY -> "星期四"

            Calendar.FRIDAY -> "星期五"

            Calendar.SATURDAY -> "星期六"

            Calendar.SUNDAY -> "星期天"

            else -> "大爷"
        }
    }

    fun create() {
        scrollAnim.addUpdateListener {
            extraRotation = it.animatedValue as Float
            drawCallback?.invoke()
            postInvalidate()
        }
        scrollAnim.addListener(onEnd = {
            extraRotation = 0F
            newTime()
            drawCallback?.invoke()
            postInvalidate()
        })

        changeModeAnim.addUpdateListener {
            changeProgress = it.animatedValue as Float
            drawCallback?.invoke()
            postInvalidate()
        }
        changeModeAnim.addListener(onStart = {
            mode = 1
            drawCallback?.invoke()
            postInvalidate()
        }, onEnd = {
            if (changeProgress < 0.5F) {
                mode = 0
            } else {
                mode = 2
            }
            drawCallback?.invoke()
            postInvalidate()
        })

        timer?.cancel()
        timer = Timer()
        ticketTimerTask = object : TimerTask() {
            override fun run() {
                if (!scrollAnim.isStarted) {
                    newHandler.post {
                        scrollAnim.start()
                    }
                }
            }
        }
        changeModeTask = object : TimerTask() {
            override fun run() {
                changeMode()
            }
        }
        //一秒一次，走时的
        timer!!.schedule(ticketTimerTask, 1000, 1000)
        //20秒一次，切换模式
//        timer!!.schedule(changeModeTask, 10 * 1000, 20*1000)

    }

    fun changeMode() {
        newHandler.post {
            if (mode == 0) {
                changeModeAnim.start()
            } else {
                changeModeAnim.reverse()
            }
        }
    }

    fun changeModeDelay(delay: Long) {
        newHandler.removeCallbacksAndMessages(null)
        newHandler.postDelayed({
            if (mode == 0) {
                changeModeAnim.start()
            } else {
                changeModeAnim.reverse()
            }
        }, delay)
    }

    fun destroy() {
        timer?.cancel()
        scrollAnim.cancel()
        scrollAnim.removeAllUpdateListeners()
        scrollAnim.removeAllListeners()
        changeModeAnim.cancel()
        changeModeAnim.removeAllUpdateListeners()
        changeModeAnim.removeAllListeners()
    }
}