package io.codeherb.mycompose.fp

interface Monoid<A> {
    fun op(a1: A, a2: A): A
    val zero: A
}

fun <A> listMonoid() = object: Monoid<List<A>> {
    override fun op(a1: List<A>, a2: List<A>): List<A> = a1 + a2

    override val zero: List<A> = listOf()

}

fun <A> endoMonoid(): Monoid<(A) -> A> =
    object: Monoid<(A) -> A> {
        override fun op(a1: (A) -> A, a2: (A) -> A): (A) -> A = {
            a2(a1(it))
        }

        override val zero: (A) -> A = { it }

    }

interface Functor<out T> {
    fun <R> map(f: (T) -> R): Functor<R>
}

interface Monad<out T>: Functor<T>, Applicative<T> {
    fun <R> flatMap(f: (T) -> Monad<R>): Monad<R>

    override fun <R> map(f: (T) -> R): Monad<R>

    override fun <A> ap(f: Applicative<A>): Applicative<T>
}

interface Applicative<out T>: Functor<T> {

    fun <A> ap(f: Applicative<A>): Applicative<T>

    override fun <R> map(f: (T) -> R): Applicative<R>
}

