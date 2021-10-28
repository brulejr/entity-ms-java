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
package io.jrb.labs.common.service.command.entity;

import io.jrb.labs.common.domain.Entity;
import io.jrb.labs.common.repository.EntityRepository;
import io.jrb.labs.common.resource.Resource;
import io.jrb.labs.common.resource.ResourceRequest;
import io.jrb.labs.common.service.command.Command;
import org.reactivestreams.Publisher;

import java.util.function.Function;

public abstract class GetEntitiesCommand<
        I extends ResourceRequest<I>,
        O extends Resource<O>,
        C extends EntityCommandContext<I, O, C>,
        E extends Entity<E>> implements Command<I, O, C> {

    private final String entityType;
    private final Function<E, O> toResourceFn;
    private final EntityRepository<E> repository;

    protected GetEntitiesCommand(
            final String entityType,
            final Function<E, O> toResourceFn,
            final EntityRepository<E> repository
    ) {
        this.entityType = entityType;
        this.toResourceFn = toResourceFn;
        this.repository = repository;
    }

    @Override
    public Publisher<C> execute(final C context) {
        return repository.findAll()
                .map(toResourceFn)
                .map(context::withOutput)
                .onErrorResume(t -> handleException(t, "retrieve all " + entityType));
    }

}
