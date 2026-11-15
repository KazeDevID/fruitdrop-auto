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
        assertEquals(DropTarget.RIGHT, config.getTargetForFruit(FruitType.GRAPE))
        assertEquals(DropTarget.RIGHT, config.getTargetForFruit(FruitType.STRAWBERRY))
        assertEquals(DropTarget.RIGHT, config.getTargetForFruit(FruitType.ORANGE))
    }

    @Test
    fun `classify HSV colors directly using real Yeetalk fruit samples`() {
        // Kiwi: yellow-green ~80° hue
        assertEquals(FruitType.KIWI_HIJAU, FruitDetector.classifyHsv(80f, 0.70f, 0.85f))

        // Peach: warm pastel salmon ~15° hue, S=0.33, V=0.98
        assertEquals(FruitType.PEACH, FruitDetector.classifyHsv(15f, 0.33f, 0.98f))

        // Blueberry: deep indigo/violet ~235° hue
        assertEquals(FruitType.BLUEBERRY, FruitDetector.classifyHsv(235f, 0.75f, 0.85f))

        // Grape: purple/magenta ~305° hue
        assertEquals(FruitType.GRAPE, FruitDetector.classifyHsv(305f, 0.65f, 0.74f))

        // Strawberry / Red Berry: crimson ~349° hue, S=0.56, V=0.98
        assertEquals(FruitType.STRAWBERRY, FruitDetector.classifyHsv(349f, 0.56f, 0.98f))

        // Orange: bright orange ~29° hue, S=0.89, V=0.99
        assertEquals(FruitType.ORANGE, FruitDetector.classifyHsv(29f, 0.89f, 0.99f))

        // Sky blue background should be ignored
        assertEquals(FruitType.UNKNOWN, FruitDetector.classifyHsv(201f, 0.55f, 0.98f))
    }

    @Test
    fun `detect kiwi from bitmap using pixel buffer`() {
        val width = 360
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val config = FruitDropConfig(scanRelX = 0.5f, scanRelY = 0.5f, scanRadiusDp = 13)
        val cx = (config.scanRelX * width).toInt()
        val cy = (config.scanRelY * height).toInt()

        val kiwiColor = Color.rgb(167, 217, 65)
        for (y in (cy - 10)..(cy + 10)) {
            for (x in (cx - 10)..(cx + 10)) {
                bitmap.setPixel(x, y, kiwiColor)
            }
        }

        val result = FruitDetector.detectFruit(bitmap, config)
        assertEquals(FruitType.KIWI_HIJAU, result.fruit)
        assertEquals(DropTarget.LEFT, result.target)
    }

    @Test
    fun `detect peach and strawberry accurately without confusion`() {
        val width = 360
        val height = 800

        // Peach bitmap (warm salmon RGB 255, 193, 168)
        val peachBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val config = FruitDropConfig(scanRelX = 0.5f, scanRelY = 0.5f, scanRadiusDp = 13)
        val cx = (config.scanRelX * width).toInt()
        val cy = (config.scanRelY * height).toInt()

        val peachColor = Color.rgb(255, 193, 168)
        for (y in (cy - 10)..(cy + 10)) {
            for (x in (cx - 10)..(cx + 10)) {
                peachBitmap.setPixel(x, y, peachColor)
            }
        }
        val peachResult = FruitDetector.detectFruit(peachBitmap, config)
        assertEquals(FruitType.PEACH, peachResult.fruit)
        assertEquals(DropTarget.LEFT, peachResult.target)

        // Red berry bitmap (crimson RGB 250, 110, 137)
        val berryBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val berryColor = Color.rgb(250, 110, 137)
        for (y in (cy - 10)..(cy + 10)) {
            for (x in (cx - 10)..(cx + 10)) {
                berryBitmap.setPixel(x, y, berryColor)
            }
        }
        val berryResult = FruitDetector.detectFruit(berryBitmap, config)
        assertEquals(FruitType.STRAWBERRY, berryResult.fruit)
        assertEquals(DropTarget.RIGHT, berryResult.target)
    }

    @Test
    fun `detect blueberry from bitmap using pixel buffer`() {
        val width = 360
        val height = 800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val config = FruitDropConfig(scanRelX = 0.5f, scanRelY = 0.5f, scanRadiusDp = 13)
        val cx = (config.scanRelX * width).toInt()
        val cy = (config.scanRelY * height).toInt()

        val blueberryColor = Color.rgb(65, 80, 220)
        for (y in (cy - 10)..(cy + 10)) {
            for (x in (cx - 10)..(cx + 10)) {
                bitmap.setPixel(x, y, blueberryColor)
            }
        }

        val result = FruitDetector.detectFruit(bitmap, config)
        assertEquals(FruitType.BLUEBERRY, result.fruit)
        assertEquals(DropTarget.RIGHT, result.target)
    }
}
