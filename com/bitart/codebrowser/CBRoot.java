/******************************************************************************
 *	Copyright 2002 BITart Gerd Knops. All rights reserved.
 *
 *	Project	: CodeBrowser
 *	File	: CBRoot.java
 *	Author	: Gerd Knops gerti@BITart.com
 *
 *******************************************************************************
 *                                    :mode=java:folding=indent:collapseFolds=1:
 *	History:
 *	020510 Creation of file
 *
 *******************************************************************************
 *
 *	Description:
 *	This is the root TreeNode for the CodeBrowser display.
 *	Here we take care of having the file parsed via ctags, and then
 *	we create child nodes as required.
 *
 *	$Id: CBRoot.java,v 1.1.1.1 2005/10/21 17:12:24 ezust Exp $
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

import java.util.*;
import java.io.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.tree.*;

import org.gjt.sp.jedit.*;

/*****************************************************************************/
public class CBRoot implements TreeNode
{
  /******************************************************************************
   * Vars
   ******************************************************************************/
  
	static final boolean DEBUG=false;
	
	Vector	children=null;
	
	/******************************************************************************
	 * Factory methods
	 ******************************************************************************/
	public CBRoot(String path, String fileName, String lang, String encoding)
	{
		parse(path, fileName, lang, encoding);
	}
	
	/******************************************************************************
	 * Implementation
	 ******************************************************************************/
	public void parse(String path, String fileName, String lang, String encoding)
	{
		if(DEBUG) System.err.println("Parsing "+path);
		children=new Vector();
		//if(lang.equals("text")) return;
		Hashtable cbTypes=new Hashtable();
		Vector tv=new Vector();
		
		boolean buildxml=false;
		
		if(path.toLowerCase().endsWith("build.xml")) buildxml=true;
		
		try
		{
			//System.err.println("Starting ctags...");
      // funa edit
      String upperEncoding = encoding.toUpperCase();
      String ctagsEncoding = "";
      if (!jEdit.getBooleanProperty("options.codebrowser.use_jcode", true)){
        ctagsEncoding = "";
      } else if (upperEncoding.indexOf("UTF-8") >= 0){
        ctagsEncoding = "utf8";
      } else if (
        upperEncoding.indexOf("MS932") >= 0 
        || upperEncoding.indexOf("SJIS") >= 0
        || upperEncoding.indexOf("SHIFT_JIS") >= 0
        || upperEncoding.indexOf("WINDOWS-31J") >= 0)
      {
        ctagsEncoding = "sjis";
      } else if (upperEncoding.indexOf("EUC") >= 0){
        ctagsEncoding = "euc";
      } else {
        ctagsEncoding = "";
      }
      
      if (upperEncoding.indexOf("NATIVE2ASCII") == 0){
        encoding = "ISO-8859-1";
      } else if (upperEncoding.indexOf("UTF-8") >= 0){
        // UTF-8YもUTF-8として処理する
        encoding = "UTF-8";
      }
      
      // System.out.println(encoding);
      // System.out.println(upperEncoding);
      // System.out.println(ctagsEncoding);
      
      String ctagsLang = getCtagsLang(lang, fileName);
      
      String[] args;
      
      if(!buildxml)
      {
        args=new String[]{
          jEdit.getProperty("options.codebrowser.ctags_path"),
          "--fields=KsSz",
          "--excmd=pattern",
          "--sort=no",
          "-f",
          "-",
          path
        };
      }
      else
      {
        args=new String[]{
          jEdit.getProperty("options.codebrowser.ctags_path"),
          "--fields=KsSz",
          "--excmd=pattern",
          "--sort=no",
          "--language-force=ant",
          "-f",
          "-",
          path
        };
        lang="ant";
      }
      // funa edit
      if (!ctagsEncoding.equals("")){
        String[] newArgs = new String[args.length + 1];
        System.arraycopy(args, 0, newArgs,0, 1);
        newArgs[1] = "--jcode="+ctagsEncoding;
        System.arraycopy(args, 1, newArgs,2, args.length - 1);
        args = newArgs;
      }
      
      if (!ctagsLang.equals("")){
        String[] newArgs = new String[args.length + 1];
        System.arraycopy(args, 0, newArgs,0, 1);
        newArgs[1] = "--language-force="+ctagsLang;
        System.arraycopy(args, 1, newArgs,2, args.length - 1);
        args = newArgs;
      }
      
      /*
      System.err.println("Args: ");
      for(int i=0;i<args.length;i++)
      {
      System.err.println("\t"+args[i]);
      }
      */
      Process p=Runtime.getRuntime().exec(args);
      BufferedReader in=new BufferedReader(new InputStreamReader(p.getInputStream(), encoding));
      //System.err.println("ctags started!");
      
      String line;
      while((line=in.readLine())!=null)
      {
        //System.err.println("Got line: "+line);
        // Get rid of crlf
        while(line.endsWith("\n") || line.endsWith("\r"))
        {
          line=line.substring(0,line.length()-1);
        }
        
        //
        // split off extension
        //
        int idx;
        idx=line.lastIndexOf(";\"\t");
        if(idx<0) continue;
        
        // extensions in Vector v, remove from line
        Vector v=split("\t",line.substring(idx+3));
        line=line.substring(0,idx);
        
        // Create a hash from extensions
        Hashtable info=new Hashtable();
        for(int i=0;i<v.size();i++)
        {
          String s=(String)v.elementAt(i);
          int ei=s.indexOf(':');
          if(ei<0) continue;
          info.put(s.substring(0,ei),s.substring(ei+1));
        }
        
        // item name
        idx=line.indexOf("\t");
        if(idx<0) continue;
        info.put("cb_tag_cb",line.substring(0,idx));
        line=line.substring(idx+1);
        
        // file name, not needed
        idx=line.indexOf("\t");
        if(idx<0) continue;
        //info.put("cb_file_cb",line.substring(0,idx));
        
        // pattern
        info.put("cb_pattern_cb",line.substring(idx+1));
        
        //System.err.println("Parsed into: "+info);
        
        String type=(String)info.get("kind");
        if(type==null || type.length()==0) continue;
        
        CBType t=(CBType)cbTypes.get(type);
        if(t==null)
        {
          t=new CBType(this,type,lang);
          cbTypes.put(type,t);
          tv.add(type);
        }
        t.add(info);
      }
    }
    catch(IOException ioe)
    {
      System.err.println(ioe);
    }
    //System.err.println("Done reading");
    
    
    Collections.sort(tv);
    for(int i=0;i<tv.size();i++)
    {
      children.add(cbTypes.get(tv.elementAt(i)));
    }
  }
  
