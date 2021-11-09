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
package io.jrb.labs.common.h2;

import lombok.extern.slf4j.Slf4j;
import org.h2.tools.Server;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.sql.SQLException;

@Slf4j
public class H2ConsoleServer {

    private final int consolePort;
    private Server webServer;

    public H2ConsoleServer(final int consolePort) {
        this.consolePort = consolePort;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void start() throws SQLException {
        log.info("starting h2 console at port {}", this.consolePort);
        this.webServer = Server.createWebServer(new String[]{
                "-webPort", String.valueOf(this.consolePort),
                "-tcpAllowOthers"
        }).start();
    }

    @EventListener({ContextClosedEvent.class})
    public void stop() {
        log.info("stopping h2 console at port {}", this.consolePort);
        this.webServer.stop();
    }

}
