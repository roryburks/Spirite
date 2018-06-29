package sjunit.spirite.base.brains

import spirite.base.brains.Hotkey
import spirite.base.brains.HotkeyManager
import spirite.base.brains.KeyCommand
import spirite.base.brains.settings.JPreferences
import java.awt.event.KeyEvent
import kotlin.test.assertEquals
import org.junit.Test as test

class HotkeyManagerTests {
    val preferences = JPreferences(HotkeyManagerTests::class.java)
    val hotketManager = HotkeyManager(preferences)

    @test fun verifyDefaults() {
        assertEquals("view.zoomIn", hotketManager.getCommand(Hotkey(KeyEvent.VK_ADD, 0))?.commandString)
    }
    @test fun do1() {
        hotketManager.setCommand(Hotkey(KeyEvent.VK_A,0), KeyCommand("global.paste"))
        //        // Can see its entry in the Registry
    }

    // TODO: Mocked tests
}