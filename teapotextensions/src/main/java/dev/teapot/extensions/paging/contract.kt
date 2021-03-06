package dev.teapot.extensions.paging

data class PagingResult<T>(val items: List<T>, val totalPages: Int, val totalCount: Int? = null)

interface PagingView  {
    fun setRefreshingVisible(isVisible: Boolean)
    fun setRefreshingEnabled(isEnabled: Boolean)
    fun setErrorStateVisible(isVisible: Boolean)
    fun setFullScreenSpinnerVisible(isVisible: Boolean)
    fun setListVisible(isVisible: Boolean)
}