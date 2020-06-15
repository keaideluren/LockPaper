package com.uoko.wallpager

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.view_gravity.view.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        setContentView(R.layout.activity_main)
        initPager()
    }

    private fun initPager() {
        vp_wall.adapter = object : PagerAdapter() {
            override fun isViewFromObject(view: View, viewObject: Any): Boolean {
                return view == viewObject
            }

            override fun getCount(): Int {
                return 2
            }

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                val v = when (position) {
                    0 -> {
                        val gravityView = LayoutInflater.from(this@MainActivity)
                            .inflate(R.layout.view_gravity, container, false)


                        gravityView.seek_1.setOnSeekBarChangeListener(object :
                            SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(
                                seekBar: SeekBar?,
                                progress: Int,
                                fromUser: Boolean
                            ) {
                                gravityView.gv.zSpaceDistance = progress * 20F
                            }

                            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                            }

                            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            }

                        })
                        gravityView.seek_2.setOnSeekBarChangeListener(object :
                            SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(
                                seekBar: SeekBar?,
                                progress: Int,
                                fromUser: Boolean
                            ) {
                                gravityView.gv.zSapceExtra = progress * 100F

                            }

                            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                            }

                            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            }

                        })
                        gravityView.seek_3.setOnSeekBarChangeListener(object :
                            SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(
                                seekBar: SeekBar?,
                                progress: Int,
                                fromUser: Boolean
                            ) {
                                //0 - 100
                                gravityView.gv.count = progress
                            }

                            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                            }

                            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            }

                        })
                        gravityView
                    }
                    1 -> ClockView(this@MainActivity)
                    else -> View(this@MainActivity)
                }

                container.addView(v)
                return v
            }

            override fun destroyItem(container: ViewGroup, position: Int, viewObject: Any) {
                container.removeView(viewObject as View)
            }
        }
    }

    fun setPager(v: View) {
        val intent = Intent().apply {
            action = WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(
                    this@MainActivity,
                    if (vp_wall.currentItem == 1) DYWallService::class.java else GravityWallService::class.java
                )
            )
        }
        startActivity(intent)
    }
}
