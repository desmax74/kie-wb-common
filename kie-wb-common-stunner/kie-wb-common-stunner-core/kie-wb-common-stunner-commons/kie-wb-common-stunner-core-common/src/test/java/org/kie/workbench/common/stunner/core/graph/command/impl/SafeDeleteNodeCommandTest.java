/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.stunner.core.graph.command.impl;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.stunner.core.TestingGraphInstanceBuilder;
import org.kie.workbench.common.stunner.core.TestingGraphMockHandler;
import org.kie.workbench.common.stunner.core.command.Command;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.kie.workbench.common.stunner.core.graph.command.GraphCommandExecutionContext;
import org.kie.workbench.common.stunner.core.rule.RuleViolation;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 * This test case uses a valid graph structure with different nodes and connectors
 * and checks that when removing a certain node, the right commands are being accumulated
 * in the right order.
 */
@RunWith(MockitoJUnitRunner.class)
public class SafeDeleteNodeCommandTest {

    private TestingGraphMockHandler graphTestHandler;
    private TestingGraphInstanceBuilder.TestGraph2 graphHolder;
    private SafeDeleteNodeCommand tested;

    @Before
    public void setup() throws Exception {
        this.graphTestHandler = new TestingGraphMockHandler();
        this.graphHolder = TestingGraphInstanceBuilder.newGraph2(graphTestHandler);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeleteStartNode() {
        this.tested = new SafeDeleteNodeCommand(graphHolder.startNode);
        final CommandResult<RuleViolation> result = tested.allow(graphTestHandler.graphCommandExecutionContext);
        final List<Command<GraphCommandExecutionContext, RuleViolation>> commands = tested.getCommands();
        assertNotNull(commands);
        assertTrue(3 == commands.size());
        final SetConnectionSourceNodeCommand setConnectionSource = (SetConnectionSourceNodeCommand) commands.get(0);
        assertNotNull(setConnectionSource);
        assertEquals(graphHolder.edge1,
                     setConnectionSource.getEdge());
        assertNull(setConnectionSource.getSourceNode());
        final RemoveChildCommand removeChild = (RemoveChildCommand) commands.get(1);
        assertNotNull(removeChild);
        assertEquals(graphHolder.parentNode,
                     removeChild.getParent());
        assertEquals(graphHolder.startNode,
                     removeChild.getCandidate());
        final DeregisterNodeCommand deleteNode = (DeregisterNodeCommand) commands.get(2);
        assertNotNull(deleteNode);
        assertEquals(graphHolder.startNode,
                     deleteNode.getNode());
        assertEquals(CommandResult.Type.INFO,
                     result.getType());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeleteEndNode() {
        this.tested = new SafeDeleteNodeCommand(graphHolder.endNode);
        final CommandResult<RuleViolation> result = tested.allow(graphTestHandler.graphCommandExecutionContext);
        final List<Command<GraphCommandExecutionContext, RuleViolation>> commands = tested.getCommands();
        assertNotNull(commands);
        assertTrue(3 == commands.size());
        final SetConnectionTargetNodeCommand setConnectionTarget = (SetConnectionTargetNodeCommand) commands.get(0);
        assertNotNull(setConnectionTarget);
        assertEquals(graphHolder.edge2,
                     setConnectionTarget.getEdge());
        assertNull(setConnectionTarget.getTargetNode());
        final RemoveChildCommand removeChild = (RemoveChildCommand) commands.get(1);
        assertNotNull(removeChild);
        assertEquals(graphHolder.parentNode,
                     removeChild.getParent());
        assertEquals(graphHolder.endNode,
                     removeChild.getCandidate());
        final DeregisterNodeCommand deleteNode = (DeregisterNodeCommand) commands.get(2);
        assertNotNull(deleteNode);
        assertEquals(graphHolder.endNode,
                     deleteNode.getNode());
        assertEquals(CommandResult.Type.INFO,
                     result.getType());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeleteIntermediateNode() {
        // Expect the connector for edge1 to be shortcut in this test case.
        this.tested = new SafeDeleteNodeCommand(graphHolder.intermNode);
        final CommandResult<RuleViolation> result = tested.allow(graphTestHandler.graphCommandExecutionContext);
        final List<Command<GraphCommandExecutionContext, RuleViolation>> commands = tested.getCommands();
        assertNotNull(commands);
        assertTrue(6 == commands.size());
        final SetConnectionSourceNodeCommand setConnectionSource = (SetConnectionSourceNodeCommand) commands.get(0);
        assertNotNull(setConnectionSource);
        assertEquals(graphHolder.edge2,
                     setConnectionSource.getEdge());
        assertNull(setConnectionSource.getSourceNode());
        assertEquals(graphHolder.endNode,
                     setConnectionSource.getTargetNode());
        final SetConnectionTargetNodeCommand setConnectionTarget = (SetConnectionTargetNodeCommand) commands.get(1);
        assertNotNull(setConnectionTarget);
        assertEquals(graphHolder.edge1,
                     setConnectionTarget.getEdge());
        assertNull(setConnectionTarget.getTargetNode());
        assertEquals(graphHolder.startNode,
                     setConnectionTarget.getSourceNode());
        final RemoveChildCommand removeChild = (RemoveChildCommand) commands.get(2);
        assertNotNull(removeChild);
        assertEquals(graphHolder.parentNode,
                     removeChild.getParent());
        assertEquals(graphHolder.intermNode,
                     removeChild.getCandidate());
        final DeleteConnectorCommand deleteConnector2 = (DeleteConnectorCommand) commands.get(3);
        assertNotNull(deleteConnector2);
        assertEquals(graphHolder.edge2,
                     deleteConnector2.getEdge());
        final SetConnectionTargetNodeCommand setConnectionNewTarget = (SetConnectionTargetNodeCommand) commands.get(4);
        assertNotNull(setConnectionNewTarget);
        assertEquals(graphHolder.edge1,
                     setConnectionNewTarget.getEdge());
        assertEquals(graphHolder.startNode,
                     setConnectionNewTarget.getSourceNode());
        assertEquals(graphHolder.endNode,
                     setConnectionNewTarget.getTargetNode());
        final DeregisterNodeCommand deleteNode = (DeregisterNodeCommand) commands.get(5);
        assertNotNull(deleteNode);
        assertEquals(graphHolder.intermNode,
                     deleteNode.getNode());
        assertEquals(CommandResult.Type.INFO,
                     result.getType());
    }
}
