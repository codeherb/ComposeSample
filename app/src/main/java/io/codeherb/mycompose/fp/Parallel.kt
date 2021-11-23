package io.codeherb.mycompose.fp

import kotlinx.coroutines.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.internal.resumeCancellableWith
import kotlinx.coroutines.selects.SelectClause0
import kotlinx.coroutines.selects.SelectClause1
import java.util.concurrent.*
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

// 병렬 수행에 대한 추상화 데이터 타입 정의
// async, await 는 비동기를 위한게 아니라 비동기를
// 철차적으로 처리할 수 있도록 돕는 도구로
// 결국 비동기를 표현하기 위해선 별도의 데이터 타입이 사용된다.
//class Par<V>(val get: V)

// 7.4.4 관점의 전환으로 기본 Future 대신, Non Block Future 를 생성한다.
abstract class Future<A> {
    // 패키지 내에만 공개함으로서 API의 순수성을 유지하도록 한다.
    internal abstract fun invoke(cb: (A) -> Unit)
}

// 4. Par 의 representation
typealias Par<A> = (ExecutorService) -> Future<A>

typealias CoPar<A> = CoroutineScope.() -> Deferred<A>

object CoPars {
    fun <A> unit(a: A): CoPar<A> = { CompletableDeferred(a) }

    fun <A> fork(f: suspend () -> CoPar<A>): CoPar<A> = {
        println("@## forking!")
        async(start = CoroutineStart.LAZY) {
            println("@## do async!")
            f()(this).await()
        }
    }

    fun <A> lazyUnit(a: suspend () -> A): CoPar<A> = fork { unit(a()) }

    fun <A, B, C> map2(a: CoPar<A>, b: CoPar<B>, f: (A, B) -> C): CoPar<C> = {
        println("@## in Map2")
        val af = a(this)
        val bf = b(this)
        async(start = CoroutineStart.LAZY) {
            println("@## do async! on Map2")
            f(af.await(), bf.await())
        }.also {
            it.invokeOnCompletion { err ->
                err?.let {
                    af.cancel()
                    bf.cancel()
                }
            }
        }
    }

    fun <A, B> map(a: CoPar<A>, f: (A) -> B): CoPar<B> =
        map2(a, unit(Unit)) { a, _ -> f(a) }

    suspend fun <A> CoroutineScope.run(a: CoPar<A>): A =
        a(this).await()

}

object Pars {

    fun <A> unit(a: A): Par<A> = { es: ExecutorService -> UnitFuture(a) }

    fun <A> lazyUnit(a: () -> A): Par<A> = fork{ unit(a()) }

    data class UnitFuture<A>(val a: A) : Future<A> {

        override fun cancel(p0: Boolean): Boolean = false

        override fun isCancelled(): Boolean = false

        override fun isDone(): Boolean = true

        override fun get(): A = a

        override fun get(p0: Long, p1: TimeUnit?): A = a

    }

    fun <A, B, C> map2 (a: Par<A>, b: Par<B>, f: (A, B) -> C): Par<C> =
        { es: ExecutorService ->
            val af = a(es)
            val bf = b(es)
            UnitFuture(f(af.get(), bf.get()))
        }

    fun <A, B, C> map2OnTime (a: Par<A>, b: Par<B>, f: (A, B) -> C): Par<C> =
        { es: ExecutorService ->
            object: Future<C> {
                val af = a(es)
                val bf = b(es)

                override fun cancel(p0: Boolean): Boolean {
                    return af.cancel(p0) && bf.cancel(p0)
                }

                override fun isCancelled(): Boolean {
                    return af.isCancelled && bf.isCancelled
                }

                override fun isDone(): Boolean {
                    return af.isDone && bf.isDone
                }

                override fun get(): C {
                    return f(af.get(), bf.get())
                }

                override fun get(p0: Long, p1: TimeUnit?): C {
                    return f(af.get(p0, p1), bf.get(p0, p1))
                }
            }
        }

    // 고정 스레드 풀을 사용할 경우 교착 상태에 빠질 수 있다.
    fun <A> fork(a: () -> Par<A>): Par<A> =
        { es: ExecutorService ->
            es.submit<A>{ a()(es).get() }
        }

    // 고정 스레드의 문제를 해결하는 방법으로 이렇게 고칠 수 있으나
    // 논리 스레드를 띄우지 않으므로 우리가 원하는 동작은 아니지만...
    // 실행을 지연하는 유용한 코드이다.
    fun <A> delay(a: () -> Par<A>): Par<A> =
        { es: ExecutorService ->
            a()(es)
        }

    fun <A, B> asyncF(f: (A) -> B): (A) -> Par<B> =
        { a: A ->
            lazyUnit { f(a) }
        }

    fun <A, B> map(pa: Par<A>, f: (A) -> B): Par<B> =
        map2(pa, unit(Unit)) { a, _ -> f(a) }

    fun <A, B> parMap(ps: List<A>, f: (A) -> B): Par<List<B>> = {
        val fbs: List<Par<B>> = ps.map(asyncF(f))
        TODO()
    }

    fun <A> sequence(ps: List<Par<A>>): Par<List<A>> =
        ps.fold(unit(mutableListOf())) { acc, v ->
            map2(acc, v) { a, b -> a.plus(b) }
        }

    fun <A> equals(es: ExecutorService, p1: Par<A>, p2: Par<A>): Boolean {
        return p1(es).get() == p2(es).get()
    }

