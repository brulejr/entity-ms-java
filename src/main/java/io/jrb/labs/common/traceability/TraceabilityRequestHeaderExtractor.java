/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Jon Brule <brulejr@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.jrb.labs.common.traceability;

import org.javatuples.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TraceabilityRequestHeaderExtractor implements Function<ServerHttpRequest, Map<String, String>> {

    private final TraceabilityDatafill traceabilityDatafill;

    public TraceabilityRequestHeaderExtractor(final TraceabilityDatafill traceabilityDatafill) {
        this.traceabilityDatafill = traceabilityDatafill;
    }

    @Override
    public Map<String, String> apply(final ServerHttpRequest httpRequest) {
        final HttpHeaders requestHeaders = httpRequest.getHeaders();
        return Stream.of(
                Pair.with(traceabilityDatafill.getApplicationId(), traceabilityDatafill.getApplicationName()),
                extractHeader(requestHeaders, traceabilityDatafill.getTransactionId()),
                Pair.with(traceabilityDatafill.getRequestId(), randomUUID())
        ).collect(Collectors.toConcurrentMap(Pair::getValue0, Pair::getValue1));
    }

    private Pair<String, String> extractHeader(final HttpHeaders httpHeaders, final String key) {
        final String value = Optional.ofNullable(httpHeaders.get(key))
                .map(list -> String.join(traceabilityDatafill.getListSeparator(), list))
                .orElseThrow(() -> new MissingTraceabilityHeaderException("Header '" + key +"' cannot be null!"));
        return Pair.with(key, value);
    }

    private String randomUUID() {
        return UUID.randomUUID().toString();
    }

}
