/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aesh.console.alias;

import org.jboss.aesh.console.Config;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class AliasManagerTest {

    private AliasManager manager;

    @Before
    public void setTup() {
        try {
            manager = new AliasManager(new File("foo"), false, "aesh");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testParseAlias() {

        assertNull(manager.parseAlias("alias foo2='bar -s -h'"));
        assertNull(manager.parseAlias("alias foo=bar"));
        assertNull(manager.parseAlias("alias foo3=bar --help"));

        String out = manager.parseAlias("alias foo");
        assertEquals("alias foo='bar'"+ Config.getLineSeparator(), out);
        out = manager.parseAlias("alias foo2");
        assertEquals("alias foo2='bar -s -h'"+ Config.getLineSeparator(), out);
        out = manager.parseAlias("alias foo3");
        assertEquals("alias foo3='bar --help'"+ Config.getLineSeparator(), out);
        out = manager.parseAlias("alias");
        StringBuilder sb = new StringBuilder();
        sb.append("alias foo='bar'").append(Config.getLineSeparator())
                .append("alias foo2='bar -s -h'").append(Config.getLineSeparator())
                .append("alias foo3='bar --help'").append(Config.getLineSeparator());
        assertEquals(sb.toString(), out);
    }

    @Test
    public void testUnalias() {

        manager.parseAlias("alias foo2='bar -s -h'");
        manager.parseAlias("alias foo=bar");
        manager.parseAlias("alias foo3=bar --help");

        manager.removeAlias("unalias foo3");
        assertEquals("aesh: unalias: foo3: not found"+Config.getLineSeparator(), manager.removeAlias("unalias foo3"));
    }

    @Test
    public void testPrintAllAliases() {
        String alias = "alias foo='bar'";
        manager.parseAlias(alias);
        Assert.assertEquals(alias + Config.getLineSeparator(), manager.printAllAliases());
    }

}
