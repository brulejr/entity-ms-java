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

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Provides a web filter that adds traceability data to reactive web service responses.
 */
@Slf4j
public class TraceabilityWebFilter implements WebFilter {

    private final TraceabilityDatafill traceabilityDatafill;
    private final Function<ServerHttpRequest, Map<String, String>> requestHeaderExtractor;
    private final BiConsumer<Map<String, String>, ServerHttpResponse> responseHeaderCompositor;

    public TraceabilityWebFilter(
            final TraceabilityDatafill traceabilityDatafill,
            final Function<ServerHttpRequest, Map<String, String>> requestHeaderExtractor,
            final BiConsumer<Map<String, String>, ServerHttpResponse> responseHeaderCompositor
    ) {
        this.traceabilityDatafill = traceabilityDatafill;
        this.requestHeaderExtractor = requestHeaderExtractor;
        this.responseHeaderCompositor = responseHeaderCompositor;
    }

    /**
     * Wraps the given reactive Web request in a block that adds traceability data, including a unique request identifier
     * and duration, before delegating to the next {@code WebFilter} through the given {@link WebFilterChain}.
     *
     * Note that this WebFilter adds a {@code beforeCommit()} hook to the response in order to capture the duration and
     * inject it into the response headers after the entire chain has completed. It is important to note that using the
     * {@link Mono#doFinally(Consumer)} block does not work as the response is already committed and the response
     * headers are readonly at that point.
     *
     * @param exchange the current server exchange
     * @param chain provides a way to delegate to the next filter
     * @return {@code Mono<Void>} to indicate when request processing is complete
     */
    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        final Map<String, String> traceData = requestHeaderExtractor.apply(exchange.getRequest());
        log.debug("inbound traceData = {}", traceData);

        long startTime = System.currentTimeMillis();

        exchange.getResponse().beforeCommit(() -> {

            // calculate duration
            final long duration = System.currentTimeMillis() - startTime;
            traceData.put(traceabilityDatafill.getDuration(), String.valueOf(duration));

            // assemble response headers
            responseHeaderCompositor.accept(traceData, exchange.getResponse());

            return Mono.empty();
        });
        return chain.filter(exchange);
    }

}
