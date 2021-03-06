package dev.teapot.extensions.paging

import dev.teapot.program.ProgramBuilder

fun <T, FETCH_PARAMS> ProgramBuilder.buildRxPagingFeature(cmdHandlerPaging: RxPagingCommandsHandler<T, FETCH_PARAMS>,
                                                          namespace: String = "") : RxPagingFeature<T, FETCH_PARAMS> {
    return RxPagingFeature(cmdHandlerPaging, logger, namespace)
}

fun <T, FETCH_PARAMS> ProgramBuilder.buildCoPagingFeature(cmdHandlerPaging: CoPagingCommandsHandler<T, FETCH_PARAMS>,
                                                          namespace: String = "") : CoPagingFeature<T, FETCH_PARAMS> {
    return CoPagingFeature(cmdHandlerPaging, logger, namespace)
}