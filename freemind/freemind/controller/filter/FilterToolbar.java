/*
 * Created on 05.05.2005
 * Copyright (C) 2005 Dimitri Polivaev
 */
package freemind.controller.filter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

import freemind.controller.Controller;
import freemind.controller.filter.condition.Condition;
import freemind.main.Resources;
import freemind.modes.MindMap;
import freemind.modes.MindMapNode;

class FilterToolbar extends JToolBar {
    private FilterController fc;
    private FilterDialog filterDialog = null;
    private JComboBox activeFilterCondition;
    private JToggleButton btnApply;
    private JCheckBox showAncestors;
    private JCheckBox showDescendants;
    private Filter activeFilter;
    private Filter inactiveFilter;
    private JButton btnEdit;
    private JButton btnUnfoldAncestors;
    private static Color filterActiveColor = null;
    private static Color filterInactiveColor = null;
    static private  final String FILTER_ON = Resources.getInstance().getResourceString("filter_on");
    static private  final String FILTER_OFF = Resources.getInstance().getResourceString("filter_off");
    
    private class ApplyFilterAction extends AbstractAction {
        
        /**
         *
         */
        private Controller c;
        ApplyFilterAction(Controller c) {
            super(FILTER_ON);
            this.c = c;
        }
        
        public void actionPerformed(ActionEvent e) {
            resetFilter();
            if (btnApply.isSelected() && getSelectedCondition() == null){
                btnEdit.doClick();
            }
            else
            {
                getFilter().applyFilter(c);
            }
            if(btnApply.isSelected()){
                if(filterInactiveColor == null)
                    filterInactiveColor = btnApply.getBackground();
                if(filterActiveColor == null)
                    filterActiveColor = new Color(255, 128, 128);
                btnApply.setBackground(filterActiveColor);
                btnApply.setText(FILTER_OFF);
            }
            else{
                btnApply.setBackground(filterInactiveColor);                
                btnApply.setText(FILTER_ON);
            }
            btnUnfoldAncestors.setEnabled(btnApply.getModel().isSelected());
        }
    }
    private class FilterChangeListener extends AbstractAction implements ItemListener{
        private Controller c;
        /* (non-Javadoc)
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        public FilterChangeListener(Controller c){
            this.c = c;
        }
        public void actionPerformed(ActionEvent arg0) {
            resetFilter();
            getFilter();
            MindMap map = c.getModel();
            map.nodeChanged((MindMapNode)map.getRoot());
            DefaultFilter.selectVisibleNode(c.getView());
        }
        /* (non-Javadoc)
         * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
         */
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED && btnApply.getModel().isSelected())
                resetFilter();
            getFilter().applyFilter(c);
        }
        
    }
    
    private class EditFilterAction extends AbstractAction {
        private Controller c;        
        private FilterToolbar ft;
        EditFilterAction(Controller c, FilterToolbar ft) {
            super(Resources.getInstance().getResourceString("filter_edit"));
            this.c = c;
            this.ft = ft;
        }
        /* (non-Javadoc)
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        private FilterDialog getFilterDialog() {
            if (filterDialog == null){
                filterDialog = new FilterDialog(c, ft);                
            }
            return filterDialog;
        }
        public void actionPerformed(ActionEvent arg0) {
            Object selectedItem = getActiveFilterConditionComboBox().getSelectedItem();
            if(selectedItem != null){
                getFilterDialog().setSelectedItem(selectedItem);
            }
            if(getFilterDialog().isVisible() == false){
                getFilterDialog().setLocationRelativeTo(Resources.getInstance().getJFrame());
                getFilterDialog().setVisible(true);
            }
        }
        
    }
    
    private class UnfoldAncestorsAction extends AbstractAction {
        private Controller c;
        /**
         *
         */
        UnfoldAncestorsAction(Controller c) {
            super("", new ImageIcon(Resources.getInstance().getResource("images/unfold.png")));
            this.c = c;
        }
        
        private void unfoldAncestors(MindMapNode parent) {
            for(Iterator i = parent.childrenUnfolded(); i.hasNext();) {
                MindMapNode node = (MindMapNode)i.next();
                if (showDescendants.isSelected()|| node.getFilterInfo().isAncestor() ){
                    setFolded(node, false);
                    unfoldAncestors(node) ;
                }
            }
        }
        
        private void setFolded(MindMapNode node, boolean state) {
            if(node.hasChildren() && (node.isFolded()!=state)) {
                c.getModeController().setFolded(node, state);
            }
        }
        public void actionPerformed(ActionEvent e) {
            if (getSelectedCondition() != null){
                unfoldAncestors((MindMapNode)c.getModel().getRoot());
            }
        }
    }
    
    FilterToolbar(final Controller c)
    {
        super();
        this.fc = c.getFilterController();
        setVisible(false);
        setFocusable(false);
        FilterChangeListener filterChangeListener = new FilterChangeListener(c);
        
        add(new JLabel(Resources.getInstance().getResourceString("filter_toolbar") + " "));
        
        activeFilterCondition = new JComboBox();
        activeFilterCondition.setFocusable(false);
        activeFilterCondition.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        activeFilterCondition.setRenderer(fc.getConditionRenderer());
        add(activeFilterCondition);
        activeFilterCondition.addItemListener(filterChangeListener);
        
        btnApply = new JToggleButton(new ApplyFilterAction(c));
        add(btnApply);
        
        btnEdit = new JButton(new EditFilterAction(c, this));
        add(btnEdit);
        
        btnUnfoldAncestors = new JButton(new UnfoldAncestorsAction(c));
        btnUnfoldAncestors.setToolTipText(Resources.getInstance().getResourceString("filter_unfold_ancestors"));
        btnUnfoldAncestors.setEnabled(false);
        add(btnUnfoldAncestors);
        
        showAncestors = new JCheckBox(Resources.getInstance().getResourceString("filter_show_ancestors"), true);
        add(showAncestors);
        showAncestors.getModel().addActionListener(filterChangeListener);
        
        showDescendants = new JCheckBox(Resources.getInstance().getResourceString("filter_show_descendants"), false);
        add(showDescendants);
        showDescendants.getModel().addActionListener(filterChangeListener);
        
        activeFilter = null;
        inactiveFilter = new DefaultFilter(null, false, false, false);
    }
    
    /**
     *
     */
    public void resetFilter() {
        activeFilter = null;
        
    }
    
    private Condition getSelectedCondition() {
        return (Condition)activeFilterCondition.getSelectedItem();
    }
    
    JComboBox getActiveFilterConditionComboBox() {
        return activeFilterCondition;
    }
    
    Filter getFilter(){
        if (isVisible())
        {
            if(activeFilter == null)
                activeFilter = new DefaultFilter(
                        getSelectedCondition(),
                        btnApply.getModel().isSelected(),
                        showAncestors.getModel().isSelected(),
                        showDescendants.getModel().isSelected()
                );
            fc.getMap().setFilter(activeFilter);
            return activeFilter;
        }
        fc.getMap().setFilter(inactiveFilter);
        return inactiveFilter;
    }
    
    /**
     * @return
     */
    public AbstractButton getBtnApply() {
        return btnApply;
    }
    
    FilterDialog getFilterDialog() {
        return filterDialog;
    }

    /**
     * @param filter
     */
    void mapChanged(MindMap newMap) {
        Filter filter = newMap.getFilter();
        if((filter == null || filter == inactiveFilter) && btnApply.isSelected()){
            btnApply.doClick();            
        }
        else {            
            if(filter != activeFilter){
                activeFilter = filter;
                activeFilterCondition.setSelectedItem(filter);
                if(filter != null){
                    showAncestors.setSelected(filter.areAncestorsShown());
                    showDescendants.setSelected(filter.areDescendantsShown());
                }
                else{                    
                    showAncestors.setSelected(false);
                    showDescendants.setSelected(false);
                }
            }
            if(filter != null && ! btnApply.isSelected())
                btnApply.doClick();            
        }
    }
}