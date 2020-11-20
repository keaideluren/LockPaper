package com.uoko.wallpager

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.hardware.*
import android.service.wallpaper.WallpaperService
import java.util.*
import kotlin.concurrent.timer
import kotlin.concurrent.timerTask

/**
 * Created by 拇指 on 2019/9/18 0018.
 * Email:muzhi@uoko.com
 * 壁纸服务
 */
class DYWallService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return DYEngine()
    }

    inner class DYEngine : Engine() {
        private var handler: android.os.Handler
        private var viewClockView: ClockView

        init {
            handler = android.os.Handler { msg ->
                when (msg?.what) {
                    0 -> surfaceHolder.lockCanvas()?.let {
                        drawCanvas(it)
                    }
                }
                true
            }
            viewClockView = ClockView(baseContext)
            val width = ScreenUtils.getScreenWidth(baseContext)
            val height = ScreenUtils.getScreenHeight(baseContext)
            viewClockView.initWidthHeight(width.toFloat(), height.toFloat())
            viewClockView.drawCallback = {
                surfaceHolder.lockCanvas()?.let {
                    drawCanvas(it)
                }
            }

        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                viewClockView.create()
                surfaceHolder.lockCanvas()?.let {
                    drawCanvas(it)
                }
                viewClockView.registerSensor()
            } else {
                viewClockView.destroy()
                viewClockView.unregisterSensor()
            }
        }

        private fun drawCanvas(canvas: Canvas) {
            canvas.save()
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            viewClockView.draw(canvas)
            canvas.restore()
            surfaceHolder.unlockCanvasAndPost(canvas)
        }
    }
}