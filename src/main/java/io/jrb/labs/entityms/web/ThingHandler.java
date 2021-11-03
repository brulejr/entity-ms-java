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
package io.jrb.labs.entityms.web;

import io.jrb.labs.common.resource.Projection;
import io.jrb.labs.common.web.RouteHandler;
import io.jrb.labs.entityms.resource.ThingRequest;
import io.jrb.labs.entityms.resource.ThingResource;
import io.jrb.labs.entityms.service.command.CreateThingCommand;
import io.jrb.labs.entityms.service.command.FindThingCommand;
import io.jrb.labs.entityms.service.command.GetThingsCommand;
import io.jrb.labs.entityms.service.command.ThingContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2CodecSupport;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Validator;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@Component
public class ThingHandler implements RouteHandler {

    private final CreateThingCommand createThingCommand;
    private final FindThingCommand findThingCommand;
    private final GetThingsCommand getThingsCommand;
    private final Validator validator;

    public ThingHandler(
            final CreateThingCommand createThingCommand,
            final FindThingCommand findThingCommand,
            final GetThingsCommand getThingsCommand,
            final Validator validator
    ) {
        this.createThingCommand = createThingCommand;
        this.findThingCommand = findThingCommand;
        this.getThingsCommand = getThingsCommand;
        this.validator = validator;
    }

    public Mono<ServerResponse> createThing(final ServerRequest serverRequest) {
        final String entityType = serverRequest.pathVariable("entityType");
        return requireValidBody((final Mono<ThingRequest> addThingMono) ->
            addThingMono.flatMap(thing -> {
                final ThingContext context = ThingContext.builder()
                        .entityType(entityType)
                        .input(thing)
                        .build();
                final Mono<ThingResource> thingResourceMono = createThingCommand.execute(context)
                        .map(ThingContext::getOutput);
                return ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .hint(Jackson2CodecSupport.JSON_VIEW_HINT, Projection.Detail.class)
                        .body(thingResourceMono, ThingResource.class);
            }), serverRequest, ThingRequest.class, validator);
    }

    public Mono<ServerResponse> findThing(final ServerRequest serverRequest) {
        final String entityType = serverRequest.pathVariable("entityType");
        final String thingGuid = serverRequest.pathVariable("guid");
        final Projection projection = extractProjection(serverRequest, Projection.DETAILS);
        final ThingContext context = ThingContext.builder()
                .entityType(entityType)
                .guid(thingGuid)
                .projection(projection)
                .build();
        final Mono<ThingResource> thingResourceMono = findThingCommand.execute(context)
                .map(ThingContext::getOutput);
        return thingResourceMono.flatMap(thing ->
                ServerResponse.ok()
                        .hint(Jackson2CodecSupport.JSON_VIEW_HINT, projection.view)
                        .body(fromValue(thing)))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> getAllThings(final ServerRequest serverRequest) {
        final String entityType = serverRequest.pathVariable("entityType");
        final Projection projection = extractProjection(serverRequest, Projection.SUMMARY);
        final ThingContext context = ThingContext.builder()
                .entityType(entityType)
                .projection(projection)
                .build();
        final Flux<ThingResource> contentFlux = Flux.from(getThingsCommand.execute(context))
                .map(ThingContext::getOutput);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .hint(Jackson2CodecSupport.JSON_VIEW_HINT, projection.view)
                .body(contentFlux, ThingResource.class);
    }

    private Projection extractProjection(final ServerRequest serverRequest, final Projection defaultProjection) {
        return serverRequest.queryParam("projection")
                .map(Projection::valueOf)
                .orElse(defaultProjection);
    }

}
