/******************************************************************************
 *	Copyright 2002 BITart Gerd Knops. All rights reserved.
 *
 *	Project	: CodeBrowser
 *	File	: CodeBrowserOptionPane.java
 *	Author	: Gerd Knops gerti@BITart.com
 *
 *******************************************************************************
 *                                    :mode=java:folding=indent:collapseFolds=1:
 *	History:
 *	020511 Creation of file
 *
 *******************************************************************************
 *
 *	Description:
 *	Simple option pane that lets the user set the path to the ctags binary
 *
 *	$Id: CodeBrowserOptionPane.java,v 1.1.1.1 2005/10/21 17:12:24 ezust Exp $
 *
 *******************************************************************************
 *
 * DISCLAIMER
 *
 * BITart and Gerd Knops make no warranties, representations or commitments
 * with regard to the contents of this software. BITart and Gerd Knops
 * specifically disclaim any and all warranties, wether express, implied or
 * statutory, including, but not limited to, any warranty of merchantability
 * or fitness for a particular purpose, and non-infringement. Under no
 * circumstances will BITart or Gerd Knops be liable for loss of data,
 * special, incidental or consequential damages out of the use of this
 * software, even if those damages were forseeable, or BITart or Gerd Knops
 * was informed of their potential.
 *
 ******************************************************************************/
package com.bitart.codebrowser;
/******************************************************************************
 * Imports
 ******************************************************************************/

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import org.gjt.sp.jedit.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import java.util.Vector;

/*****************************************************************************/
public class CodeBrowserOptionPane extends AbstractOptionPane 
{
  /******************************************************************************
   * Vars
   ******************************************************************************/
  
  private JTextField     ctagsPathTF;
  private JTextField		parserHistoryTextField;
  private JCheckBox		  use_jcode;
  private JCheckBox		  use_temporary;
  private MappingModel		  mappingModel;
	
  /******************************************************************************
   * Factory methods
   ******************************************************************************/
  public CodeBrowserOptionPane() 
	{
	  super("codebrowser");
	  setBorder(new EmptyBorder(5,5,5,5));
		
		JTextArea ta=new JTextArea(jEdit.getProperty("options.codebrowser.ctags_path_note"),0,60);
		ta.setEditable(false);
		ta.setLineWrap(true);
		ta.setWrapStyleWord(true);
		ta.setBackground(Color.yellow);
		
		addComponent(ta);
		
		addSeparator();
		
		addComponent(
			jEdit.getProperty("options.codebrowser.ctags_path_label"),
			ctagsPathTF=new JTextField(
				jEdit.getProperty("options.codebrowser.ctags_path"),
				40
				)
			);
		
		addComponent(
			jEdit.getProperty("options.codebrowser.parser_history"),
			parserHistoryTextField=new JTextField(
				jEdit.getProperty("options.codebrowser.parser_history.value"),
				10
				)
			);
		use_jcode = new JCheckBox(
		  jEdit.getProperty("options.codebrowser.use_jcode_label"),
		  jEdit.getBooleanProperty("options.codebrowser.use_jcode", true));
		addComponent(use_jcode);
		use_temporary = new JCheckBox(
		  jEdit.getProperty("options.codebrowser.use_temporary_label"),
		  jEdit.getBooleanProperty("options.codebrowser.use_temporary", false));
		addComponent(use_temporary);
		
		mappingModel = new MappingModel();
		JTable table = new JTable(mappingModel);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getTableHeader().setReorderingAllowed(false);
		JPanel tablePanel = new JPanel();
		MappingListener listener = new MappingListener(table, mappingModel);
		
		Vector<Vector<String>> mapping = getMapping();
		for(int i = 0; i < mapping.size(); i++) {
		  mappingModel.addRow(mapping.get(i));
		}
		
		tablePanel.setLayout(new BorderLayout());
		tablePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Language Mapping"));
		
		JPanel buttonPanel = new JPanel();
    JButton addButton = new JButton("Add");
    addButton.addActionListener(listener);
    buttonPanel.add(addButton);
    JButton removeButton = new JButton("Remove");
    removeButton.addActionListener(listener);
    buttonPanel.add(removeButton);
    tablePanel.add(buttonPanel, BorderLayout.SOUTH);
    
    JScrollPane scrollPane = new JScrollPane(table);
		tablePanel.add(scrollPane, BorderLayout.CENTER);
		Dimension d = tablePanel.getPreferredSize();
		d.height = 300;
		tablePanel.setPreferredSize(d);
		
		addComponent(tablePanel, GridBagConstraints.BOTH);
		
		// テーブル部の高さ自動調整も可能だが、
		// そこまでする必要もないかな。とりあえず、高さ固定
		// setLayout(new BorderLayout())をしても良い
		// GridBagConstraints cons = new GridBagConstraints();
    // cons.gridy = (this.y++);
    // cons.gridheight = 1;
    // cons.gridwidth = 0;
    // cons.fill = 1;
    // cons.weightx = 1.0D;
    // cons.weighty = 1.0D;
    // this.gridBag.setConstraints(tablePanel, cons);
    // add(tablePanel);
		
 		addSeparator();
 	}
 	
