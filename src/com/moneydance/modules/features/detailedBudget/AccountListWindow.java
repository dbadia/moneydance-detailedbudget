/************************************************************\
 *       Copyright (C) 2001 Appgen Personal Software        *
\************************************************************/

package com.moneydance.modules.features.detailedBudget;

import java.awt.AWTEvent;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import com.infinitekind.moneydance.model.Account;
import com.moneydance.awt.AwtUtil;

/** Window used for Account List interface
  ------------------------------------------------------------------------
*/

public class AccountListWindow 
  extends JFrame
  implements ActionListener
{
  private static final long serialVersionUID = 1L;
  
  private final Main extension;
  private final JTextArea accountListArea;
  private final JButton clearButton;
  private final JButton closeButton;
  private final JTextField inputArea;

  public AccountListWindow(final Main extension) {
    super("Account List Console");
    this.extension = extension;

    accountListArea = new JTextArea();
    
		final Account root = extension.getUnprotectedContext().getCurrentAccountBook().getRootAccount();
    final StringBuffer acctStr = new StringBuffer();
    if(root!=null) {
      addSubAccounts(root, acctStr);
    }
    accountListArea.setEditable(false);
    accountListArea.setText(acctStr.toString());
    inputArea = new JTextField();
    inputArea.setEditable(true);
    clearButton = new JButton("Clear");
    closeButton = new JButton("Close");

    final JPanel p = new JPanel(new GridBagLayout());
    p.setBorder(new EmptyBorder(10,10,10,10));
    p.add(new JScrollPane(accountListArea), AwtUtil.getConstraints(0,0,1,1,4,1,true,true));
    p.add(Box.createVerticalStrut(8), AwtUtil.getConstraints(0,2,0,0,1,1,false,false));
    p.add(clearButton, AwtUtil.getConstraints(0,3,1,0,1,1,false,true));
    p.add(closeButton, AwtUtil.getConstraints(1,3,1,0,1,1,false,true));
    getContentPane().add(p);

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    enableEvents(WindowEvent.WINDOW_CLOSING);
    closeButton.addActionListener(this);
    clearButton.addActionListener(this);
        
    /*PrintStream c = */new PrintStream(new ConsoleStream());

    setSize(500, 400);
    AwtUtil.centerWindow(this);
  }

  public static void addSubAccounts(final Account parentAcct, final StringBuffer acctStr) {
    final int sz = parentAcct.getSubAccountCount();
    for(int i=0; i<sz; i++) {
      final Account acct = parentAcct.getSubAccount(i);
      acctStr.append(acct.getFullAccountName());
      acctStr.append("\n");
      addSubAccounts(acct, acctStr);
    }
  }


  @Override
public void actionPerformed(final ActionEvent evt) {
    final Object src = evt.getSource();
    if(src==closeButton) {
      extension.closeConsole();
    }
    if(src==clearButton) {
      accountListArea.setText("");
    }
  }

  @Override
public final void processEvent(final AWTEvent evt) {
    if(evt.getID()==WindowEvent.WINDOW_CLOSING) {
      extension.closeConsole();
      return;
    }
    super.processEvent(evt);
  }
  
  private class ConsoleStream
    extends OutputStream
    implements Runnable
  {    
    @Override
	public void write(final int b)
      throws IOException
    {
      accountListArea.append(String.valueOf((char)b));
      repaint();
    }

    @Override
	public void write(final byte[] b)
      throws IOException
    {
      accountListArea.append(new String(b));
      repaint();
    }
    @Override
	public void run() {
      accountListArea.repaint();
    }
  }

  void goAway() {
    setVisible(false);
    dispose();
  }
}
