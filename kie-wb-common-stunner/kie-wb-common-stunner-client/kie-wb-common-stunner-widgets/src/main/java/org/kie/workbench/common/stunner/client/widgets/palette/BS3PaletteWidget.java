/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.workbench.common.stunner.client.widgets.palette;

import org.kie.workbench.common.stunner.client.widgets.palette.factory.BS3PaletteViewFactory;
import org.kie.workbench.common.stunner.core.client.components.palette.model.PaletteDefinition;
import org.kie.workbench.common.stunner.core.definition.shape.Glyph;

public interface BS3PaletteWidget<P extends PaletteDefinition> extends PaletteWidget<P> {

    BS3PaletteWidget setViewFactory(final BS3PaletteViewFactory viewFactory);

    void onDragStart(String definitionId,
                     double x,
                     double y);

    void onDragProxyComplete(String definitionId,
                             double x,
                             double y);

    void onDragProxyMove(String definitionId,
                         double x,
                         double y);

    Glyph getShapeGlyph(String definitionId);
}
