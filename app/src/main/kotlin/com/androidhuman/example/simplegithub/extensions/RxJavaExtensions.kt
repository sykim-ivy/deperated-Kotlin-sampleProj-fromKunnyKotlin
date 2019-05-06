package com.androidhuman.example.simplegithub.extensions

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

// CompositeDisposable의 '+='연산자 뒤에 Disposable 타입이 오는 경우를 재정의 합니다.
operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
    // CompositeDiaposable.add() 함수를 호출합니다.
    this.add(disposable)
}