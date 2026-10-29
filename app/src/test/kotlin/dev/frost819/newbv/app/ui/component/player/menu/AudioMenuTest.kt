package dev.frost819.newbv.app.ui.component.player.menu

import com.google.common.truth.Truth.assertThat
import dev.frost819.newbv.data.datastore.Audio
import org.junit.jupiter.api.Test

/**
 * [Audio.detailedDisplayName] 的单元测试。
 */
class AudioMenuTest {
    @Test
    fun `detailedDisplayName returns expected Chinese and tech descriptions`() {
        assertThat(Audio.ADolbyAtoms.detailedDisplayName).isEqualTo("杜比全景声 (Dolby Atmos)")
        assertThat(Audio.AHiRes.detailedDisplayName).isEqualTo("Hi-Res 无损音质")
        assertThat(Audio.A192K.detailedDisplayName).isEqualTo("高品质 192K")
        assertThat(Audio.A132K.detailedDisplayName).isEqualTo("标准 132K")
        assertThat(Audio.A64K.detailedDisplayName).isEqualTo("流畅 64K")
    }

    @Test
    fun `all Audio entries have detailed display name`() {
        Audio.entries.forEach {
            assertThat(it.detailedDisplayName).isNotEmpty()
        }
    }
}
