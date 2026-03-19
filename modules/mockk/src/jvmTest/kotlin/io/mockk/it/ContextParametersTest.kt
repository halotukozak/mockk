package io.mockk.it

import io.mockk.*
import io.mockk.forContext
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ContextParametersTest {

    // --- Test fixtures ---

    class Transaction(val id: String)

    class Logger(val prefix: String) {
        fun log(msg: String) = "$prefix: $msg"
    }

    interface Repository {
        context(tx: Transaction)
        fun save(entity: String): String

        context(tx: Transaction)
        fun findById(id: Int): String?

        context(tx: Transaction)
        fun delete(id: Int)
    }

    interface AsyncRepository {
        context(tx: Transaction)
        suspend fun save(entity: String): String

        context(tx: Transaction)
        suspend fun findById(id: Int): String?

        context(tx: Transaction)
        suspend fun delete(id: Int)
    }

    interface MultiContextService {
        context(tx: Transaction, log: Logger)
        fun process(data: String): String
    }

    // ===================================================================
    // every
    // ===================================================================

    @Test
    fun `every with returns`() {
        val repo = mockk<Repository>()

        forContext<Transaction>().every { repo.save(any()) } returns "saved"

        context(Transaction("tx-1")) {
            assertEquals("saved", repo.save("entity-1"))
        }
    }

    @Test
    fun `every with returnsMany`() {
        val repo = mockk<Repository>()

        forContext<Transaction>().every { repo.save(any()) } returnsMany listOf("first", "second", "third")

        context(Transaction("tx-1")) {
            assertEquals("first", repo.save("a"))
            assertEquals("second", repo.save("b"))
            assertEquals("third", repo.save("c"))
        }
    }

    @Test
    fun `every with throws`() {
        val repo = mockk<Repository>()

        forContext<Transaction>().every { repo.save(any()) } throws IllegalStateException("boom")

        context(Transaction("tx-1")) {
            assertFailsWith<IllegalStateException> {
                repo.save("entity-1")
            }
        }
    }

    @Test
    fun `every with answersWithContext`() {
        val repo = mockk<Repository>()

        forContext<Transaction>().every { repo.save(any()) } answersWithContext {
            "${contextArg<Transaction>().id}:${secondArg<String>()}"
        }

        context(Transaction("tx-42")) {
            assertEquals("tx-42:my-entity", repo.save("my-entity"))
        }
    }

    @Test
    fun `every with answers (untyped context via firstArg)`() {
        val repo = mockk<Repository>()

        forContext<Transaction>().every { repo.save(any()) } answers {
            val tx = firstArg<Transaction>()
            "answered-${tx.id}"
        }

        context(Transaction("tx-7")) {
            assertEquals("answered-tx-7", repo.save("entity"))
        }
    }

    // ===================================================================
    // coEvery
    // ===================================================================

    @Test
    fun `coEvery with returns`() {
        val repo = mockk<AsyncRepository>()

        forContext<Transaction>().coEvery { repo.save(any()) } returns "saved-async"

        runBlocking {
            context(Transaction("tx-1")) {
                assertEquals("saved-async", repo.save("entity-1"))
            }
        }
    }

    @Test
    fun `coEvery with coAnswersWithContext`() {
        val repo = mockk<AsyncRepository>()

        forContext<Transaction>().coEvery { repo.save(any()) } coAnswersWithContext {
            "async-${contextArg<Transaction>().id}"
        }

        runBlocking {
            context(Transaction("tx-99")) {
                assertEquals("async-tx-99", repo.save("entity"))
            }
        }
    }

    @Test
    fun `coEvery with throws`() {
        val repo = mockk<AsyncRepository>()

        forContext<Transaction>().coEvery { repo.save(any()) } throws IllegalArgumentException("async boom")

        runBlocking {
            context(Transaction("tx-1")) {
                assertFailsWith<IllegalArgumentException> {
                    repo.save("entity-1")
                }
            }
        }
    }

    // ===================================================================
    // justRun / coJustRun
    // ===================================================================

    @Test
    fun `justRun for Unit function`() {
        val repo = mockk<Repository>()

        forContext<Transaction>().justRun { repo.delete(any()) }

        context(Transaction("tx-1")) {
            repo.delete(42)
        }

        forContext<Transaction>().verify(exactly = 1) { repo.delete(42) }
    }

    @Test
    fun `coJustRun for suspend Unit function`() {
        val repo = mockk<AsyncRepository>()

        forContext<Transaction>().coJustRun { repo.delete(any()) }

        runBlocking {
            context(Transaction("tx-1")) {
                repo.delete(42)
            }
        }

        forContext<Transaction>().coVerify(exactly = 1) { repo.delete(42) }
    }

    // ===================================================================
    // coJustAwait
    // ===================================================================

    @Test
    fun `coJustAwait stubs never-returning function`() {
        val repo = mockk<AsyncRepository>()

        forContext<Transaction>().coJustAwait { repo.delete(any()) }

        forContext<Transaction>().coVerify(inverse = true) { repo.delete(any()) }
    }

    // ===================================================================
    // verify
    // ===================================================================

    @Test
    fun `verify with exact count`() {
        val repo = mockk<Repository>()

        forContext<Transaction>().every { repo.save(any()) } returns "saved"

        context(Transaction("tx-1")) {
            repo.save("entity-1")
            repo.save("entity-2")
        }

        forContext<Transaction>().verify(exactly = 2) { repo.save(any()) }
    }

    @Test
    fun `verify inverse`() {
        val repo = mockk<Repository>()

        forContext<Transaction>().every { repo.save(any()) } returns "saved"

        forContext<Transaction>().verify(inverse = true) { repo.save(any()) }
    }

    // ===================================================================
    // coVerify
    // ===================================================================

    @Test
    fun `coVerify with exact count`() {
        val repo = mockk<AsyncRepository>()

        forContext<Transaction>().coEvery { repo.save(any()) } returns "saved"

        runBlocking {
            context(Transaction("tx-1")) {
                repo.save("a")
                repo.save("b")
            }
        }

        forContext<Transaction>().coVerify(exactly = 2) { repo.save(any()) }
    }

    // ===================================================================
    // verifyAll / coVerifyAll
    // ===================================================================

    @Test
    fun `verifyAll checks all calls`() {
        val repo = mockk<Repository>()

        forContext<Transaction>().every { repo.save(any()) } returns "saved"

        context(Transaction("tx-1")) {
            repo.save("entity-1")
        }

        forContext<Transaction>().verifyAll { repo.save("entity-1") }
    }

    @Test
    fun `coVerifyAll checks all calls`() {
        val repo = mockk<AsyncRepository>()

        forContext<Transaction>().coEvery { repo.save(any()) } returns "saved"

        runBlocking {
            context(Transaction("tx-1")) {
                repo.save("entity-1")
            }
        }

        forContext<Transaction>().coVerifyAll { repo.save("entity-1") }
    }

    // ===================================================================
    // verifyOrder / coVerifyOrder
    // ===================================================================

    @Test
    fun `verifyOrder checks single call`() {
        val repo = mockk<Repository>()

        forContext<Transaction>().every { repo.save(any()) } returns "saved"

        context(Transaction("tx-1")) {
            repo.save("entity-1")
        }

        forContext<Transaction>().verifyOrder { repo.save("entity-1") }
    }

    @Test
    fun `coVerifyOrder checks single call`() {
        val repo = mockk<AsyncRepository>()

        forContext<Transaction>().coEvery { repo.save(any()) } returns "saved"

        runBlocking {
            context(Transaction("tx-1")) {
                repo.save("entity-1")
            }
        }

        forContext<Transaction>().coVerifyOrder { repo.save("entity-1") }
    }

    // ===================================================================
    // verifySequence / coVerifySequence
    // ===================================================================

    @Test
    fun `verifySequence checks single call`() {
        val repo = mockk<Repository>()

        forContext<Transaction>().every { repo.save(any()) } returns "saved"

        context(Transaction("tx-1")) {
            repo.save("a")
        }

        forContext<Transaction>().verifySequence { repo.save("a") }
    }

    @Test
    fun `coVerifySequence checks single call`() {
        val repo = mockk<AsyncRepository>()

        forContext<Transaction>().coEvery { repo.save(any()) } returns "saved"

        runBlocking {
            context(Transaction("tx-1")) {
                repo.save("a")
            }
        }

        forContext<Transaction>().coVerifySequence { repo.save("a") }
    }

    // ===================================================================
    // excludeRecords / coExcludeRecords
    // ===================================================================

    @Test
    fun `excludeRecords excludes from verification`() {
        val repo = mockk<Repository>()

        forContext<Transaction>().every { repo.save(any()) } returns "saved"
        forContext<Transaction>().every { repo.findById(any()) } returns "found"

        context(Transaction("tx-1")) {
            repo.save("entity-1")
            repo.findById(1)
        }

        forContext<Transaction>().excludeRecords { repo.findById(any()) }
        forContext<Transaction>().verify(exactly = 1) { repo.save("entity-1") }
    }

    @Test
    fun `coExcludeRecords excludes from verification`() {
        val repo = mockk<AsyncRepository>()

        forContext<Transaction>().coEvery { repo.save(any()) } returns "saved"
        forContext<Transaction>().coEvery { repo.findById(any()) } returns "found"

        runBlocking {
            context(Transaction("tx-1")) {
                repo.save("entity-1")
                repo.findById(1)
            }
        }

        forContext<Transaction>().coExcludeRecords { repo.findById(any()) }
        forContext<Transaction>().coVerify(exactly = 1) { repo.save("entity-1") }
    }

    // ===================================================================
    // Specific context values (uses with() for matchers)
    // ===================================================================

    @Test
    fun `every with specific context value`() {
        val repo = mockk<Repository>()
        val tx = Transaction("tx-1")

        every {
            with(tx) {
                repo.save("entity-1")
            }
        } returns "saved-1"

        context(tx) {
            assertEquals("saved-1", repo.save("entity-1"))
        }
    }

    @Test
    fun `every with different stubs for different contexts`() {
        val repo = mockk<Repository>()
        val tx1 = Transaction("tx-1")
        val tx2 = Transaction("tx-2")

        every {
            with(tx1) { repo.findById(1) }
        } returns "entity-from-tx1"

        every {
            with(tx2) { repo.findById(1) }
        } returns "entity-from-tx2"

        context(tx1) {
            assertEquals("entity-from-tx1", repo.findById(1))
        }
        context(tx2) {
            assertEquals("entity-from-tx2", repo.findById(1))
        }
    }

    @Test
    fun `verify with specific context value`() {
        val repo = mockk<Repository>()
        val tx1 = Transaction("tx-1")
        val tx2 = Transaction("tx-2")

        forContext<Transaction>().every { repo.save(any()) } returns "saved"

        context(tx1) { repo.save("a") }
        context(tx2) { repo.save("b") }

        verify(exactly = 1) {
            with(tx1) { repo.save("a") }
        }
        verify(exactly = 1) {
            with(tx2) { repo.save("b") }
        }
    }

    // ===================================================================
    // Slot / capture
    // ===================================================================

    @Test
    fun `capture context parameter with slot`() {
        val repo = mockk<Repository>()
        val txSlot = slot<Transaction>()

        every {
            with(capture(txSlot)) {
                repo.save(any())
            }
        } returns "saved"

        context(Transaction("tx-captured")) {
            repo.save("entity-1")
        }

        assertEquals("tx-captured", txSlot.captured.id)
    }

    // ===================================================================
    // Multiple context parameters
    // ===================================================================

    @Test
    fun `every with multiple context parameters`() {
        val service = mockk<MultiContextService>()

        every {
            with(any<Transaction>()) {
                with(any<Logger>()) {
                    service.process(any())
                }
            }
        } returns "processed"

        context(Transaction("tx-1"), Logger("test")) {
            assertEquals("processed", service.process("data"))
        }

        verify {
            with(any<Transaction>()) {
                with(any<Logger>()) {
                    service.process("data")
                }
            }
        }
    }
}
