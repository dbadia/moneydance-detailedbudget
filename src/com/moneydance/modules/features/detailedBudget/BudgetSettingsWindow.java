package com.moneydance.modules.features.detailedBudget;

import java.awt.AWTEvent;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.AccountBook;
import com.infinitekind.moneydance.model.Budget;
import com.infinitekind.moneydance.model.BudgetList;
import com.infinitekind.util.CustomDateFormat;
import com.moneydance.apps.md.controller.FeatureModuleContext;
import com.moneydance.awt.AwtUtil;
import com.moneydance.awt.JDateField;

/** Window used for Account List interface
  ------------------------------------------------------------------------
*/

public class BudgetSettingsWindow 
  extends JFrame
{
  private static final long serialVersionUID = 1L;
  private final Main extension;
  private final JComboBox budgetName;
  private final JComboBox budgetPeriod;
  private final JDateField txtStartDate;
  private final JDateField txtEndDate;
  // Subtotal by week, fortnight, month, quarter, year, etc
  private final JComboBox subTotalBy;
  private final JCheckBox includeBudgetedInEachStep;
  private final JCheckBox includeDifferenceInEachStep;
  private final JCheckBox showAllAccounts;
  private final JCheckBox addSubtotalsForParentCategories;

  private final JButton generateButton;
  private final JButton closeButton;
  
  public BudgetSettingsWindow(final Main extension) {
    super("Budget Settings");
    this.extension = extension;
    System.out.println("Budget Settings");

    final JPanel p = new JPanel(new GridBagLayout());
    p.setBorder(new EmptyBorder(10,10,10,10));

    int row = 0;
    // Budget Names
    final JLabel lblBudgetName = new JLabel("Budget:");
    p.add(lblBudgetName,AwtUtil.getConstraints(0, row, 1, 1, 1, 1, false, false, GridBagConstraints.EAST,3));
    final List<String> budgetList = new ArrayList<String>();
    budgetList.add("ALL");
    
		System.err.println("starting");
		final FeatureModuleContext context = extension.getUnprotectedContext();
		System.err.println("context=" + context);
		final Account account = context.getRootAccount();
		System.err.println("account=" + account);
		final AccountBook accoutBook = context.getCurrentAccountBook();
		System.err.println("accoutBook=" + accoutBook);
		final BudgetList bList = accoutBook.getBudgets();
		System.err.println("bList=" + bList);
		for (final Budget budget : bList.getAllBudgets()) {
			System.err.println("budget=" + budget);
			budgetList.add(budget.getName());
		}
    budgetName = new JComboBox(budgetList.toArray());
    p.add(budgetName,AwtUtil.getConstraints(1, row, 1, 1, 1, 1, false, false, GridBagConstraints.WEST,3));
    row++;
    
    // Budget Period
    final JLabel lblBudgetPeriod = new JLabel("Period:");
    p.add(lblBudgetPeriod,AwtUtil.getConstraints(0, row, 1, 1, 1, 1, false, false, GridBagConstraints.EAST,3));
    final String[] periodList = new String[] {
    	"Month to Date",	
    	"Quarter to Date",
    	"Year to Date",
    	"This Month",	
    	"This Quarter",
    	"This Year",
    	"Last Month",	
    	"Last Quarter",
    	"Last Year",
    	"Custom"
    };
    budgetPeriod = new JComboBox(periodList);
    budgetPeriod.addActionListener(new ActionListener() {
		@Override
		public void actionPerformed(final ActionEvent e) {
			System.out.println("Update Dates");
			updateDates();
		}
	});
    p.add(budgetPeriod,AwtUtil.getConstraints(1, row, 1, 1, 1, 1, false, false, GridBagConstraints.WEST,3));
    row++;

    final CustomDateFormat cdf = new CustomDateFormat("dd/MM/yyyy");
    // Start Date
    final JLabel lblStartDate = new JLabel("Start Date:");
    p.add(lblStartDate,AwtUtil.getConstraints(0, row, 1, 1, 1, 1, false, false,GridBagConstraints.EAST,3));
    txtStartDate = new JDateField(cdf);
    p.add(txtStartDate,AwtUtil.getConstraints(1, row, 1, 1, 1, 1, false, false,GridBagConstraints.WEST,3));
    txtStartDate.setEnabled(false);
    row++;

    // End Date
    final JLabel lblEndDate = new JLabel("End Date:");
    p.add(lblEndDate,AwtUtil.getConstraints(0, row, 1, 1, 1, 1, false, false,GridBagConstraints.EAST,3));
    txtEndDate = new JDateField(cdf);
    p.add(txtEndDate,AwtUtil.getConstraints(1, row, 1, 1, 1, 1, false, false,GridBagConstraints.WEST,3));
    txtEndDate.setEnabled(false);
    row++;

    // Subtotal By
    final JLabel lblSubtotalBy = new JLabel("SubTotal By:");
    p.add(lblSubtotalBy,AwtUtil.getConstraints(0, row, 1, 1, 1, 1, false, false,GridBagConstraints.EAST,3));
    final String[] subTotalByList = new String[] {
    	"None",	
    	"Week",	
    	"Month",
    	"Year",	
    };
    subTotalBy = new JComboBox(subTotalByList);
    p.add(subTotalBy,AwtUtil.getConstraints(1, row, 1, 1, 1, 1, false, false,GridBagConstraints.WEST,3));
    row++;

    // Include Budgeted In each SubTotal
    final JLabel lblIncBudgeted = new JLabel("Budget with Subtotal");
    p.add(lblIncBudgeted,AwtUtil.getConstraints(0, row, 1, 1, 1, 1, false, false,GridBagConstraints.EAST,3));
    includeBudgetedInEachStep = new JCheckBox();
    p.add(includeBudgetedInEachStep,AwtUtil.getConstraints(1, row, 1, 1, 1, 1, false, false,GridBagConstraints.WEST,3));
    row++;

    // Include Difference In each SubTotal
    final JLabel lblIncDiff = new JLabel("Difference with Subtotal");
    p.add(lblIncDiff,AwtUtil.getConstraints(0, row, 1, 1, 1, 1, false, false,GridBagConstraints.EAST,3));
    includeDifferenceInEachStep = new JCheckBox();
    p.add(includeDifferenceInEachStep,AwtUtil.getConstraints(1, row, 1, 1, 1, 1, false, false,GridBagConstraints.WEST,3));
    row++;

    // Show All Accounts
    final JLabel lblShowAllAccounts = new JLabel("Show All Categories");
    p.add(lblShowAllAccounts,AwtUtil.getConstraints(0, row, 1, 1, 1, 1, false, false,GridBagConstraints.EAST,3));
    showAllAccounts = new JCheckBox();
    p.add(showAllAccounts,AwtUtil.getConstraints(1, row, 1, 1, 1, 1, false, false,GridBagConstraints.WEST,3));
    row++;
    
    // Add Subtotals For Parent Categories
    final JLabel lblAddSubtotalsForParentCategories = new JLabel("Subtotals For Parent Categories");
    p.add(lblAddSubtotalsForParentCategories,AwtUtil.getConstraints(0, row, 1, 1, 1, 1, false, false,GridBagConstraints.EAST,3));
    addSubtotalsForParentCategories = new JCheckBox();
    p.add(addSubtotalsForParentCategories,AwtUtil.getConstraints(1, row, 1, 1, 1, 1, false, false,GridBagConstraints.WEST,3));
    row++;
    
    p.add(Box.createVerticalStrut(8), AwtUtil.getConstraints(0,row,0,0,1,1,false,false));
    row++;
    // Buttons
    generateButton = new JButton("Generate");
    generateButton.addActionListener(new ActionListener() {
		@Override
		public void actionPerformed(final ActionEvent e) {
			generate();
		}
	});
    p.add(generateButton, AwtUtil.getConstraints(0,row,1,0,1,1,false,true));
    closeButton = new JButton("Close");
    closeButton.addActionListener(new ActionListener() {
		@Override
		public void actionPerformed(final ActionEvent e) {
			BudgetSettingsWindow.this.extension.closeConsole();
		}
	});
    p.add(closeButton, AwtUtil.getConstraints(1,row,1,0,1,1,false,true));
    row++;

    getContentPane().add(p);

    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    enableEvents(WindowEvent.WINDOW_CLOSING);

    setSize(400, 360);
    AwtUtil.centerWindow(this);
    
    budgetPeriod.setSelectedIndex(0);
    
    System.out.println("Done Init.");
  }
  
  /** Generate Detailed Budget Report. Open Window. */
  protected void generate() {
		final DetailedBudgetWindow win = new DetailedBudgetWindow(
				this.extension,
				(String)budgetName.getSelectedItem(),
				(String)budgetPeriod.getSelectedItem(),
				txtStartDate.getDate(),
				txtEndDate.getDate(),
				(String)subTotalBy.getSelectedItem(),
				includeBudgetedInEachStep.isSelected(),
				includeDifferenceInEachStep.isSelected(),
				showAllAccounts.isSelected(),
				addSubtotalsForParentCategories.isSelected()
				);
		win.setVisible(true);
		this.setVisible(false);
		this.dispose();
  }

/** Update Start and End dates after user selects Period */
  private void updateDates() {
	  boolean enable = false;
	  final Date now = new Date();
	  if (budgetPeriod.getSelectedItem().equals("Month to Date")) {
		  txtStartDate.setDate(DateUtil.getStartOfMonth(now));
		  txtEndDate.setDate(now);
	  }
	  else if (budgetPeriod.getSelectedItem().equals("Quarter to Date")) {
		  txtStartDate.setDate(DateUtil.getStartOfQuarter(now));
		  txtEndDate.setDate(now);
	  }
	  else if (budgetPeriod.getSelectedItem().equals("Year to Date")) {
		  txtStartDate.setDate(DateUtil.getStartOfYear(now));
		  txtEndDate.setDate(now);
	  }
	  else if (budgetPeriod.getSelectedItem().equals("This Month")) {
		  txtStartDate.setDate(DateUtil.getStartOfMonth(now));
		  txtEndDate.setDate(DateUtil.getEndOfMonth(now));
	  }
	  else if (budgetPeriod.getSelectedItem().equals("This Quarter")) {
		  txtStartDate.setDate(DateUtil.getStartOfQuarter(now));
		  txtEndDate.setDate(DateUtil.getEndOfQuarter(now));
	  }
	  else if (budgetPeriod.getSelectedItem().equals("This Year")) {
		  txtStartDate.setDate(DateUtil.getStartOfYear(now));
		  txtEndDate.setDate(DateUtil.getEndOfYear(now));
	  }
	  else if (budgetPeriod.getSelectedItem().equals("Last Month")) {
		  final Date dt = DateUtil.addMonths(now, -1);
		  txtStartDate.setDate(DateUtil.getStartOfMonth(dt));
		  txtEndDate.setDate(DateUtil.getEndOfMonth(dt));
	  }
	  else if (budgetPeriod.getSelectedItem().equals("Last Quarter")) {
		  final Date dt = DateUtil.addQuarters(now, -1);
		  txtStartDate.setDate(DateUtil.getStartOfQuarter(dt));
		  txtEndDate.setDate(DateUtil.getEndOfQuarter(dt));
	  }
	  else if (budgetPeriod.getSelectedItem().equals("Last Year")) {
		  final Date dt = DateUtil.addYears(now, -1);
		  txtStartDate.setDate(DateUtil.getStartOfYear(dt));
		  txtEndDate.setDate(DateUtil.getEndOfYear(dt));
	  }
	  else if (budgetPeriod.getSelectedItem().equals("Custom")) {
		  enable = true;
	  }
	  txtStartDate.setEnabled(enable);
	  txtEndDate.setEnabled(enable);
  }

  @Override
public final void processEvent(final AWTEvent evt) {
    if(evt.getID()==WindowEvent.WINDOW_CLOSING) {
      extension.closeConsole();
      return;
    }
    super.processEvent(evt);
  }
  
  void goAway() {
    setVisible(false);
    dispose();
  }
}
