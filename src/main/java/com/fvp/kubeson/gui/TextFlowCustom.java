package com.fvp.kubeson.gui;

import com.sun.javafx.tk.quantum.QuantumToolkit;
import javafx.scene.Node;
import javafx.scene.text.TextFlow;

public class TextFlowCustom extends TextFlow {

    public TextFlowCustom() {
        super();
        QuantumToolkit q = new QuantumToolkit();
    }

    public TextFlowCustom(Node... children) {
        super(children);
    }

/*
    @Override
    TextLayout getTextLayout() {
        return super.getTextLayout();
    }
*/
}
