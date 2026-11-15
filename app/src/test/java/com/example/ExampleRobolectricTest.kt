package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.detector.FruitDetector
import com.example.model.DropTarget
import com.example.model.FruitDropConfig
import com.example.model.FruitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `launch MainActivity without crashing`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FruitDrop", appName)
    }

    @Test
    fun `test default fruit drop targets match user request`() {
        val config = FruitDropConfig()

        // Marked fruits must drop LEFT
        assertEquals(DropTarget.LEFT, config.getTargetForFruit(FruitType.KIWI_HIJAU))
        assertEquals(DropTarget.LEFT, config.getTargetForFruit(FruitType.PEACH))

        // Smaller fruits must drop RIGHT
        assertEquals(DropTarget.RIGHT, config.getTargetForFruit(FruitType.BLUEBERRY))
        assertEquals(DropTarget.RIGHT, config.getTargetForFruit(FruitType.STRAWBERRY))
        assertEquals(DropTarget.RIGHT, config.getTargetForFruit(FruitType.ORANGE))
    }

    @Test
    fun `classify HSV colors directly`() {
        assertEquals(FruitType.KIWI_HIJAU, FruitDetector.classifyHsv(80f, 0.70f, 0.85f))
        assertEquals(FruitType.PEACH, FruitDetector.classifyHsv(20f, 0.35f, 0.95f))
        assertEquals(FruitType.BLUEBERRY, FruitDetector.classifyHsv(235f, 0.75f, 0.85f))
        assertEquals(FruitType.STRAWBERRY, FruitDetector.classifyHsv(355f, 0.85f, 0.80f))
        assertEquals(FruitType.ORANGE, FruitDetector.classifyHsv(40f, 0.80f, 0.90f))
    }

    @Test
    fun `detect kiwi from bitmap using pixel buffer`() {
        val width = 360
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val config = FruitDropConfig(scanRelX = 0.5f, scanRelY = 0.5f, scanRadiusDp = 20)
        val cx = (config.scanRelX * width).toInt()
        val cy = (config.scanRelY * height).toInt()

        val kiwiColor = Color.rgb(167, 217, 65)
        for (y in (cy - 15)..(cy + 15)) {
            for (x in (cx - 15)..(cx + 15)) {
                bitmap.setPixel(x, y, kiwiColor)
            }
        }

        val result = FruitDetector.detectFruit(bitmap, config)
        assertEquals(FruitType.KIWI_HIJAU, result.fruit)
        assertEquals(DropTarget.LEFT, result.target)
    }

    @Test
    fun `detect blueberry from bitmap using pixel buffer`() {
        val width = 360
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val config = FruitDropConfig(scanRelX = 0.5f, scanRelY = 0.5f, scanRadiusDp = 20)
        val cx = (config.scanRelX * width).toInt()
        val cy = (config.scanRelY * height).toInt()

        val blueberryColor = Color.rgb(65, 80, 220)
        for (y in (cy - 15)..(cy + 15)) {
            for (x in (cx - 15)..(cx + 15)) {
                bitmap.setPixel(x, y, blueberryColor)
            }
        }

        val result = FruitDetector.detectFruit(bitmap, config)
        assertEquals(FruitType.BLUEBERRY, result.fruit)
        assertEquals(DropTarget.RIGHT, result.target)
    }
}