  private String getCtagsLang(String mode, String fileName) {
    Vector<Vector<String>> mapping = CodeBrowserOptionPane.getMapping();
    
    for(int i = 0; i < mapping.size(); i++) {
      String mappingMode = mapping.get(i).get(CodeBrowserOptionPane.MappingModel.COL.MODE.ordinal());
      String mappingRegex = mapping.get(i).get(CodeBrowserOptionPane.MappingModel.COL.FILE_REGEX.ordinal());
      String mappingLang = mapping.get(i).get(CodeBrowserOptionPane.MappingModel.COL.LANG.ordinal());
      
      if (!"".equals(mappingMode) && !mappingMode.toLowerCase().equals(mode.toLowerCase())) {
        continue;
      }
      
      if (!"".equals(mappingRegex) && !Pattern.matches(mappingRegex, fileName)) {
        continue;
      }
      
      return mappingLang;
    }
    
    return "";
  }
  
  public Vector split(String where,String str)
  /***********************************************************************
   * Splits the String txt on occurances of str, returns a Vector
   * of Strings.
   * @param where The String to split on.
   * @param str The String to split.
   * @return A Vector of strings.
   ***********************************************************************/
  {
    Vector v=new Vector();
    
    int idx;
    
    while((idx=str.indexOf(where))>=0)
    {
      String s="";
      if(idx>0) s=str.substring(0,idx);
      v.addElement(s);
      str=str.substring(idx+where.length());
    }
    v.addElement(str);
    
    return v;
  }
  
  public void expandPaths(JTree tree)
  {
    Object[] objs={
      this,
      this
    };
    for(int i=children.size()-1;i>=0;i--)
    {
      CBType t=(CBType)children.elementAt(i);
      if(t.getState())
      {
        objs[1]=t;
        tree.expandPath(new TreePath(objs));
      }
    }
  }
  
  public void setSorted(boolean flag,JTree tree)
  {
    DefaultTreeModel tm=null;
    if(tree!=null) tm=(DefaultTreeModel)tree.getModel();
    
    for(int i=0;i<children.size();i++)
    {
      CBType t=(CBType)children.elementAt(i);
      t.setSorted(flag);
      if(tm!=null && t.getState())
      {
        int l=t.getChildCount();
        int[] idc=new int[l];
        for(int j=0;j<l;j++)
        {
          idc[j]=j;
        }
        
        tm.nodesChanged(t,idc);
      }
    }
  }
  
  /******************************************************************************
   * TreeNode interface
   ******************************************************************************/
  public Enumeration children()
  {
    return children.elements();
  }
  
  public boolean getAllowsChildren()
  {
    return true;
  }
  
  public TreeNode getChildAt(int index)
  {
    return (TreeNode)children.elementAt(index);
  }
  
  public int getChildCount()
  {
    return children.size();
  }
  
  public int getIndex(TreeNode child)
  {
    return children.indexOf(child);
  }
  
  public TreeNode getParent()
  {
    return null;
  }
  
  public boolean isLeaf()
  {
    return false;
  }
}
/*************************************************************************EOF*/

