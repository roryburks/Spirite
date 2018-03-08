package sjunit.spirite.base.brains


import spirite.base.brains.Hotkey
import spirite.base.brains.HotkeyManager
import spirite.base.brains.Settings.JPreferences
import java.awt.event.KeyEvent
import kotlin.test.assertEquals
import org.junit.Test as test

class HotkeyManagerTests {
    val preferences = JPreferences(HotkeyManagerTests::class.java)
    val hotketManager = HotkeyManager(preferences)

    @test fun verifyDefaults() {
        assertEquals("workspace.zoom_in", hotketManager.getCommand(Hotkey(KeyEvent.VK_ADD, 0)))
    }
    @test fun do1() {
        hotketManager.setCommand(Hotkey(KeyEvent.VK_A,0), "global.paste")
        // Can see its entry in the Registry
    }

    // TODO: Mocked tests
}