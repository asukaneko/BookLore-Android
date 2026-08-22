package app.booklore.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.booklore.android.core.database.BookLoreDatabase
import app.booklore.android.core.cache.CacheManager
import app.booklore.android.core.network.BookLoreApi
import app.booklore.android.core.offline.OfflineBookStore
import app.booklore.android.core.security.SessionStore
import app.booklore.android.feature.AppViewModel
import app.booklore.android.sync.PendingSyncQueue
import app.booklore.android.ui.BookLoreApp
import app.booklore.android.ui.theme.BookLoreTheme

class MainActivity : ComponentActivity() {
    private val sessionStore by lazy { SessionStore(applicationContext) }
    private val appViewModel by viewModels<AppViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = BookLoreDatabase.get(applicationContext)
                return AppViewModel(
                    api = BookLoreApi(sessionStore),
                    sessionStore = sessionStore,
                    database = database,
                    pendingSyncQueue = PendingSyncQueue(applicationContext, database),
                    cacheManager = CacheManager(applicationContext, database),
                    offlineBookStore = OfflineBookStore(applicationContext),
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by appViewModel.state.collectAsStateWithLifecycle()
            BookLoreTheme(displaySettings = state.displaySettings) {
                BookLoreApp(viewModel = appViewModel)
            }
        }
    }
}