 	/******************************************************************************
 	 * Implementation
 	 ******************************************************************************/
 	public void _save() 
	{
		jEdit.setProperty("options.codebrowser.ctags_path", ctagsPathTF.getText());
		try {
			Integer.parseInt(parserHistoryTextField.getText());
			jEdit.setProperty("options.codebrowser.parser_history.value",
				parserHistoryTextField.getText());
		}
		catch(NumberFormatException e)
		{
			// don't allow an invalid property
			jEdit.resetProperty("options.codebrowser.parser_history.value");
		}
		jEdit.setBooleanProperty("options.codebrowser.use_jcode", use_jcode.isSelected());
		jEdit.setBooleanProperty("options.codebrowser.use_temporary", use_temporary.isSelected());
		
		clearMappingProperties();
		int count = mappingModel.getRowCount();
		int index = 0;
		for(int i = 0; i < count; i++) {
		  String mode = mappingModel.getValueAt(i, MappingModel.COL.MODE.ordinal()).toString();
		  String regex = mappingModel.getValueAt(i, MappingModel.COL.FILE_REGEX.ordinal()).toString();
		  String lang = mappingModel.getValueAt(i, MappingModel.COL.LANG.ordinal()).toString();
		  if ("".equals(lang)) {
		    continue;
		  }
		  if ("".equals(mode) && "".equals(regex)) {
		    continue;
	}
	
		  jEdit.setProperty("options.codebrowser.mapping."+index+".mode", mode);
		  jEdit.setProperty("options.codebrowser.mapping."+index+".regex", regex);
		  jEdit.setProperty("options.codebrowser.mapping."+index+".lang", lang);
		  index++;
		}
	  
	}
	    
	public static Vector<Vector<String>> getMapping() {
	  int index = 0;
	  Vector<Vector<String>> mapping = new Vector<Vector<String>>();
		while(true) {
		  String value = null;
		  Vector<String> row = new Vector<String>();
		  value = jEdit.getProperty("options.codebrowser.mapping."+index+".mode");
		  if (value == null) {
		    break;
		  }
		  row.add(value);
		  value = jEdit.getProperty("options.codebrowser.mapping."+index+".regex");
		  if (value == null) {
		    break;
		  }
		  row.add(value);
		  value = jEdit.getProperty("options.codebrowser.mapping."+index+".lang");
		  if (value == null) {
		    break;
	  }
		  row.add(value);
		  mapping.add(row);
		  index++;
		}
	  
		return mapping;
	  }
	  
	private void clearMappingProperties() {
	  int index = 0;
	  while(true) {
		  String value = null;
		  value = jEdit.getProperty("options.codebrowser.mapping."+index+".mode");
		  if (value == null) {
		    break;
		  }
		  jEdit.unsetProperty("options.codebrowser.mapping."+index+".mode");
		  
		  value = jEdit.getProperty("options.codebrowser.mapping."+index+".regex");
		  if (value == null) {
		    break;
      }
		  jEdit.unsetProperty("options.codebrowser.mapping."+index+".regex");
		  
		  value = jEdit.getProperty("options.codebrowser.mapping."+index+".lang");
		  if (value == null) {
		    break;
    }
		  jEdit.unsetProperty("options.codebrowser.mapping."+index+".regex");
	  
		  index++;
		}
	    }
	
	static class MappingListener implements ActionListener {
	  private JTable table;
	  private MappingModel model;
	  private MappingListener(JTable table, MappingModel model) {
	    this.table = table;
	    this.model = model;
	    }
	    
	  public void actionPerformed(ActionEvent evt) {
	    String command = evt.getActionCommand();
	    
	    if ("Add".equals(command)) {
	      Object[] data = new Object[]{"","",""};
	      model.addRow(data);
	    } else if ("Remove".equals(command)) {
	      int modelIndex = table.convertRowIndexToModel(table.getSelectedRow());
	      if (modelIndex >= 0) {
	        model.removeRow(modelIndex);
	  }
	    }
	  }
	}
	  
	static class MappingModel extends DefaultTableModel {
	  public enum COL {
	    MODE,
	    FILE_REGEX,
	    LANG
	  };
	  
	  public MappingModel() {
	    super(new Object[][]{}, new Object[]{"Edit Mode", "File Regex", "Ctags Language"});
	  }
	  
	  public boolean isCellEditable(int row, int col) {
	    return true;
	  }
	 
	}
}
/*************************************************************************EOF*/

