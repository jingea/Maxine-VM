/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.ins.memory;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.object.*;
import com.sun.max.ins.value.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.value.*;


/**
 * A table specialized for displaying a range of memory words in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class MemoryWordsTable extends InspectorTable {

    //private final ObjectInspector _objectInspector;
    private final Address startAddress;
    private final int wordCount;
    private final MemoryWordsTableModel model;
    private final MemoryWordsColumnModel columnModel;
    private final TableColumn[] columns;

    private MaxVMState lastRefreshedState = null;

    public MemoryWordsTable(final ObjectInspector objectInspector, TeleObject teleObject) {
        this(objectInspector, teleObject.getCurrentOrigin(), teleObject.getCurrentSize().toInt());

    }

    public MemoryWordsTable(final ObjectInspector objectInspector, Address startAddress, int wordCount) {
        super(objectInspector.inspection());
        //_objectInspector = objectInspector;
        this.startAddress = startAddress.wordAligned();
        this.wordCount = wordCount;

        model = new MemoryWordsTableModel();
        columns = new TableColumn[MemoryWordsColumnKind.VALUES.length()];
        columnModel = new MemoryWordsColumnModel();

        setModel(model);
        setColumnModel(columnModel);
        setShowHorizontalLines(style().defaultTableShowHorizontalLines());
        setShowVerticalLines(style().defaultTableShowVerticalLines());
        setIntercellSpacing(style().defaultTableIntercellSpacing());
        setRowHeight(style().defaultTableRowHeight());
        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        refresh(true);
        JTableColumnResizer.adjustColumnPreferredWidths(this);
    }

    public void refresh(boolean force) {
        if (maxVMState().newerThan(lastRefreshedState) || force) {
            lastRefreshedState = maxVMState();
            model.refresh();
            for (TableColumn column : columns) {
                final Prober prober = (Prober) column.getCellRenderer();
                prober.refresh(force);
            }
        }
    }

    public void redisplay() {
        for (TableColumn column : columns) {
            final Prober prober = (Prober) column.getCellRenderer();
            prober.redisplay();
        }
        invalidate();
        repaint();
    }

    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            @Override
            public String getToolTipText(MouseEvent mouseEvent) {
                final Point p = mouseEvent.getPoint();
                final int index = columnModel.getColumnIndexAtX(p.x);
                final int modelIndex = columnModel.getColumn(index).getModelIndex();
                return MemoryWordsColumnKind.VALUES.get(modelIndex).toolTipText();
            }
        };
    }

    private final class MemoryWordsColumnModel extends DefaultTableColumnModel {

        private final MemoryWordsViewPreferences localPreferences;

        private MemoryWordsColumnModel() {
            localPreferences = new MemoryWordsViewPreferences(MemoryWordsViewPreferences.globalPreferences(inspection())) {
                @Override
                public void setIsVisible(MemoryWordsColumnKind columnKind, boolean visible) {
                    super.setIsVisible(columnKind, visible);
                    final int col = columnKind.ordinal();
                    if (visible) {
                        addColumn(columns[col]);
                    } else {
                        removeColumn(columns[col]);
                    }
                    fireColumnPreferenceChanged();
                }
            };
            createColumn(MemoryWordsColumnKind.TAG, new TagRenderer(inspection()));
            createColumn(MemoryWordsColumnKind.ADDRESS, new AddressRenderer(inspection()));
            createColumn(MemoryWordsColumnKind.POSITION, new PositionRenderer(inspection()));
            createColumn(MemoryWordsColumnKind.VALUE, new ValueRenderer(inspection()));
            createColumn(MemoryWordsColumnKind.REGION, new RegionRenderer(inspection()));
        }

        private void createColumn(MemoryWordsColumnKind columnKind, TableCellRenderer renderer) {
            final int col = columnKind.ordinal();
            columns[col] = new TableColumn(col, 0, renderer, null);
            columns[col].setHeaderValue(columnKind.label());
            columns[col].setMinWidth(columnKind.minWidth());
            if (localPreferences.isVisible(columnKind)) {
                addColumn(columns[col]);
            }
            columns[col].setIdentifier(columnKind);
        }
    }

    private final class MemoryWordsTableModel extends AbstractTableModel {

        public MemoryWordsTableModel() {

        }

        void refresh() {


            fireTableDataChanged();
        }

        public int getColumnCount() {
            return MemoryWordsColumnKind.VALUES.length();
        }

        public int getRowCount() {
            return wordCount;
        }

        public Object getValueAt(int row, int col) {
            return row;
        }

        @Override
        public Class< ? > getColumnClass(int c) {
            return Integer.class;
        }

        public void redisplay() {
//            for (MemoryRegionDisplay memoryRegionData : _sortedMemoryWords) {
//                memoryRegionData.redisplay();
//            }
        }

    }

    private final class TagRenderer extends PlainLabel implements TableCellRenderer {

        TagRenderer(Inspection inspection) {
            super(inspection, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText("Tag(" + row + ")");
            return this;
        }

    }

    private final class AddressRenderer extends LocationLabel.AsAddressWithPosition implements TableCellRenderer {

        AddressRenderer(Inspection inspection) {
              super(inspection, 0, startAddress);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(startAddress.plus(row * maxVM().wordSize()).toInt());
            return this;
        }
    }

    private final class PositionRenderer extends LocationLabel.AsPosition implements TableCellRenderer {

        public PositionRenderer(Inspection inspection) {
            super(inspection, 0, startAddress);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setValue(row * maxVM().wordSize());
            return this;
        }
    }

    private final class ValueRenderer extends WordValueLabel implements TableCellRenderer {
        // Designed so that we only read memory lazily, for words that are visible
        ValueRenderer(Inspection inspection) {
            super(inspection, WordValueLabel.ValueMode.WORD, null);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
            setValue(WordValue.from(maxVM().readWord(startAddress.plus(row * maxVM().wordSize()))));
            return this;
        }
    }

    private final class RegionRenderer extends MemoryRegionValueLabel implements TableCellRenderer {
        // Designed so that we only read memory lazily, for words that are visible
        RegionRenderer(Inspection inspection) {
            super(inspection);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, final int row, int column) {
            setValue(WordValue.from(maxVM().readWord(startAddress.plus(row * maxVM().wordSize()))));
            return this;
        }
    }
}
