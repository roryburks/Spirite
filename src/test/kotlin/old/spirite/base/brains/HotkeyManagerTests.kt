package old.spirite.base.brains

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import spirite.base.brains.Hotkey
import spirite.base.brains.HotkeyManager
import spirite.base.brains.KeyCommand
import spirite.base.brains.settings.JPreferences
import java.awt.event.KeyEvent
import kotlin.test.assertEquals

@Tag("Old")
class HotkeyManagerTests {
    val preferences = JPreferences(HotkeyManagerTests::class.java)
    val hotketManager = HotkeyManager(preferences)

    @Test fun verifyDefaults() {
        assertEquals("view.zoomIn", hotketManager.getCommand(Hotkey(KeyEvent.VK_ADD, 0))?.commandString)
    }
    @Test fun do1() {
        hotketManager.setCommand(Hotkey(KeyEvent.VK_A,0), KeyCommand("global.paste"))
        //        // Can see its entry in the Registry
    }

    // TODO: Mocked tests
}