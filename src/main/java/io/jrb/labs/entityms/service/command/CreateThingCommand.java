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
package io.jrb.labs.entityms.service.command;

import io.jrb.labs.common.service.command.entity.CreateEntityCommand;
import io.jrb.labs.common.service.command.entity.EntityUtils;
import io.jrb.labs.entityms.domain.ThingEntity;
import io.jrb.labs.entityms.mapper.ThingMapper;
import io.jrb.labs.entityms.repository.ThingEntityRepository;
import io.jrb.labs.entityms.resource.ThingRequest;
import io.jrb.labs.entityms.resource.ThingResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CreateThingCommand extends CreateEntityCommand<ThingRequest, ThingResource, ThingContext, ThingEntity> {

    public CreateThingCommand(
            final ThingMapper mapper,
            final ThingEntityRepository repository,
            final EntityUtils entityUtils
    ) {
        super(mapper::thingRequestToThingEntity, mapper::thingEntityToThingResource, repository, entityUtils);
    }

}
