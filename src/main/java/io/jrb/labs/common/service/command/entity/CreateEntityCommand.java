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
import io.jrb.labs.common.domain.LookupValue;
import io.jrb.labs.common.repository.EntityRepository;
import io.jrb.labs.common.repository.LookupValueRepository;
import io.jrb.labs.common.resource.Resource;
import io.jrb.labs.common.resource.ResourceRequest;
import io.jrb.labs.common.service.command.Command;
import io.jrb.labs.common.service.command.CommandException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
public abstract class CreateEntityCommand<
        I extends ResourceRequest<I>,
        O extends Resource<O>,
        C extends EntityCommandContext<I, O, C>,
        E extends Entity<E>> implements Command<I, O, C> {

    private static final String UNIQUE_INDEX_ERROR = "Unique index or primary key violation";

    private final String entityType;
    private final Function<I, E> toEntityFn;
    private final Function<E, O> toResourceFn;
    private final EntityRepository<E> repository;
    private final LookupValueRepository lookupValueRepository;

    protected CreateEntityCommand(
            final String entityType,
            final Function<I, E> toEntityFn,
            final Function<E, O> toResourceFn,
            final EntityRepository<E> repository,
            final LookupValueRepository lookupValueRepository
    ) {
        this.entityType = entityType;
        this.toEntityFn = toEntityFn;
        this.toResourceFn = toResourceFn;
        this.repository = repository;
        this.lookupValueRepository = lookupValueRepository;
    }

    @Override
    public Mono<C> execute(final C context) {
        final I input = context.getInput();
        return createEntity(input)
                .zipWhen(entity -> createLookupValues(entity.getId(), "TAG", input.getTags()))
                .map(tuple -> toResourceFn.apply(tuple.getT1())
                        .withTags(tuple.getT2()))
                .map(context::withOutput)
                .onErrorResume(this::handleException);
    }

    private Mono<E> createEntity(final I request) {
        return Mono.just(request)
                .map(toEntityFn)
                .map(entity -> entity.withGuid(UUID.randomUUID().toString()))
                .flatMap(repository::save);
    }

    private Mono<List<String>> createLookupValues(
            final long entityId,
            final String type,
            final List<String> values
    ) {
        if (values != null) {
            return Flux.fromIterable(values)
                    .map(value -> LookupValue.builder()
                            .entityId(entityId)
                            .valueType(type)
                            .value(value)
                            .build())
                    .flatMap(lookupValueRepository::save)
                    .map(LookupValue::getValue)
                    .collectList();
        } else {
            return Mono.empty();
        }
    }

    private Mono<C> handleException(final Throwable t) {
        if (t instanceof DataIntegrityViolationException) {
            final Optional<String> message = Optional.ofNullable(t).map(Throwable::getMessage);
            if (message.isPresent() && message.get().contains(UNIQUE_INDEX_ERROR)) {
                return Mono.error(new DuplicateEntityException(this, entityType));
            }
        }
        return Mono.error(new CommandException(
                this,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "create " + entityType,
                t
        ));
    }

}
