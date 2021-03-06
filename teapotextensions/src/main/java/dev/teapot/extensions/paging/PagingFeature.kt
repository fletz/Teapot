package dev.teapot.extensions.paging

import dev.teapot.msg.Msg
import dev.teapot.cmd.BatchCmd
import dev.teapot.cmd.CancelCmd
import dev.teapot.cmd.Cmd
import dev.teapot.contract.PluggableFeature
import dev.teapot.contract.Update
import dev.teapot.log.TeapotLogger


abstract class PagingFeature<T, FETCH_PARAMS>(
        protected val errorLogger: TeapotLogger? = null,
        protected val namespace: String = ""
) : PluggableFeature<PagingState<T, FETCH_PARAMS>, FETCH_PARAMS> {


    override fun initialState(initialParams : FETCH_PARAMS): PagingState<T, FETCH_PARAMS> {
        return PagingState(fetchParams = initialParams)
    }

    override fun handlesMessage(msg: Msg): Boolean {
        return msg is PagingMsg && msg.namespace == namespace
    }

    override fun handlesCommands(cmd: Cmd): Boolean {
        return cmd is PagingCmd && cmd.namespace == namespace || (cmd is LogThrowableCmd && cmd.ns == namespace)
    }

    @Suppress("UnsafeCast", "UNCHECKED_CAST")
    override fun update(msg: Msg, state: PagingState<T, FETCH_PARAMS>): Update<PagingState<T, FETCH_PARAMS>> =
            when (msg) {
                is PagingStartMsg -> startPaging(state, state.fetchParams)

                is PagingStartWithParamsMsg<*, *> -> startPaging(state, msg.fetchParams as? FETCH_PARAMS)

                is PagingOnScrolledToEndMsg -> loadNextPage(state)

                is PagingOnLoadedItemsMsg<*> -> itemsLoaded(state, state.items + msg.items, msg.totalPages)

                is PagingOnRefreshedItemsMsg<*> -> itemsLoaded(state, msg.items, msg.totalPages, msg.totalCount)

                is PagingOnRetryListButtonClickMsg -> loadNextPage(state)
                is PagingOnRetryAfterFullscreenErrorButtonClickMsg ->
                    Update.update(
                            state.toFullscreenLoadingState().copy(isPageLoading = true), PagingRefreshItemsCmd(
                            state.fetchParams,
                            ns = namespace
                    )
                    )
                is PagingOnSwipeMsg -> Update.update(
                        state.toRefreshingState().copy(isPageLoading = true), BatchCmd(
                        //no matter what fetchParams we pass except namespace, since we've override hashcode() method
                        CancelCmd(PagingLoadItemsCmd(1, state.fetchParams, namespace)),
                        CancelCmd(PagingRefreshItemsCmd(state.fetchParams, namespace)),
                        PagingRefreshItemsCmd(state.fetchParams, ns = namespace)
                )
                )
                is PagingErrorMsg -> onErrorMsg(msg, state)
                else -> throw IllegalArgumentException("Unsupported message $msg")
            }

    private fun startPaging(state: PagingState<T, FETCH_PARAMS>, fetchParams: FETCH_PARAMS? = null):
            Update<PagingState<T, FETCH_PARAMS>> {

        return Update.update(
                state.toFullscreenLoadingState()
                        .copy(
                                isPageLoading = true,
                                isStarted = true,
                                fetchParams = fetchParams
                        ), BatchCmd(
                //no matter what fetchParams we pass except namespace, since we've override hashcode() method
                CancelCmd(PagingLoadItemsCmd(1, state.fetchParams, namespace)),
                CancelCmd(PagingRefreshItemsCmd(state.fetchParams, namespace)),
                PagingRefreshItemsCmd(fetchParams, ns = namespace)
        )
        )
    }

    @Suppress("UnsafeCast", "UNCHECKED_CAST")
    fun itemsLoaded(state: PagingState<T, FETCH_PARAMS>, items: List<Any?>, totalPages: Int, totalCount: Int? = null):
            Update<PagingState<T, FETCH_PARAMS>> {
        val newState = state.copy(
                items = items as List<T>,
                totalPages = totalPages,
                isPageLoading = false,
                totalCount = totalCount ?: state.totalCount
        )
        return Update.state(
                if (newState.hasLoadedAllItems()) {
                    newState.toCompletelyLoadedState()
                } else {
                    newState.toLoadingState()
                }
        )
    }

    private fun onErrorMsg(
            msg: PagingErrorMsg,
            state: PagingState<T, FETCH_PARAMS>
    ) = when (msg.cmd) {
        is PagingLoadItemsCmd<*> -> {
            Update.update(
                    if (state.items.isEmpty()) {
                        state.toErrorState()
                    } else {
                        state.toRetryState()
                    }.copy(nextPage = state.nextPage.dec()), LogThrowableCmd(msg.err, namespace)
            )
        }
        is PagingRefreshItemsCmd<*> -> Update.update(state.toErrorState(), LogThrowableCmd(msg.err, namespace))
        else -> throw IllegalArgumentException("Can't handle msg $msg")
    }

    private fun <T> loadNextPage(state: PagingState<T, FETCH_PARAMS>): Update<PagingState<T, FETCH_PARAMS>> {
        if (state.isPageLoading) {
            return Update.idle()
        }

        return Update.update(
                state.toLoadingState().copy(
                        nextPage = state.nextPage.inc(), isPageLoading = true

                ), PagingLoadItemsCmd(state.nextPage, state.fetchParams, ns = namespace)
        )
    }
}
