@file:JvmName("ContextParametersKt")

package io.mockk


/**
 * Convenience extensions for mocking functions that use Kotlin context parameters.
 *
 * Instead of writing:
 * ```
 * every { with(any<MyContext>()) { mock.doSomething(any()) } } returns "result"
 * ```
 * you can write:
 * ```
 * forContext<MyContext>().every { mock.doSomething(any()) } returns "result"
 * ```
 *
 * Requires Kotlin 2.2+ with `-Xcontext-parameters`.
 */

/**
 * Scope that captures the context parameter type [C] once, so you don't have to
 * repeat it (or specify the return type) on every call.
 *
 * Usage:
 * ```
 * val ctx = forContext<Transaction>()
 * ctx.every { repo.save(any()) } returns "saved"
 * ctx.verify(exactly = 1) { repo.save("entity-1") }
 * ```
 *
 * Or inline:
 * ```
 * forContext<Transaction>().every { repo.save(any()) } returns "saved"
 * ```
 */
class MockKContextScope<C : Any> @PublishedApi internal constructor(
  @PublishedApi internal val anyMatcher: MockKMatcherScope.() -> C,
)

/**
 * Creates a [MockKContextScope] for the given context parameter type [C].
 * All stubbing/verification methods on the returned scope will automatically
 * provide an [any] matcher for the context parameter.
 */
inline fun <reified C : Any> forContext(): MockKContextScope<C> =
  MockKContextScope { any<C>() }

fun <C : Any, T> MockKContextScope<C>.every(
  block: context(C) MockKMatcherScope.() -> T,
): MockKContextStubScope<T, T, C> =
  MockKContextStubScope(io.mockk.every { context(anyMatcher()) { block() } })

fun <C : Any, T> MockKContextScope<C>.coEvery(
  block: suspend context(C) MockKMatcherScope.() -> T,
): MockKContextStubScope<T, T, C> =
  MockKContextStubScope(io.mockk.coEvery { context(anyMatcher()) { block() } })

fun <C : Any> MockKContextScope<C>.justRun(
  block: context(C) MockKMatcherScope.() -> Unit,
) {
  every(block) just Runs
}

fun <C : Any> MockKContextScope<C>.coJustRun(
  block: suspend context(C) MockKMatcherScope.() -> Unit,
) {
  coEvery(block) just Runs
}

fun <C : Any> MockKContextScope<C>.coJustAwait(
  block: suspend context(C) MockKMatcherScope.() -> Unit,
) {
  coEvery(block) just Awaits
}

fun <C : Any> MockKContextScope<C>.verify(
  ordering: Ordering = Ordering.UNORDERED,
  inverse: Boolean = false,
  atLeast: Int = 1,
  atMost: Int = Int.MAX_VALUE,
  exactly: Int = -1,
  timeout: Long = 0,
  verifyBlock: context(C) MockKVerificationScope.() -> Unit,
) = io.mockk.verify(ordering, inverse, atLeast, atMost, exactly, timeout) {
  context(anyMatcher()) { verifyBlock() }
}

fun <C : Any> MockKContextScope<C>.coVerify(
  ordering: Ordering = Ordering.UNORDERED,
  inverse: Boolean = false,
  atLeast: Int = 1,
  atMost: Int = Int.MAX_VALUE,
  exactly: Int = -1,
  timeout: Long = 0,
  verifyBlock: suspend context(C) MockKVerificationScope.() -> Unit,
) = io.mockk.coVerify(ordering, inverse, atLeast, atMost, exactly, timeout) {
  context(anyMatcher()) { verifyBlock() }
}

fun <C : Any> MockKContextScope<C>.verifyAll(
  inverse: Boolean = false,
  verifyBlock: context(C) MockKVerificationScope.() -> Unit,
) = io.mockk.verifyAll(inverse) {
  context(anyMatcher()) { verifyBlock() }
}

fun <C : Any> MockKContextScope<C>.coVerifyAll(
  inverse: Boolean = false,
  verifyBlock: suspend context(C) MockKVerificationScope.() -> Unit,
) = io.mockk.coVerifyAll(inverse) {
  context(anyMatcher()) { verifyBlock() }
}

fun <C : Any> MockKContextScope<C>.verifyOrder(
  inverse: Boolean = false,
  verifyBlock: context(C) MockKVerificationScope.() -> Unit,
) = io.mockk.verifyOrder(inverse) {
  context(anyMatcher()) { verifyBlock() }
}

fun <C : Any> MockKContextScope<C>.coVerifyOrder(
  inverse: Boolean = false,
  verifyBlock: suspend context(C) MockKVerificationScope.() -> Unit,
) = io.mockk.coVerifyOrder(inverse) {
  context(anyMatcher()) { verifyBlock() }
}

fun <C : Any> MockKContextScope<C>.verifySequence(
  inverse: Boolean = false,
  verifyBlock: context(C) MockKVerificationScope.() -> Unit,
) = io.mockk.verifySequence(inverse) {
  context(anyMatcher()) { verifyBlock() }
}

fun <C : Any> MockKContextScope<C>.coVerifySequence(
  inverse: Boolean = false,
  verifyBlock: suspend context(C) MockKVerificationScope.() -> Unit,
) = io.mockk.coVerifySequence(inverse) {
  context(anyMatcher()) { verifyBlock() }
}

fun <C : Any> MockKContextScope<C>.excludeRecords(
  current: Boolean = true,
  excludeBlock: context(C) MockKMatcherScope.() -> Unit,
) = io.mockk.excludeRecords(current) {
  context(anyMatcher()) { excludeBlock() }
}

fun <C : Any> MockKContextScope<C>.coExcludeRecords(
  current: Boolean = true,
  excludeBlock: suspend context(C) MockKMatcherScope.() -> Unit,
) = io.mockk.coExcludeRecords(current) {
  context(anyMatcher()) { excludeBlock() }
}

/**
 * Answers with the context parameter [C] available both as a Kotlin context parameter
 * and as the `contextArg` property on the receiver.
 *
 * Example:
 * ```
 * forContext<Transaction>().every {
 *     repo.save(any())
 * } answersWithContext {
 *     "${contextArg.id}:${secondArg<String>()}"
 * }
 * ```
 */
inline infix fun <T, B, reified C> MockKContextStubScope<T, B, C>.answersWithContext(
  crossinline answer: context(C) MockKAnswerScope<T, B>.() -> T,
) = answers { context(firstArg<C>()) { answer() } }

/**
 * Suspend variant of [answersWithContext].
 */
inline infix fun <T, B, reified C> MockKContextStubScope<T, B, C>.coAnswersWithContext(
  crossinline answer: suspend context(C) MockKAnswerScope<T, B>.() -> T,
) = coAnswers { context(firstArg<C>()) { answer() } }

/**
 * Retrieves the context argument from the current call.
 * Equivalent to `firstArg<C>()` but with a more descriptive name.
 */
inline fun <reified C> MockKAnswerScope<*, *>.contextArg(): C = firstArg<C>()
