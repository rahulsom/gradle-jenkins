package com.github.rahulsom.jenkins.dependencies

import groovy.lang.Closure
import org.gradle.internal.Cast

/**
 * Adapts a Kotlin function to a single argument Groovy [Closure].
 *
 * @param T the expected type of the single argument to the closure.
 * @param action the function to be adapted.
 *
 * @see KotlinClosure
 */
fun <T : Any> Any.closureOf(action: T.() -> Unit): Closure<Any?> =
    KotlinClosure(action, this, this)

fun <T> Any.delegateClosureOf(action: T.() -> Unit) =
    object : Closure<Unit>(this, this) {
        @Suppress("unused") // to be called dynamically by Groovy
        fun doCall() = Cast.uncheckedCast<T>(delegate).action()
    }


class KotlinClosure<in T : Any, V : Any>(
    val function: T.() -> V?,
    owner: Any? = null,
    thisObject: Any? = null) : Closure<V?>(owner, thisObject) {

    @Suppress("unused") // to be called dynamically by Groovy
    fun doCall(it: T): V? =
        it.function()
}
