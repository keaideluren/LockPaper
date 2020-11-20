package com.uoko.wallpager

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.service.wallpaper.WallpaperService

/**
 * Created by 拇指 on 2019/9/18 0018.
 * Email:muzhi@uoko.com
 * 壁纸服务
 */
class GravityWallService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return GravityEngine()
    }

    inner class GravityEngine : Engine() {
        private var handler: android.os.Handler
        private var viewClockView: GravityView

        init {
            handler = android.os.Handler { msg ->
                when (msg?.what) {
                    0 -> surfaceHolder.lockCanvas()?.let {
                        drawCanvas(it)
                    }
                }
                true
            }
            viewClockView = GravityView(baseContext)
            val width = ScreenUtils.getScreenWidth(baseContext)
            val height = ScreenUtils.getScreenHeight(baseContext)
            viewClockView.initWidthHeight(width, height)
            viewClockView.sensorCallback = {
                surfaceHolder.lockCanvas()?.let {
                    drawCanvas(it)
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                viewClockView.resumeSensor()
                surfaceHolder.lockCanvas()?.let {
                    drawCanvas(it)
                }
            } else {
                viewClockView.pauseSensor()
            }
        }

        private fun drawCanvas(canvas: Canvas) {
            canvas.save()
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            viewClockView.draw(canvas)
            canvas.restore()
            surfaceHolder.unlockCanvasAndPost(canvas)
        }

    }
}