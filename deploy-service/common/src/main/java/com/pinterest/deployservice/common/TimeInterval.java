/**
 * Copyright (c) 2026 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.deployservice.common;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Immutable half-open [start, end) time interval expressed in epoch milliseconds. The start is
 * inclusive and the end is exclusive.
 */
public final class TimeInterval {
    /** ISO-8601 with millisecond precision and zone offset, e.g. 2022-07-04T10:00:00.000-07:00 */
    public static final DateTimeFormatter ISO_MILLIS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private final long startMillis;
    private final long endMillis;

    public TimeInterval(long startMillis, long endMillis) {
        if (endMillis < startMillis) {
            throw new IllegalArgumentException("The end instant must be greater than the start");
        }
        this.startMillis = startMillis;
        this.endMillis = endMillis;
    }

    public TimeInterval(Instant start, Instant end) {
        this(start.toEpochMilli(), end.toEpochMilli());
    }

    public long getStartMillis() {
        return startMillis;
    }

    public long getEndMillis() {
        return endMillis;
    }

    public Instant getStart() {
        return Instant.ofEpochMilli(startMillis);
    }

    public Instant getEnd() {
        return Instant.ofEpochMilli(endMillis);
    }

    public boolean contains(long millis) {
        return millis >= startMillis && millis < endMillis;
    }

    public static String format(Instant instant) {
        return ISO_MILLIS.format(instant.atZone(ZoneId.systemDefault()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TimeInterval)) {
            return false;
        }
        TimeInterval other = (TimeInterval) o;
        return startMillis == other.startMillis && endMillis == other.endMillis;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startMillis, endMillis);
    }

    @Override
    public String toString() {
        return format(getStart()) + "/" + format(getEnd());
    }
}
