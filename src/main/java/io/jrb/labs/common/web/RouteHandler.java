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
package io.jrb.labs.common.web;

import io.jrb.labs.common.resource.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface RouteHandler {

    default <T> Mono<ServerResponse> requireValidBody(
            final Function<Mono<T>, Mono<ServerResponse>> block,
            final ServerRequest request,
            final Class<T> bodyClass,
            final Validator validator
    ) {
        return request
                .bodyToMono(bodyClass)
                .flatMap((final T body) -> {
                    final Set<ConstraintViolation<T>> violations = validator.validate(body);
                    return violations.isEmpty()
                            ? block.apply(Mono.just(body))
                            : handleValidationErrors(bodyClass, violations);
                });
    }

    private <T> Mono<ServerResponse> handleValidationErrors(
            final Class<T> bodyClass,
            final Set<ConstraintViolation<T>> violations
    ) {
        final List<String> bindingErrors = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toUnmodifiableList());
        final ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .message("Cannot validate " + bodyClass.getSimpleName())
                .bindingErrors(bindingErrors)
                .build();
        return ServerResponse.unprocessableEntity().bodyValue(errorResponse);
    }

}
