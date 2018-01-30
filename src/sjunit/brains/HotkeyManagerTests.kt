package sjunit.brains


import spirite.base.v2.brains.Hotkey
import spirite.base.v2.brains.HotkeyManager
import spirite.base.v2.brains.Settings.JPreferences
import java.awt.event.KeyEvent
import kotlin.test.assertEquals
import org.junit.Test as test

class HotkeyManagerTests {
    val preferences = JPreferences(HotkeyManagerTests::class.java)
    val hotketManager = HotkeyManager(preferences)

    @test fun verifyDefaults() {
        assertEquals("context.zoom_in", hotketManager.getCommand(Hotkey(KeyEvent.VK_ADD, 0)))
    }
    @test fun do1() {
        hotketManager.setCommand(Hotkey(KeyEvent.VK_A,0), "global.paste")
        // Can see its entry in the Registry
    }

    // TODO: Mocked tests
}