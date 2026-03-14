package org.dgeek.sumgrid.analytics

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashlyticsInitializerTest {

    @Test
    fun `CrashlyticsInitializer object exists`() {
        assertNotNull(CrashlyticsInitializer)
    }

    @Test
    fun `CrashlyticsInitializer has initialize method via Java reflection`() {
        val methods = CrashlyticsInitializer::class.java.declaredMethods
        val initMethod = methods.find { it.name == "initialize" }
        assertNotNull("Expected CrashlyticsInitializer to have an initialize() method", initMethod)
    }

    @Test
    fun `CrashlyticsInitializer initialize method accepts one parameter`() {
        val methods = CrashlyticsInitializer::class.java.declaredMethods
        val initMethod = methods.find { it.name == "initialize" }
        assertNotNull(initMethod)
        assertTrue(
            "Expected initialize() to have exactly one parameter (Context)",
            initMethod!!.parameterCount == 1
        )
    }

    @Test
    fun `CrashlyticsInitializer initialize parameter type is android Context`() {
        val methods = CrashlyticsInitializer::class.java.declaredMethods
        val initMethod = methods.find { it.name == "initialize" }
        assertNotNull(initMethod)
        val paramType = initMethod!!.parameterTypes[0]
        assertTrue(
            "Expected parameter type to be android.content.Context, got ${paramType.name}",
            paramType.name == "android.content.Context"
        )
    }
}