    fun some() {
        Executors.newFixedThreadPool(3)
        Executors.newScheduledThreadPool(2)

        fork { map2(unit(1), unit(2)) { a,b -> a + b} } == map2(unit(1), unit(2)) { a,b -> a + b}
    }
}



//fun <A> unit(a: A): Par<A> = Par(a)

// fork가 비동기 평가가 필요하다는 표기의 의미이며, Par는 나중에 해석될 병렬 계산의 서술이라고 볼수 있음
// get으로 실행 가능한 일급 프로그램이라는 의미로 run으로 이름을 바꾼다.
//fun <A> get(a: Par<A>): A = a.get
//fun <A> run(a: Par<A>): A = a.get
// 7.4.3 스레드 풀의 사이즈가 1개인 경우 교착상태에 빠지게 되는 구현
//fun <A> run(es: ExecutorService, a: Par<A>): Future<A> = a(es)


typealias Par2<A> = (ExecutorService) -> io.codeherb.mycompose.fp.Future<A>

object Par2s {
    fun <A> unit(a: A): Par2<A> = { es ->
        object: io.codeherb.mycompose.fp.Future<A>() {
            override fun invoke(cb: (A) -> Unit) {
                cb(a)
            }
        }
    }

    fun <A> fork(a: () -> Par2<A>): Par2<A> = { es ->
        object: io.codeherb.mycompose.fp.Future<A>() {
            override fun invoke(cb: (A) -> Unit) {
                eval(es) {
                    a()(es).invoke(cb)
                }
            }
        }
    }

    fun eval(es: ExecutorService, r: () -> Unit): Unit {
        es.submit { r() }
    }

//    fun <A, B, C> map2(pa: Par2<A>, pb: Par2<B>, f: (A, B) -> C): Par2<C> = { es ->
//        object: io.codeherb.mycompose.fp.Future<C>() {
//            override fun invoke(cb: (C) -> Unit) {
//                val ar = AtomicReference<Option<A>>(Option.None)
//                val br = AtomicReference<Option<B>>(Option.None)
//                val combiner = Actor()
//
//            }
//        }
//    }
}

fun <A> run(es: ExecutorService, p: Par2<A>): A {
    val ref = AtomicReference<A>()
    val latch = CountDownLatch(1)
    p(es).invoke { a ->
        ref.set(a)
        latch.countDown()
    }
    latch.await()
    return ref.get()
}

//fun <A> run2(es:ExecutorService, p: Par2<A>): A {
//    val ref = CompletableFuture<A>()
//    p(es).invoke {
//        ref.complete(it)
//    }
//    return ref.get()
//}



// 3. fork 를 이용하여 derived Combinator 를 만들었다. (다른 프리미티브 연산의 조합한 형태를 의미)
//    fork가 생김으로서 unit 을 통해 비동기 연산을 지연할 필요 없어짐
//fun <A> lazyUnit(a: () -> A): Par<A> =
//    fork { unit(a()) }

//fun <A> fork(a: () -> Par<A>): Par<A> = TODO()

fun sum(items: List<Int>) : Int =
    items.fold(0) { acc, a -> acc + a }

fun dncSum(ints: List<Int>) : Int {
    return if (ints.size <= 1)
        ints.getOrElse(0) { 0 }
    else {
        val (l, r) = ints.splitAt(ints.size / 2)
        dncSum(l) + dncSum(r)
    }
}

fun parSum(ints: List<Int>) : Par<Int> {
    return if (ints.size <= 1)
        Pars.unit(ints.getOrElse(0) { 0 })
    else {
        val (l, r) = ints.splitAt(ints.size / 2)
        // 1. get() 에 의한 부수효과를 제거하기 위해 호출을 하지 않아야함
        //    이러한 효과를 가지는 combinator 의 시그니쳐를 통해 map2 를 도입
        // map2(parSum(l), parSum(r)) { a: Int, b: Int -> a + b }
        // 2. 논리적 스레드 분기를 명시적으로 분기하기 위해 fork 도입
        //    fork를 도입함에 따라 map2를 엄격하게 만들 수 있다.
        //    (map2 자체는 실제 작업을 하지 않고 조합의 역할만 하므로...)
        Pars.map2(Pars.fork { parSum(l) }, Pars.fork { parSum(r) }) { a: Int, b: Int -> a + b }
        // 관심사 => 1. 조합 되어야함을 지정하는 수단, 2. 비동기 / 동기 여야 함을 서너택할 수단
        // fork는 인수들을 개별 논리적 스레드에서 평가되게 하는 수단
        // fork와 get의 구현에 어떤 정보가 필요한가 생각해보기
        // fork가 자신의 인수를 즉시 병렬로 평가한다면, 직간접적으로 스레드에 대해 알고 있어야함
        //  그렇게 될 경우 병렬성을 프로그래머가 제어할 수 있는 능력을 포기해야함
        //  필연적으로 전역적인 스레드 설정이 필요해짐
    }
}

fun <A> sortPar(parList: Par<List<Int>>): Par<List<Int>> =
//    Pars.map2(parList, Pars.unit(Unit)) { a, _ -> a.sorted() }
    Pars.map(parList) { it.sorted() }

fun <T> List<T>.splitAt(index: Int): Pair<List<T>, List<T>> =
    Pair(subList(0, index), subList(index, size))

