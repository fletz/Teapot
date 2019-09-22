package com.factorymarket.rxelm.contract

import com.factorymarket.rxelm.cmd.Cmd
import com.factorymarket.rxelm.msg.Msg
import io.reactivex.Single

interface RxFeature<S : State> : Upd<S>, RxEffectHandler

interface RxEffectHandler  {
    fun call(cmd: Cmd): Single<Msg>
}