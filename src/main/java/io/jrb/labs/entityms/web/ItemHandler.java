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
import io.jrb.labs.entityms.resource.AddItemResource;
import io.jrb.labs.entityms.resource.ItemResource;
import io.jrb.labs.entityms.service.command.CreateItemCommand;
import io.jrb.labs.entityms.service.command.FindItemCommand;
import io.jrb.labs.entityms.service.command.GetItemsCommand;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2CodecSupport;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import javax.validation.Validator;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

@Component
public class ItemHandler implements RouteHandler {

    private final CreateItemCommand createItemCommand;
    private final FindItemCommand findItemCommand;
    private final GetItemsCommand getItemsCommand;
    private final Validator validator;

    public ItemHandler(
            final CreateItemCommand createItemCommand,
            final FindItemCommand findItemCommand,
            final GetItemsCommand getItemsCommand,
            final Validator validator
    ) {
        this.createItemCommand = createItemCommand;
        this.findItemCommand = findItemCommand;
        this.getItemsCommand = getItemsCommand;
        this.validator = validator;
    }

    public Mono<ServerResponse> createItem(final ServerRequest serverRequest) {
        return requireValidBody((final Mono<AddItemResource> addItemMono) ->
            addItemMono.flatMap(item -> {
                final Mono<ItemResource> itemResourceMono = createItemCommand.execute(item);
                return ServerResponse.status(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .hint(Jackson2CodecSupport.JSON_VIEW_HINT, Projection.Detail.class)
                        .body(itemResourceMono, ItemResource.class);
            }), serverRequest, AddItemResource.class, validator);
    }

    public Mono<ServerResponse> findItem(final ServerRequest serverRequest) {
        final String itemGuid = serverRequest.pathVariable("guid");
        final Mono<ItemResource> itemResourceMono = findItemCommand.execute(itemGuid);
        return itemResourceMono.flatMap(item ->
                ServerResponse.ok()
                        .hint(Jackson2CodecSupport.JSON_VIEW_HINT, Projection.Detail.class)
                        .body(fromValue(item)))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> getAllItems(final ServerRequest serverRequest) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .hint(Jackson2CodecSupport.JSON_VIEW_HINT, Projection.Summary.class)
                .body(getItemsCommand.execute(null), ItemResource.class);
    }

}
