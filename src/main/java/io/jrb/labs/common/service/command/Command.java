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
package io.jrb.labs.common.service.command;

import org.reactivestreams.Publisher;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import static java.lang.String.format;

/**
 * Defines a command that can be executed.
 *
 * @param <REQ> the request type
 * @param <RSP> the response type
 */
@FunctionalInterface
public interface Command<REQ, RSP> {

    /**
     * Obtains the name for this command.
     *
     * @return the command name
     */
    default String getCommandName() { return getClass().getSimpleName(); }

    /**
     * Provides a default exception handler for commands.
     *
     * @param t the exception
     * @param action a string describing the current action
     * @param <T> the effective type
     * @return a {@link Mono} containing the error
     */
    default <T> Mono<T> handleException(final Throwable t, final String action) {
        final String pattern = "Unable to %s due to unexpected error!";
        return Mono.error(new CommandException(
                this,
                HttpStatus.INTERNAL_SERVER_ERROR.value(), format(pattern, action),
                t
        ));
    }

    /**
     * Executes the command.
     *
     * @param request the command request
     * @return the command response
     */
    Publisher<RSP> execute(REQ request);

}
