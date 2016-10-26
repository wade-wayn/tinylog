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

package org.tinylog.provider;

import org.junit.Test;
import org.tinylog.Level;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link WrappedLoggingProvider}.
 */
public final class WrapperLoggingProviderTest {

	private LoggingProvider first;
	private LoggingProvider second;
	private LoggingProvider wrapped;

	/**
	 * Verifies that {@code getMinimumLevel()} method returns the minimum severity level of underlying logging
	 * providers, if all have the same minimum severity level.
	 */
	@Test
	public void getSameMinimumLevel() {
		init(Level.TRACE, Level.TRACE);
		assertThat(wrapped.getMinimumLevel()).isEqualTo(Level.TRACE);
	}

	/**
	 * Verifies that {@code getMinimumLevel()} method returns the lowest minimum severity level of underlying logging
	 * providers, if there are different minimum severity levels.
	 */
	@Test
	public void getDifferentMinimumLevel() {
		init(Level.DEBUG, Level.WARN);
		assertThat(wrapped.getMinimumLevel()).isEqualTo(Level.DEBUG);
	}

	/**
	 * Verifies that {@code isEnabled()} method evaluates the severity level from underlying logging providers and
	 * returns {@code true} if given severity level is enabled at least for one of the underlying logging providers.
	 */
	@Test
	public void isEnabled() {
		init(Level.TRACE, Level.TRACE);

		when(first.isEnabled(anyInt(), eq(Level.TRACE))).thenReturn(false);
		when(first.isEnabled(anyInt(), eq(Level.DEBUG))).thenReturn(false);
		when(first.isEnabled(anyInt(), eq(Level.INFO))).thenReturn(false);
		when(first.isEnabled(anyInt(), eq(Level.WARN))).thenReturn(true);
		when(first.isEnabled(anyInt(), eq(Level.ERROR))).thenReturn(true);

		when(second.isEnabled(anyInt(), eq(Level.TRACE))).thenReturn(false);
		when(second.isEnabled(anyInt(), eq(Level.DEBUG))).thenReturn(true);
		when(second.isEnabled(anyInt(), eq(Level.INFO))).thenReturn(true);
		when(second.isEnabled(anyInt(), eq(Level.WARN))).thenReturn(true);
		when(second.isEnabled(anyInt(), eq(Level.ERROR))).thenReturn(true);

		assertThat(wrapped.isEnabled(1, Level.TRACE)).isEqualTo(false);
		assertThat(wrapped.isEnabled(1, Level.DEBUG)).isEqualTo(true);
		assertThat(wrapped.isEnabled(1, Level.INFO)).isEqualTo(true);
		assertThat(wrapped.isEnabled(1, Level.WARN)).isEqualTo(true);
		assertThat(wrapped.isEnabled(1, Level.ERROR)).isEqualTo(true);

		verify(first, atLeastOnce()).isEnabled(eq(2), any());
		verify(second, atLeastOnce()).isEnabled(eq(2), any());
	}

	/**
	 * Verifies that {@code log()} method invokes {@code log()} methods from underlying logging providers.
	 */
	@Test
	public void log() {
		init(Level.TRACE, Level.TRACE);

		NullPointerException exception = new NullPointerException();
		wrapped.log(1, Level.INFO, exception, "Test", 42);

		verify(first).log(2, Level.INFO, exception, "Test", 42);
		verify(second).log(2, Level.INFO, exception, "Test", 42);
	}

	/**
	 * Verifies that {@code internal()} method invokes {@code internal()} methods from underlying logging providers.
	 */
	@Test
	public void internal() {
		init(Level.TRACE, Level.TRACE);

		NullPointerException exception = new NullPointerException();
		wrapped.internal(1, Level.INFO, exception, "Test", 42);

		verify(first).internal(2, Level.INFO, exception, "Test", 42);
		verify(second).internal(2, Level.INFO, exception, "Test", 42);
	}

	/**
	 * Creates underlying logging providers as well as the wrapper logging provider.
	 *
	 * @param firstLevel
	 *            Minimum severity level of first underlying logging provider
	 * @param secondLevel
	 *            Minimum severity level of second underlying logging provider
	 */
	private void init(final Level firstLevel, final Level secondLevel) {
		first = mock(LoggingProvider.class);
		second = mock(LoggingProvider.class);

		when(first.getMinimumLevel()).thenReturn(firstLevel);
		when(second.getMinimumLevel()).thenReturn(secondLevel);

		wrapped = new WrapperLoggingProvider(asList(first, second));
	}

}