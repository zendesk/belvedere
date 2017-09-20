package zendesk.belvedere

import android.content.Intent
import android.net.Uri
import org.mockito.Mockito
import java.util.*

interface TestHelper {

    fun mediaIntent(target: Int, intent: Intent? = null): MediaIntent {
        return MediaIntent(1, intent, "", true, target)
    }

    fun mediaResult(uri: Uri = Mockito.mock(Uri::class.java), name: String = UUID.randomUUID().toString(), size: Long = 1): MediaResult {
        return MediaResult(null, uri, uri, name, "", size, 1, 1)
    }

}