package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.example.data.AudioCoverArtHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  @Test
  fun albumArtUriIsBuiltCorrectly() {
    val uri = AudioCoverArtHelper.getAlbumArtUriForAlbumId(12345L)
    assertTrue(uri.contains("content://media/external/audio/albumart/12345"))
    assertTrue(uri.isNotBlank())
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AuraSound DSP", appName)
  }
}
