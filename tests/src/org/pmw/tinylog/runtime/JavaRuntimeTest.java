/*
 * Copyright 2016 Martin Winandy
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.pmw.tinylog.runtime;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.pmw.tinylog.hamcrest.StringMatchers.matchesPattern;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import org.junit.Test;
import org.pmw.tinylog.AbstractTest;

import mockit.Mock;
import mockit.MockUp;
import mockit.NonStrictExpectations;

/**
 * Tests for Sun's and Oracle's Java runtime implementation.
 *
 * @see JavaRuntime
 */
public class JavaRuntimeTest extends AbstractTest {

	/**
	 * Test getting the current process ID.
	 */
	@Test
	public final void testCurrentProcessId() {
		String pid = new JavaRuntime().getProcessId();
		assertThat(pid, instanceOf(String.class));
		assertThat(pid, matchesPattern("\\d+"));
		assertThat(ManagementFactory.getRuntimeMXBean().getName(), startsWith(pid));
	}

	/**
	 * Test use case if {@link RuntimeMXBean#getName()} returns only the process ID.
	 */
	@Test
	public final void testProcessIdWithoutHost() {
		new NonStrictExpectations(ManagementFactory.getRuntimeMXBean()) {
			{
				ManagementFactory.getRuntimeMXBean().getName();
				returns("1234");
			}
		};

		assertEquals("1234", new JavaRuntime().getProcessId());
	}

	/**
	 * Test use case if {@link RuntimeMXBean#getName()} returns process ID plus host name.
	 */
	@Test
	public final void testProcessIdWithHost() {
		new NonStrictExpectations(ManagementFactory.getRuntimeMXBean()) {
			{
				ManagementFactory.getRuntimeMXBean().getName();
				returns("5678@localhost");
			}
		};

		assertEquals("5678", new JavaRuntime().getProcessId());
	}

	/**
	 * Tests getting a class name from stack trace.
	 */
	@Test
	public final void testGettingClassName() {
		String name = new JavaRuntime().getClassName(1);
		assertEquals(JavaRuntimeTest.class.getName(), name);
	}

	/**
	 * Tests getting a stack trace element from StackTrace.
	 */
	@Test
	public final void testGettingStackTraceElement() {
		StackTraceElement stackTraceElement = new JavaRuntime().getStackTraceElement(1);
		assertNotNull(stackTraceElement);
		assertEquals(JavaRuntimeTest.class.getName(), stackTraceElement.getClassName());
		assertEquals("testGettingStackTraceElement", stackTraceElement.getMethodName());
	}

	/**
	 * Test getting the right class name of caller even if sun.reflect.Reflection is not supported.
	 */
	@SuppressWarnings("restriction")
	@Test
	public final void testWithoutSupportingSunReflection() {
		new MockUp<sun.reflect.Reflection>() {
			@Mock(invocations = 1)
			public Class<?> getCallerClass(final int index) {
				throw new UnsupportedOperationException();
			}
		};

		String name = new JavaRuntime().getClassName(1);
		assertEquals(JavaRuntimeTest.class.getName(), name);
	}

	/**
	 * Test getting the right stack trace element even if single stack trace element extracting is not supported.
	 */
	@Test
	public final void testWithoutSupportingSingleStackTraceElementExtracting() {
		MockUp<Throwable> mock = new MockUp<Throwable>() {
			@Mock
			public StackTraceElement getStackTraceElement(final int index) {
				throw new UnsupportedOperationException();
			}
		};

		JavaRuntime runtime = new JavaRuntime();

		mock.tearDown();

		StackTraceElement stackTraceElement = runtime.getStackTraceElement(1);
		assertNotNull(stackTraceElement);
		assertEquals(JavaRuntimeTest.class.getName(), stackTraceElement.getClassName());
	}

	/**
	 * Test getting the right class name of caller even if calling of sun.reflect.Reflection will fail.
	 */
	@SuppressWarnings("restriction")
	@Test
	public final void testErrorWhileCallingSunReflection() {
		JavaRuntime runtime = new JavaRuntime();

		new MockUp<sun.reflect.Reflection>() {
			@Mock(invocations = 1)
			public Class<?> getCallerClass(final int index) {
				try {
					throw new UnsupportedOperationException();
				} finally {
					tearDown();
				}
			}
		};

		String name = runtime.getClassName(1);
		assertEquals(JavaRuntimeTest.class.getName(), name);
		assertThat(getErrorStream().nextLine(), matchesPattern("LOGGER WARNING\\: Failed to get caller class from sun.reflect.Reflection \\(.+\\)"));
	}

	/**
	 * Test getting the right stack trace element even if getting single stack trace element will fail.
	 */
	@Test
	public final void testErrorWhileGettingSingleStackTraceElement() {
		JavaRuntime runtime = new JavaRuntime();

		new MockUp<Throwable>() {
			@Mock(invocations = 1)
			public StackTraceElement getStackTraceElement(final int index) {
				try {
					throw new UnsupportedOperationException();
				} finally {
					tearDown();
				}
			}
		};

		StackTraceElement stackTraceElement = runtime.getStackTraceElement(1);
		assertNotNull(stackTraceElement);
		assertEquals(JavaRuntimeTest.class.getName(), stackTraceElement.getClassName());
		assertEquals("testErrorWhileGettingSingleStackTraceElement", stackTraceElement.getMethodName());
		assertThat(getErrorStream().nextLine(), matchesPattern("LOGGER WARNING\\: Failed to get single stack trace element from throwable \\(.+\\)"));
	}

}
