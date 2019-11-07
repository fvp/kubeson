/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package com.sun.javafx.scene.control.behavior;

import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.UP;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.ComboBox;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TableComboBox;

public class TableComboBoxListViewBehavior<T> extends ComboBoxBaseBehavior<T> {

    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /***************************************************************************
     *                                                                         *
     * Key event handling                                                      *
     *                                                                         *
     **************************************************************************/

    protected static final List<KeyBinding> COMBO_BOX_BINDINGS = new ArrayList<KeyBinding>();

    static {
        COMBO_BOX_BINDINGS.add(new KeyBinding(UP, KEY_PRESSED, "selectPrevious"));
        COMBO_BOX_BINDINGS.add(new KeyBinding(DOWN, "selectNext"));
        COMBO_BOX_BINDINGS.addAll(COMBO_BOX_BASE_BINDINGS);
    }

    /**
     *
     */
    public TableComboBoxListViewBehavior(final TableComboBox<T> comboBox) {
        super(comboBox, COMBO_BOX_BINDINGS);
    }

    @Override
    protected void callAction(String name) {
        if ("selectPrevious".equals(name)) {
            selectPrevious();
        } else if ("selectNext".equals(name)) {
            selectNext();
        } else {
            super.callAction(name);
        }
    }

    private ComboBox<T> getComboBox() {
        return (ComboBox<T>) getControl();
    }

    private void selectPrevious() {
        SelectionModel<T> sm = getComboBox().getSelectionModel();
        if (sm == null) {
            return;
        }
        sm.selectPrevious();
    }

    private void selectNext() {
        SelectionModel<T> sm = getComboBox().getSelectionModel();
        if (sm == null) {
            return;
        }
        sm.selectNext();
    }
}
