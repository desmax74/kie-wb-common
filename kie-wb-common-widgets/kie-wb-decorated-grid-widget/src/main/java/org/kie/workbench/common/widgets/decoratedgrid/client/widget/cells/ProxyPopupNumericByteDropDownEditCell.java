/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
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
package org.kie.workbench.common.widgets.decoratedgrid.client.widget.cells;

import org.kie.soup.project.datamodel.oracle.DataType;
import org.kie.workbench.common.widgets.client.datamodel.AsyncPackageDataModelOracle;
import org.kie.workbench.common.widgets.client.widget.TextBoxFactory;
import org.kie.workbench.common.widgets.decoratedgrid.client.widget.CellTableDropDownDataValueMapProvider;

/**
 * A Popup drop-down Editor proxy that delegates operation to different implementations depending on whether
 * the cell should represent a list of values or single value. The need arose from incomplete dependent enumeration
 * definitions; e.g. Fact.field1=['A', 'B'] Fact.field2[field1=A]=['A1', 'A2']; where no dependent enumeration has
 * been defined for Fact.field2[field1=B]. In this scenario a TextBox for field2 should be shown when field1=B
 */
public class ProxyPopupNumericByteDropDownEditCell extends
                                                   AbstractProxyPopupDropDownEditCell<Byte, Byte> {

    public ProxyPopupNumericByteDropDownEditCell(final String factType,
                                                 final String factField,
                                                 final String operator,
                                                 final AsyncPackageDataModelOracle dmo,
                                                 final CellTableDropDownDataValueMapProvider dropDownManager,
                                                 final boolean isReadOnly) {
        super(factType,
              factField,
              operator,
              dmo,
              dropDownManager,
              isReadOnly);
    }

    @Override
    protected ProxyPopupDropDown<Byte> getSingleValueEditor() {
        return new AbstractProxyPopupDropDownTextBox<Byte>(TextBoxFactory.getTextBox(DataType.TYPE_NUMERIC_BYTE),
                                                           this) {
            @Override
            public String convertToString(final Byte value) {
                return ProxyPopupNumericByteDropDownEditCell.this.convertToString(value);
            }

            @Override
            public Byte convertFromString(final String value) {
                return ProxyPopupNumericByteDropDownEditCell.this.convertFromString(value);
            }
        };
    }

    @Override
    protected ProxyPopupDropDown<Byte> getMultipleValueEditor(final String operator) {
        return new AbstractProxyPopupDropDownListBox<Byte>(this, operator) {
            @Override
            public String convertToString(final Byte value) {
                return ProxyPopupNumericByteDropDownEditCell.this.convertToString(value);
            }

            @Override
            public Byte convertFromString(final String value) {
                return ProxyPopupNumericByteDropDownEditCell.this.convertFromString(value);
            }
        };
    }

    private String convertToString(final Byte value) {
        return (value == null ? null : value.toString());
    }

    private Byte convertFromString(final String value) {
        Byte number = null;
        if (value.length() > 0) {
            try {
                number = new Byte(value);
            } catch (NumberFormatException e) {
                number = Byte.valueOf("0");
            }
        }
        return number;
    }
}
