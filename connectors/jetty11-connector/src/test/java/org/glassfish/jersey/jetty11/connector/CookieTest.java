/*
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.jersey.jetty11.connector;

import java.util.logging.Logger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Paul Sandoz
 * @author Arul Dhesiaseelan (aruld at acm.org)
 */
public class CookieTest extends JerseyTest {

    private static final Logger LOGGER = Logger.getLogger(CookieTest.class.getName());

    @Path("/")
    public static class CookieResource {
        @GET
        public Response get(@Context HttpHeaders h) {
            Cookie c = h.getCookies().get("name");
            String e = (c == null) ? "NO-COOKIE" : c.getValue();
            return Response.ok(e)
                    .cookie(new NewCookie("name", "value")).build();
        }
    }

    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig(CookieResource.class);
        config.register(new LoggingFeature(LOGGER, LoggingFeature.Verbosity.PAYLOAD_ANY));
        return config;
    }

    @Test
    public void testCookieResource() {
        ClientConfig config = new ClientConfig();
        config.connectorProvider(new Jetty11ConnectorProvider());
        Client client = ClientBuilder.newClient(config);
        WebTarget r = client.target(getBaseUri());


        assertEquals("NO-COOKIE", r.request().get(String.class));
        assertEquals("value", r.request().get(String.class));
        client.close();
    }

    @Test
    public void testDisabledCookies() {
        ClientConfig cc = new ClientConfig();
        cc.property(Jetty11ClientProperties.DISABLE_COOKIES, true);
        cc.connectorProvider(new Jetty11ConnectorProvider());
        JerseyClient client = JerseyClientBuilder.createClient(cc);
        WebTarget r = client.target(getBaseUri());

        assertEquals("NO-COOKIE", r.request().get(String.class));
        assertEquals("NO-COOKIE", r.request().get(String.class));

        final Jetty11Connector connector = (Jetty11Connector) client.getConfiguration().getConnector();
        if (connector.getCookieStore() != null) {
            assertTrue(connector.getCookieStore().getCookies().isEmpty());
        } else {
            assertNull(connector.getCookieStore());
        }
        client.close();
    }

    @Test
    public void testCookies() {
        ClientConfig cc = new ClientConfig();
        cc.connectorProvider(new Jetty11ConnectorProvider());
        JerseyClient client = JerseyClientBuilder.createClient(cc);
        WebTarget r = client.target(getBaseUri());

        assertEquals("NO-COOKIE", r.request().get(String.class));
        assertEquals("value", r.request().get(String.class));

        final Jetty11Connector connector = (Jetty11Connector) client.getConfiguration().getConnector();
        assertNotNull(connector.getCookieStore().getCookies());
        assertEquals(1, connector.getCookieStore().getCookies().size());
        assertEquals("value", connector.getCookieStore().getCookies().get(0).getValue());
        client.close();
    }
}