package com.neomaverick.stride

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.neomaverick.stride.ui.StrideRoot
import com.neomaverick.stride.ui.StrideViewModel
import com.neomaverick.stride.ui.Tab
import com.neomaverick.stride.ui.theme.StrideTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders every main screen with the demo data; the PNGs are the README screenshots. Run: ./gradlew recordRoborazziDebug */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xxhdpi", application = StrideApp::class)
class ScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @org.junit.After fun freshDb() = com.neomaverick.stride.data.StrideDb.resetForTests()

    private fun shoot(tab: Tab, unlock: Boolean = false) {
        val app = ApplicationProvider.getApplicationContext<StrideApp>()
        kotlinx.coroutines.runBlocking { app.repo.loadDemo() }
        val vm = StrideViewModel(app)
        vm.premium.demoUnlock(unlock)
        compose.setContent { StrideTheme { StrideRoot(vm, tab) } }
        // Room delivers on its own threads; pump the main looper in real time until the screen has its data.
        val deadline = System.currentTimeMillis() + 30_000
        while (!(vm.state.value.rows.isNotEmpty() && vm.report.value != null) && System.currentTimeMillis() < deadline) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper(); Thread.sleep(50)
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("../docs/screenshots/${tab.route}.png")
    }

    @Test fun today() = shoot(Tab.TODAY)
    @Test fun habits() = shoot(Tab.HABITS)
    @Test fun workouts() = shoot(Tab.MOVE)
    @Test fun coach() = shoot(Tab.COACH, unlock = true)
    @Test fun you() = shoot(Tab.YOU)
}
