/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.stunner.core.graph.content.view;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.stunner.core.graph.impl.GraphImpl;
import org.kie.workbench.common.stunner.core.graph.impl.NodeImpl;
import org.kie.workbench.common.stunner.core.graph.store.GraphNodeStoreImpl;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ViewConnectorHashCodeAndEqualityTest {

    @Test
    public void testViewConnectorEquals() {
        ViewConnectorImpl<String> a = new ViewConnectorImpl<>("a", new BoundsImpl(new BoundImpl(0.0,0.0),
                                                                                  new BoundImpl(1.0,1.0)));
        ViewConnectorImpl<String> b = new ViewConnectorImpl<>("a", new BoundsImpl(new BoundImpl(0.0,0.0),
                                                                                  new BoundImpl(1.0,1.0)));
        assertEquals(a,b);

        b.setDefinition("b");
        assertNotEquals(a,b);

        b.setDefinition("a");
        b.setBounds(new BoundsImpl(new BoundImpl(0.0,0.0),
                                   new BoundImpl(5.0,5.0)));
        assertNotEquals(a,b);
        b.setBounds(new BoundsImpl(new BoundImpl(0.0,0.0),
                       new BoundImpl(1.0,1.0)));

        b.setSourceMagnet(MagnetImpl.Builder.build(0.0,0.0));
        assertNotEquals(a,b);

        a.setSourceMagnet(MagnetImpl.Builder.build(0.0,0.0));
        assertEquals(a,b);
        a.setSourceMagnet(MagnetImpl.Builder.build(1.0,1.0));
        assertNotEquals(a,b);
        b.setSourceMagnet(MagnetImpl.Builder.build(1.0,1.0));

        b.setTargetMagnet(MagnetImpl.Builder.build(0.0,0.0));
        assertNotEquals(a,b);
        a.setTargetMagnet(MagnetImpl.Builder.build(0.0,0.0));
        assertEquals(a,b);
        a.setTargetMagnet(MagnetImpl.Builder.build(2.0,2.0));
        assertNotEquals(a,b);
    }

    @Test
    public void testGraphHashCode() {
        ViewConnectorImpl<String> a = new ViewConnectorImpl<>("a", new BoundsImpl(new BoundImpl(0.0,0.0),
                                                                                  new BoundImpl(1.0,1.0)));
        ViewConnectorImpl<String> b = new ViewConnectorImpl<>("a", new BoundsImpl(new BoundImpl(0.0,0.0),
                                                                                  new BoundImpl(1.0,1.0)));
        assertEquals(a.hashCode(),b.hashCode());

        b.setDefinition("b");
        assertNotEquals(a.hashCode(),b.hashCode());

        b.setDefinition("a");
        b.setBounds(new BoundsImpl(new BoundImpl(0.0,0.0),
                                   new BoundImpl(5.0,5.0)));
        assertNotEquals(a.hashCode(),b.hashCode());
        b.setBounds(new BoundsImpl(new BoundImpl(0.0,0.0),
                                   new BoundImpl(1.0,1.0)));

        b.setSourceMagnet(MagnetImpl.Builder.build(0.0,0.0));
        assertNotEquals(a.hashCode(),b.hashCode());

        a.setSourceMagnet(MagnetImpl.Builder.build(0.0,0.0));
        assertEquals(a.hashCode(),b.hashCode());
        a.setSourceMagnet(MagnetImpl.Builder.build(1.0,1.0));
        assertNotEquals(a.hashCode(),b.hashCode());
        b.setSourceMagnet(MagnetImpl.Builder.build(1.0,1.0));

        b.setTargetMagnet(MagnetImpl.Builder.build(0.0,0.0));
        assertNotEquals(a.hashCode(),b.hashCode());
        a.setTargetMagnet(MagnetImpl.Builder.build(0.0,0.0));
        assertEquals(a.hashCode(),b.hashCode());
        a.setTargetMagnet(MagnetImpl.Builder.build(3.0,2.0));
        assertNotEquals(a.hashCode(),b.hashCode());
    }
}
