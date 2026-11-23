package com.example.homework2.ui.state

import androidx.annotation.StringRes
import com.example.homework2.R

enum class EmptyScene(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @StringRes val actionRes: Int
) {
    HOME(R.string.empty_home_title, R.string.empty_home_subtitle, R.string.empty_home_action),
    SEARCH(R.string.empty_search_title, R.string.empty_search_subtitle, R.string.empty_search_action),
    MY_POSTS(R.string.empty_my_posts_title, R.string.empty_my_posts_subtitle, R.string.empty_my_posts_action),
    FAVORITES(R.string.empty_favorites_title, R.string.empty_favorites_subtitle, R.string.empty_favorites_action),
    GROUPS(R.string.empty_groups_title, R.string.empty_groups_subtitle, R.string.empty_groups_action),
    MESSAGES(R.string.empty_messages_title, R.string.empty_messages_subtitle, R.string.empty_messages_action)
}

enum class ErrorScene(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @StringRes val actionRes: Int
) {
    NETWORK(R.string.error_network_title, R.string.error_network_subtitle, R.string.error_network_action),
    GENERIC(R.string.error_generic_title, R.string.error_generic_subtitle, R.string.error_generic_action),
    AI(R.string.error_ai_title, R.string.error_ai_subtitle, R.string.error_ai_action)
}

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Empty(val scene: EmptyScene) : UiState<Nothing>()
    data class Error(val scene: ErrorScene) : UiState<Nothing>()
}
