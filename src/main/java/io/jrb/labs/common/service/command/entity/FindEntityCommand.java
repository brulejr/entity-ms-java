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
import io.jrb.labs.common.resource.Projection;
import io.jrb.labs.common.resource.Resource;
import io.jrb.labs.common.service.command.Command;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class FindEntityCommand<RSP extends Resource<RSP>, E extends Entity<E>> implements Command<String, RSP> {

    private final String entityType;
    private final Function<E, RSP> toResourceFn;
    private final EntityRepository<E> repository;
    private final LookupValueRepository lookupValueRepository;

    protected FindEntityCommand(
            final String entityType,
            final Function<E, RSP> toResourceFn,
            final EntityRepository<E> repository,
            final LookupValueRepository lookupValueRepository
    ) {
        this.entityType = entityType;
        this.toResourceFn = toResourceFn;
        this.repository = repository;
        this.lookupValueRepository = lookupValueRepository;
    }

    @Override
    public Mono<RSP> execute(final String guid) {
        return repository.findByGuid(guid)
                .zipWhen(entity -> findValueList(entity.getId(), Projection.DEEP))
                .map(tuple -> toResourceFn.apply(tuple.getT1())
                        .withTags(extractValues(tuple.getT2(), "TAG")))
                .onErrorResume(t -> handleException(t, "find " + entityType))
                .switchIfEmpty(Mono.error(new UnknownEntityException(this, entityType)));
    }

    private List<String> extractValues(final List<LookupValue> values, final String valueType) {
        return values.stream()
                .filter(v -> valueType.equals(v.getValueType()))
                .map(LookupValue::getValue)
                .collect(Collectors.toList());
    }

    private Mono<List<LookupValue>> findValueList(final long entityId, final Projection projection) {
        if (projection == Projection.DEEP) {
            return lookupValueRepository.findByEntityId(entityId)
                    .collectList();
        } else {
            return Mono.just(Collections.emptyList());
        }
    }

}
