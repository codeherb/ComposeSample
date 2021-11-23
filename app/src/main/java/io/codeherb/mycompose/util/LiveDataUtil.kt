package io.codeherb.mycompose.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

fun <A, S> LiveData<A>.runningFold(initState: S, f: (S, A) -> S): LiveData<S> =
    MediatorLiveData<S>().apply {
        var acc = initState
        addSource(this@runningFold) { item ->
            value = f(acc, item).also {
                acc = it
            }
        }
    }