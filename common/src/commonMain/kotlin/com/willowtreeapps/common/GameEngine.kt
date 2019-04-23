package com.willowtreeapps.common

import com.beyondeye.reduks.SimpleStore
import com.beyondeye.reduks.middlewares.applyMiddleware
import com.beyondeye.reduks.middlewares.thunkMiddleware
import com.willowtreeapps.common.middleware.*
import com.willowtreeapps.common.middleware.NavigationMiddleware
import com.willowtreeapps.common.middleware.SettingsMiddleware
import com.willowtreeapps.common.middleware.ViewEffectsMiddleware
import com.willowtreeapps.common.repo.LocalStorageSettingsRepository
import com.willowtreeapps.common.repo.userSettings
import com.willowtreeapps.common.ui.Presenter
import com.willowtreeapps.common.ui.PresenterFactory
import com.willowtreeapps.common.ui.View
import com.willowtreeapps.common.util.VibrateUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class GameEngine(navigator: Navigator,
                 application: Any = Any(),
                 networkContext: CoroutineContext,
                 val uiContext: CoroutineContext) {
    private val navigationMiddleware = NavigationMiddleware(navigator)
    private val viewEffectsMiddleware = ViewEffectsMiddleware()
    private val presenterFactory by lazy { PresenterFactory(this, networkContext) }
    val vibrateUtil = VibrateUtil(application)
    private val settingsMiddleware by lazy { SettingsMiddleware(LocalStorageSettingsRepository(userSettings(application)), networkContext) }

    val appStore by lazy {
        SimpleStore(AppState.INITIAL_STATE, ::reducer)
                .applyMiddleware(::thunkMiddleware,
                        viewEffectsMiddleware::dispatch,
                        navigationMiddleware::dispatch,
                        settingsMiddleware::dispatch,
                        ::loggerMiddleware)
    }

    init {
        appStore.dispatch(Actions.LoadAllSettingsAction())
    }

    fun dispatch(action: Any) {
        CoroutineScope(uiContext).launch {
            appStore.dispatch(action)
        }
    }

    val state: AppState
        get() = appStore.state

    fun <T : Presenter<*>> attachView(view: View<T>) = presenterFactory.attachView(view as View<Presenter<*>>)

    fun detachView(view: View<*>) = presenterFactory.detachView(view)
}
