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
import io.jrb.labs.common.repository.LookupValueRepository;
import io.jrb.labs.common.resource.Projection;
import io.jrb.labs.common.resource.Resource;
import io.jrb.labs.common.service.command.entity.config.EntityType;
import io.jrb.labs.common.service.command.entity.config.EntityServiceProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EntityUtils {

    private final LookupValueRepository lookupValueRepository;
    private final EntityServiceProperties entSvcProps;

    public EntityUtils(
            final LookupValueRepository lookupValueRepository,
            final EntityServiceProperties entSvcProps
    ) {
        this.lookupValueRepository = lookupValueRepository;
        this.entSvcProps = entSvcProps;
    }

    public <E extends Entity<E>, O extends Resource<O>> Mono<O> addLookupValues(
            final E entity,
            final Function<E, O> toResourceFn,
            final Projection projection
    ) {
        return Mono.just(entity)
                .zipWhen(e -> findValueList(e.getId(), projection))
                .map(tuple -> toResourceFn.apply(tuple.getT1())
                        .withTags(extractValues(tuple.getT2(), "TAG")));
    }

    public Mono<List<String>> createLookupValues(final long entityId, final String type, final List<String> values) {
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

    public List<String> extractValues(final List<LookupValue> values, final String valueType) {
        return values.stream()
                .filter(v -> valueType.equals(v.getValueType()))
                .map(LookupValue::getValue)
                .collect(Collectors.toList());
    }

    public EntityType findEntityType(final String entityTypeName) {
        return entSvcProps.getEntities().stream()
                .filter(d -> entityTypeName.equals(d.getType()))
                .findAny()
                .orElseThrow(() -> new UnknownEntityTypeException(entityTypeName));
    }

    public Mono<List<LookupValue>> findValueList(final long entityId, final Projection projection) {
        if (projection == Projection.DEEP) {
            return lookupValueRepository.findByEntityId(entityId)
                    .collectList();
        } else {
            return Mono.just(Collections.emptyList());
        }
    }

}
