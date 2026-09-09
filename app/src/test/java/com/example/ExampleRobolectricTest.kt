package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.LudoColor
import com.example.engine.LudoBoardCoordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Rai Ludo King", appName)
  }

  @Test
  fun `verify board coordinates mapping`() {
    // Red start position at step 0
    val redStart = LudoBoardCoordinates.getTokenGridCoord(LudoColor.RED, 0, 0)
    assertEquals(6f, redStart.row, 0.01f)
    assertEquals(1f, redStart.col, 0.01f)

    // Red home position at step 56
    val redHome = LudoBoardCoordinates.getTokenGridCoord(LudoColor.RED, 0, 56)
    assertEquals(7f, redHome.col, 1.0f)

    // Verify safe cells
    assertTrue(LudoBoardCoordinates.isSafeCell(0))
    assertTrue(LudoBoardCoordinates.isSafeCell(8))
    assertTrue(LudoBoardCoordinates.isSafeCell(13))
  }
}
