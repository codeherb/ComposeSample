package io.codeherb.mycompose.fp

fun <A, B, C> ((a: A, b: B) -> C).curried(): (A) -> (B) -> C =
    { a: A ->
        { b: B -> this(a, b) }
    }

infix fun <A, B, C> ((B) -> C).compose(f: (A) -> B) : (A) -> C =
    { a -> this(f(a)) }

fun <A, B> foldMap(list: List<A>, m: Monoid<B>, f: (A) -> B): B =
    list.fold(m.zero) { acc, a -> m.op(acc, f(a)) }

fun <R, A> foldLeft(list: List<A>, initial: R, f: (R, A) -> R): R {
    // (a -> (a -> (a -> (a -> a))))(initial)
    return foldMap(list, endoMonoid<R>()) { a ->
        { acc -> f(acc, a) }
    }(initial)
}


sealed class Option<out T> {
    object None: Option<Nothing>()
    data class Some<A>(val get: A): Option<A>()
}

sealed class FList<out T> {
    object None: FList<Nothing>()
    data class Cons<T>(val head: T, val tail: FList<T> = None): FList<T>()
}