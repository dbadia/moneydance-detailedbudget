package com.moneydance.modules.features.detailedBudget;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import com.infinitekind.moneydance.model.AbstractTxn;
import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.Account.AccountType;
import com.infinitekind.moneydance.model.Budget;
import com.infinitekind.moneydance.model.BudgetItem;
import com.infinitekind.moneydance.model.BudgetList;
import com.infinitekind.moneydance.model.TransactionSet;
import com.moneydance.apps.md.controller.Util;
import com.moneydance.awt.AwtUtil;

/** Detailed Budget.
 * Can include subtotals per week, month, year.
 * With subtotals it will always show Actual Amount. You can
 * include the budgeted Amount with that, and the difference.
 * Does NOT give budgeted amount for an item if not in
 * the budgeted period (ie does not do what MoneyDance Budget report, 
 * add per year and divide per period).
 * */
public class DetailedBudgetWindow extends JFrame {
	private static final long serialVersionUID = 1L;
	private final Main extension;
	private final String budgetName;
	private final String budgetPeriod;
	private final Date startDate;
	private final Date endDate;
	private final String subTotalBy;
	private final boolean budgetWithSubtotal;
	private final boolean diffWithSubtotal;
	private final boolean showAllAccounts;
	private final boolean subtotalsForParentCategories;
	
	private final JEditorPane txtReport;
	private final JButton printButton;
	private final JButton saveButton;
	private final JButton closeButton;

	/** Categories to show in report*/
	private List<Account> categories = null;
	
	public static final DecimalFormat CURR_FMT = new DecimalFormat("#,##0");
	public static final DecimalFormat CENTS_FMT = new DecimalFormat("00");
	public static final SimpleDateFormat DT_FMT = new SimpleDateFormat("yyyy-MM-dd");
	
	public static final int START_DAY_OF_WEEK = Calendar.MONDAY;
	
	public static final int INCOME_ACCOUNTS = 0;
	public static final int EXPENSE_ACCOUNTS = 1;
	public static final int DIFF_ACCOUNTS = 2; // Income - Expense
	
	// -------------------------------------------
	
	/**
	 * Detailed Budget Report
	 * @param budgetName
	 * @param budgetPeriod
	 * @param startDate
	 * @param endDate
	 * @param subTotalBy
	 * @param budgetWithSubtotal
	 * @param diffWithSubtotal
	 */
	public DetailedBudgetWindow(final Main extension, final String budgetName, final String budgetPeriod,
			final Date startDate, final Date endDate, final String subTotalBy,
			final boolean budgetWithSubtotal, final boolean diffWithSubtotal,
			final boolean showAllAccounts,
			final boolean subtotalsForParentCategories) {
	    super("Detailed Budget");
//	    System.out.println("Detailed Budget");
	    this.extension = extension;
	    this.budgetName = budgetName;
	    this.budgetPeriod = budgetPeriod;
	    this.startDate = startDate;
	    this.endDate = endDate;
	    this.subTotalBy = subTotalBy;
	    this.budgetWithSubtotal = budgetWithSubtotal;
	    this.diffWithSubtotal = diffWithSubtotal;
	    this.showAllAccounts = showAllAccounts;
	    this.subtotalsForParentCategories = subtotalsForParentCategories;

	    // Get a list of all categories
		categories = getCategories();

	    final JPanel p = new JPanel(new GridBagLayout());
	    p.setBorder(new EmptyBorder(10,10,10,10));

	    // Text Area
	    txtReport = new JEditorPane();
	    txtReport.setEditable(false);
	    txtReport.setContentType("text/html");
	    txtReport.setText(getReportStr());
	    p.add(new JScrollPane(txtReport), AwtUtil.getConstraints(0,0,1,1,4,1,true,true));
	    p.add(Box.createVerticalStrut(8), AwtUtil.getConstraints(0,2,0,0,1,1,false,false));
	    printButton = new JButton("Print");
	    printButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				print();
			}
		});
	    p.add(printButton, AwtUtil.getConstraints(0,3,1,0,1,1,false,true));
	    saveButton = new JButton("Save");
	    saveButton.addActionListener(new ActionListener() {
	    	@Override
				public void actionPerformed(final ActionEvent e) {
	    		save();
	    	}
	    });
	    p.add(saveButton, AwtUtil.getConstraints(1,3,1,0,1,1,false,true));
	    closeButton = new JButton("Close");
	    closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				close();
			}
		});
	    p.add(closeButton, AwtUtil.getConstraints(2,3,1,0,1,1,false,true));
	    
	    getContentPane().add(p);

	    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	    enableEvents(WindowEvent.WINDOW_CLOSING);

	    setSize(600, 500);
	    AwtUtil.centerWindow(this);

//		System.out.println("Done Init DB.");
	}

	/**
	 * Print Report
	 */
	protected void print() {
		final DocumentRenderer dr = new DocumentRenderer();
		dr.print(txtReport);
	}

	/** Get the Report based on parameters given.
	 * Will be as HTML format. */
	private String getReportStr() {
//		System.out.println("getReportStr");
		final StringBuffer sb = new StringBuffer();
		// Start
		sb.append("<HTML>");
		// Heading
		sb.append("<font size=5><strong>Detailed Budget Report</strong></font><br><br>");
		sb.append("<font size=4><strong>").append(getBudgetPeriodStr());
		sb.append("</strong></font><br>");
		if (!subTotalBy.equals("None")) {
			sb.append("<font size=4><strong>");
			sb.append("Subtotal by ").append(subTotalBy);
			sb.append("</strong></font><br>");
		}
		final SimpleDateFormat pdf = new SimpleDateFormat("d MMM yyyy");
		sb.append("Date: <strong>").append(pdf.format(new Date())).append("</strong><br>");
		sb.append("Budget: <strong>").append(budgetName).append("</strong><br>");
		sb.append("Period: <strong>").append(pdf.format(startDate)).
			append("</strong> to <strong>").append(pdf.format(endDate)).append("</strong><br><br>");
		// Body
		
		// Get Subtotal times
		final List<DetailedBudgetColumn> columns = getDetailedBudgetColumns(startDate,endDate);
		
		// Fill Actual and Budgeted Amounts for each column
		for (final Iterator<DetailedBudgetColumn> iterator = columns.iterator(); iterator.hasNext();) {
			final DetailedBudgetColumn col = iterator.next();
			
			// Map of int (account number) to DetailedBudgetItem
			col.detIncomeItems =  getDetailedBudgetItems(col.startDay, col.endDay, INCOME_ACCOUNTS);
			col.detExpenseItems =  getDetailedBudgetItems(col.startDay, col.endDay, EXPENSE_ACCOUNTS);
		}
		
		// Number of table columns in subtotals
		final int numSubTotalCols = getNumSubtotalsColumns();
		
		sb.append("<table border=\"1\">\n");
		// First line of header
		sb.append("<tr><td align=\"center\"><strong>Item</strong></td>");
		if (columns.size() > 1) {
			for (final Iterator<DetailedBudgetColumn> iterator = columns.iterator(); iterator.hasNext();) {
				final DetailedBudgetColumn col = iterator.next();
				sb.append("<td align=\"center\" colspan="+numSubTotalCols+"><strong>"+DT_FMT.format(col.startDay)+
						" - "+DT_FMT.format(col.endDay)+"</strong></td>");
			}
			final int lastSpan = 3;
			sb.append("<td align=\"center\" colspan="+lastSpan+"><strong>TOTAL</strong></td>");
			sb.append("</tr>\n");
			// Second line of header
			sb.append("<tr><td>&nbsp</td>\n");
			if (columns.size() > 1) {
				for (final Iterator<DetailedBudgetColumn> iterator = columns.iterator(); iterator.hasNext();) {
					/*DetailedBudgetColumn col = (DetailedBudgetColumn) */iterator.next();
					if (budgetWithSubtotal || columns.size() == 1) {
						sb.append("<td align=\"center\"><strong>Budget</strong></td>");
					}
					sb.append("<td align=\"center\"><strong>Actual</strong></td>");
					if (diffWithSubtotal || columns.size() == 1) {
						sb.append("<td align=\"center\"><strong>Diff</strong></td>");
					}
				}
			}
		}
		
		sb.append("<td align=\"center\"><strong>Budget</strong></td>");
		sb.append("<td align=\"center\"><strong>Actual</strong></td>");
		sb.append("<td align=\"center\"><strong>Diff</strong></td>");
		sb.append("</tr>\n");

		sb.append(getCategoriesHTML(columns,INCOME_ACCOUNTS));
		if (subtotalsForParentCategories) {
			sb.append("<tr><td colspan="+getNumTableColumns(columns)+">&nbsp;</td></tr>\n");
		}
		sb.append(getCategoriesHTML(columns,EXPENSE_ACCOUNTS));
		sb.append("<tr><td colspan="+getNumTableColumns(columns)+">&nbsp;</td></tr>\n");
		
		sb.append(getCategoriesTotalHTML(columns,INCOME_ACCOUNTS));
		sb.append(getCategoriesTotalHTML(columns,EXPENSE_ACCOUNTS));
		sb.append(getCategoriesTotalHTML(columns,DIFF_ACCOUNTS));
		
		sb.append("</table>");
		
		// End
		sb.append("</HTML>");
//		System.out.println("Done getReportStr.");
		return sb.toString();
	}
	
	private String getBudgetPeriodStr() {
		final Date now = startDate;
		final SimpleDateFormat MONTH_DF = new SimpleDateFormat("MMMM yyyy");
		final SimpleDateFormat YEAR_DF = new SimpleDateFormat("yyyy");
		if (budgetPeriod.equals("Month to Date")) {
			return MONTH_DF.format(now) + " to Date";
		}
		if (budgetPeriod.equals("Quarter to Date")) {
			return YEAR_DF.format(now) + " Quarter " + DateUtil.getQuarterNum(now) + " to Date";
		}
		if (budgetPeriod.equals("Year to Date")) {
			return YEAR_DF.format(now) + " to Date";
		}
		if (budgetPeriod.equals("This Month")) {
			return MONTH_DF.format(now);
		}
		if (budgetPeriod.equals("This Quarter")) {
			return YEAR_DF.format(now) + " Quarter " + DateUtil.getQuarterNum(now);
		}
		if (budgetPeriod.equals("This Year")) {
			return YEAR_DF.format(now);
		}
		if (budgetPeriod.equals("Last Month")) {
			return MONTH_DF.format(now);
		}
		if (budgetPeriod.equals("Last Quarter")) {
			return YEAR_DF.format(now) + " Quarter " + DateUtil.getQuarterNum(now);
		}
		if (budgetPeriod.equals("Last Year")) {
			return YEAR_DF.format(now);
		}
		if (budgetPeriod.equals("Custom")) {
			return "Custom";
		}
		
		return "";
	}

	/**
	 * Number of Report Columns for each subtotal
	 * @return
	 */
	public int getNumSubtotalsColumns() {
		int numSubTotalCols = 1;
		if (budgetWithSubtotal) {
			numSubTotalCols++;
		}
		if (diffWithSubtotal) {
			numSubTotalCols++;
		}
		return numSubTotalCols;
	}
	
	/**
	 * Total number of columns for report row
	 * @param columns
	 * @return
	 */
	public int getNumTableColumns(final List<DetailedBudgetColumn> columns) {
		int i = 4;
		if (columns.size() > 1) {
			i += getNumSubtotalsColumns() * columns.size();
		}
		if (subtotalsForParentCategories) {
			i++;
		}

		return i;
	}

	/** Categories HTML for Income or Expenses */
	public String getCategoriesHTML(final List<DetailedBudgetColumn> columns, final int type) {
		final StringBuffer sbo = new StringBuffer();
		sbo.append("<tr><td colspan="+getNumTableColumns(columns)+"><strong>");
		if (type == INCOME_ACCOUNTS) {
			sbo.append("INCOME");
		} else if (type == EXPENSE_ACCOUNTS) {
			sbo.append("EXPENSE");
		}
		sbo.append("</strong></td></tr>");
		
		// Categories
		Account lastParentAccount = null;
		// Array of longs for subtotals
		Map<Integer, Long> parentSubtotalMap = new HashMap<Integer, Long>();
		Map<Integer, Long> lastParentSubtotalMap = new HashMap<Integer, Long>();
		boolean lastParentHasValues = false;
		for (final Iterator<Account> iterator = categories.iterator(); iterator.hasNext();) {
			int parentSubPtr = 0;
			final Account account = iterator.next();
			final StringBuffer sb = new StringBuffer();
			final StringBuffer sbBefore = new StringBuffer();
			if (account == null) {
				continue;
			}
			// Only accept income or expense accounts
			if (type == INCOME_ACCOUNTS && account.getAccountType() != AccountType.INCOME) {
				continue;
			} else if (type == EXPENSE_ACCOUNTS && account.getAccountType() != AccountType.EXPENSE) {
				continue;
//			System.out.println("  account="+account.getAccountName()+" "+account);
			}
			
			// Do we add Parent Account row?
			int indent = 0;
			if (subtotalsForParentCategories) {
				indent = account.getPath().size() - 2;

				final Account parentAccount = account.getPath().get(0);
				if (lastParentAccount != null && !lastParentAccount.equals(parentAccount) && lastParentHasValues) {
					lastParentSubtotalMap = parentSubtotalMap;
					parentSubtotalMap = new HashMap<Integer, Long>();
					lastParentHasValues = false;
				}
			}

			final Integer accNum = new Integer(account.getAccountNum());
			// Account Name
			sb.append("<tr><td"+getIndentStyle(indent)+">");
			sb.append(getAccountName(account));
			sb.append("</td>");
			
			// Columns
			long totalActual = 0;
			long totalBudget = 0;
			for (final Iterator<DetailedBudgetColumn> iterator2 = columns.iterator(); iterator2.hasNext();) {
				final DetailedBudgetColumn col = iterator2.next();
				DetailedBudgetItem item = null;
				if (type == INCOME_ACCOUNTS) {
					item = col.detIncomeItems.get(accNum);
				} else if (type == EXPENSE_ACCOUNTS) {
					item = col.detExpenseItems.get(accNum);
				}
				long actual = 0;
				long budget = 0;
				if (item != null) {
					actual = item.actualAmount;
					budget = item.budgetAmount;
				}
//				System.out.println(" -- item="+item+" actual="+actual+" budget="+budget);
				
				
				if (budgetWithSubtotal || columns.size() == 1) {
					sb.append("<td align=\"right\">").append(getCurrencyStr(budget,null)).append("</td>");
					addSubtotal(parentSubtotalMap,budget,parentSubPtr++);
				}
				sb.append("<td align=\"right\">").append(getCurrencyStr(actual,col.endDay)).append("</td>");
				addSubtotal(parentSubtotalMap,actual,parentSubPtr++);
				if (diffWithSubtotal || columns.size() == 1) {
					long diff = budget - actual;
					if (type == INCOME_ACCOUNTS) {
						diff = actual - budget;
					}
					sb.append("<td align=\"right\">").append(getCurrencyStr(diff,col.endDay)).append("</td>");
					addSubtotal(parentSubtotalMap,diff,parentSubPtr++);
				}
				totalActual += actual;
				totalBudget += budget;
			}
			
			// If more than 1 column add a total column
			if (columns.size() > 1) {
				sb.append("<td align=\"right\">").append(getCurrencyStr(totalBudget,null)).append("</td>");
				addSubtotal(parentSubtotalMap,totalBudget,parentSubPtr++);
				sb.append("<td align=\"right\">").append(getCurrencyStr(totalActual,null)).append("</td>");
				addSubtotal(parentSubtotalMap,totalActual,parentSubPtr++);
				long diff = totalBudget - totalActual;
				if (type == INCOME_ACCOUNTS) {
					diff = totalActual - totalBudget;
				}
				sb.append("<td align=\"right\">").append(getCurrencyStr(diff,null)).append("</td>");
				addSubtotal(parentSubtotalMap,diff,parentSubPtr++);
			}
			sb.append("</tr>\n");
			
			// Do we show all accounts, even if all 0?
			if (showAllAccounts || totalActual != 0 || totalBudget != 0) {
				lastParentHasValues = true;
				if (subtotalsForParentCategories) {
					final Account parentAccount = account.getPath().get(0);
//					System.out.println("  parentAccount="+parentAccount.getAccountName()+" last="+lastParentAccount+" account="+account);
					if (lastParentAccount == null || !lastParentAccount.equals(parentAccount)) {
						// Add subtotal of previous parent account
						if (lastParentAccount != null) {
							addParentCategorySubtotalRow(sbBefore,lastParentSubtotalMap);
//							System.out.println("  sbBefore1="+sbBefore.toString());
							addBlankRow(sbBefore,columns);
						}
						
						// Add heading of current parent account
						addParentHeading(sbBefore,parentAccount,columns);
//						System.out.println("  sbBefore2 ="+sbBefore.toString());
						
						lastParentAccount = parentAccount;
					}
				}
				
				sbo.append(sbBefore);
				sbo.append(sb);
			}
		}
		// Last subtotal
		if (subtotalsForParentCategories) {
			if (lastParentAccount != null) {
				addParentCategorySubtotalRow(sbo,parentSubtotalMap);
			}
		}
	
		return sbo.toString();
	}

	private String getIndentStyle(final int indent) {
		if (indent <= 0) {
			return "";
		}
		return " style=\"padding-left:20px;\"";
	}
	
	private void addSubtotal(final Map<Integer, Long> subtotalMap, final long value, final int index) {
		long val = 0;
		if (subtotalMap.get(new Integer(index)) != null) {
			val = subtotalMap.get(new Integer(index)).longValue();
		}
		val += value;
		subtotalMap.put(new Integer(index),new Long(val));
	}
	
	/** Parent Category with Subtotals row for Parent Category */
	private void addParentCategorySubtotalRow(final StringBuffer sb, final Map<Integer, Long> subtotalMap) {
		sb.append("<tr><td><strong>Subtotal</strong></td>");

		for (int i = 0; i < subtotalMap.size(); i++) {
			final long val = subtotalMap.get(new Integer(i)).longValue();
			sb.append("<td align=\"right\"><strong>"+getCurrencyStr(val,null)+"</strong></td>");
		}
		
//		sb.append("<td>&nbsp;</td>");
		sb.append("</tr>");
	}

	/** Parent Category with Subtotals row for Parent Category */
	private void addParentHeading(final StringBuffer sb, final Account account, final List<DetailedBudgetColumn> columns) {
		sb.append("<tr><td colspan="+getNumTableColumns(columns)+"><strong>"+getAccountName(account)+"</strong></td></tr>");
	}
	
	/** Blank row */
	private void addBlankRow(final StringBuffer sb, final List<DetailedBudgetColumn> columns) {
		sb.append("<tr><td colspan="+getNumTableColumns(columns)+">&nbsp;</td></tr>");
	}
	
	/** Categories HTML for TOTAL of Income or Expenses */
	public String getCategoriesTotalHTML(final List<DetailedBudgetColumn> columns, final int type) {
		final StringBuffer sb = new StringBuffer();
		// TOTALS
		sb.append("<tr><td><strong>");
		if (type == INCOME_ACCOUNTS) {
			sb.append("TOTAL INCOME");
		} else if (type == EXPENSE_ACCOUNTS) {
			sb.append("TOTAL EXPENSE");
		} else if (type == DIFF_ACCOUNTS) {
			sb.append("TOTAL DIFF");
		}
		sb.append("</strong></td>");
		
		// Columns
		long totalActual = 0;
		long totalBudget = 0;
		for (final Iterator<DetailedBudgetColumn> iterator2 = columns.iterator(); iterator2.hasNext();) {
			final DetailedBudgetColumn col = iterator2.next();
			long actual = 0;
			long budget = 0;
			if (type == INCOME_ACCOUNTS) {
				actual = col.getTotalIncomeActualAmount();
				budget = col.getTotalIncomeBudgetAmount();
			}
			else if (type == EXPENSE_ACCOUNTS) {
				actual = col.getTotalExpenseActualAmount();
				budget = col.getTotalExpenseBudgetAmount();
			}
			else if (type == DIFF_ACCOUNTS) {
				actual = col.getTotalIncomeActualAmount() - col.getTotalExpenseActualAmount();
				budget = col.getTotalIncomeBudgetAmount() - col.getTotalExpenseBudgetAmount();
			}
			
			if (budgetWithSubtotal || columns.size() == 1) {
				sb.append("<td align=\"right\"><strong>").append(getCurrencyStr(budget,null)).append("</strong></td>");
			}
			sb.append("<td align=\"right\"><strong>").append(getCurrencyStr(actual,col.endDay)).append("</strong></td>");
			if (diffWithSubtotal || columns.size() == 1) {
				sb.append("<td align=\"right\"><strong>").append(getCurrencyStr(budget - actual,col.endDay)).append("</strong></td>");
			}
			totalActual += actual;
			totalBudget += budget;
		}
		
		// If more than 1 column add a total column
		if (columns.size() > 1) {
			sb.append("<td align=\"right\"><strong>").append(getCurrencyStr(totalBudget,null)).append("</strong></td>");
			sb.append("<td align=\"right\"><strong>").append(getCurrencyStr(totalActual,null)).append("</strong></td>");
			sb.append("<td align=\"right\"><strong>").append(getCurrencyStr(totalBudget - totalActual,null)).append("</strong></td>");
		}
		sb.append("</tr>\n");
		
		return sb.toString();
	}
	
	/**
	 * Get a DetailedBudgetColumn object which contains all Actual and Budgeted Values
	 * for a given time period.
	 * @param startDay Start Day of Period
	 * @param endDay End Day of Period
	 * @return
	 */
	private List<DetailedBudgetColumn> getDetailedBudgetColumns(final Date startDay, final Date endDay) {
		final List<DetailedBudgetColumn> columns = new ArrayList<DetailedBudgetColumn>();
		
		Date sd = startDay;

		// No Subtotals
		if (subTotalBy == null || subTotalBy.equals("None")) {
			final DetailedBudgetColumn col = new DetailedBudgetColumn(startDay,endDay);
			columns.add(col);
		} 
		// Want subtotals
		else  {
			final boolean done = false;
			while (!done) {
				Date e2 = null;
				if (subTotalBy.equals("Week")) {
					e2 = DateUtil.getEndOfWeek(sd,START_DAY_OF_WEEK);
				}
				else if (subTotalBy.equals("Month")) {
					e2 = DateUtil.getEndOfMonth(sd);
				}
				else if (subTotalBy.equals("Year")) {
					e2 = DateUtil.getEndOfYear(sd);
				} 
				else {
					// Shouldnt get here
					break;
				}
				// Have we reached the end
				if (DateUtil.isInSameDayOrAfter(e2, endDay)) {
					final DetailedBudgetColumn col = new DetailedBudgetColumn(sd,endDay);
					columns.add(col);
					break;
				}
				// Next day
				final DetailedBudgetColumn col = new DetailedBudgetColumn(sd,e2);
				columns.add(col);
				sd = DateUtil.setTimeZero(DateUtil.addDays(e2, 1));
			}
		}

		return columns;
	}

	/**
	 * Name of the account (Category)
	 * @param account
	 * @return
	 */
	private String getAccountName(final Account account) {
		if (account == null) {
			return "";
		}
		final StringBuffer sb = new StringBuffer();
		final String[] names = account.getAllAccountNames();
		for (int i = 0; i < names.length; i++) {
			if (subtotalsForParentCategories) {
				if (i > 1) {
					sb.append(":");
				}
				if (i > 0 || names.length == 1) {
					sb.append(names[i]);
				}
			} else {
				if (i > 0) {
					sb.append(":");
				}
				sb.append(names[i]);
			}
		}
		
		return sb.toString();
	}

	private String getFullAccountName(final Account account) {
		if (account == null) {
			return "";
		}
		final StringBuffer sb = new StringBuffer();
		final String[] names = account.getAllAccountNames();
		for (int i = 0; i < names.length; i++) {
			if (i > 0) {
				sb.append(":");
			}
			sb.append(names[i]);
		}
		
		return sb.toString();
	}
	
	/** Return amount as dollars and cents in HTML format. 
	 * If date in future return empty space.*/
	public String getCurrencyStr(final long amount, final Date dt) {
		final StringBuffer sb = new StringBuffer();
		if (dt != null && dt.after(new Date()) && amount == 0) {
			return "&nbsp;";
		}
		if (amount < 0) {
			sb.append("<font color=\"red\">");
		}
		sb.append(CURR_FMT.format(amount/100)).append(".").append(CENTS_FMT.format(Math.abs(amount%100)));
		if (amount < 0) {
			sb.append("</font>");
		}
		return sb.toString();
	}
	
	/**
	 * Get a the budgeted and actual amounts for the period given.
	 * @param type Either INCOME_ACCOUNTS(0) or EXPENSE_ACCOUNTS(1)
	 * @return Map of int (account number) to DetailedBudgetItem
	 */
	private Map<Integer, DetailedBudgetItem> getDetailedBudgetItems(final Date startDay, final Date endDay, final int type) {
//		System.out.println("getDetailedBudgetItems type="+type);
		final Map<Integer, DetailedBudgetItem> txnMap = new HashMap<Integer, DetailedBudgetItem>();
		
		final TransactionSet txSet = extension.getUnprotectedContext()
				.getCurrentAccountBook().getTransactionSet();
		
		// Loop through all transaction
			for(final AbstractTxn t : txSet) {
//			System.out.println("..txn="+t.getAccount().getAccountName()+" => "+t.getAccount().getClass().getName());
			// Only accept income or expense account
			if (type == INCOME_ACCOUNTS && t.getAccount().getAccountType() != AccountType.INCOME) {
				continue;
			} else if (type == EXPENSE_ACCOUNTS && t.getAccount().getAccountType() != AccountType.EXPENSE) {
				continue;
			}
			
			// Is this transaction in the range?
			final int intStartDay = Util.convertDateToInt(startDay);
			final int intEndDay   = Util.convertDateToInt(endDay);
			final int dt = t.getDateInt();
			if (intStartDay <= dt && dt <= intEndDay) {
//				System.out.println("    in date range t="+t+" class="+t.getClass().getName());
				addTransaction(txnMap, t);
			}
		}

		// Loop through all budgeted items
		// If more than one budget has the same Category budgeted, it
		// will sum them.
		final BudgetList budList = extension.getUnprotectedContext().getCurrentAccountBook().getBudgets();
		for(final Budget b : budList.getAllBudgets()) {
			for(final BudgetItem bi : b.getItemList()) {
				final Account a = bi.getTransferAccount();

				// Only accept income or expense accounts
				if (type == INCOME_ACCOUNTS && a.getAccountType() != AccountType.INCOME) {
					continue;
				} else if (type == EXPENSE_ACCOUNTS && a.getAccountType() != AccountType.EXPENSE) {
					continue;
				}
				
				// Is the budget item scheduled for the given time period
				final long budgetedAmount = getBudgetedAmount(bi.getIntervalStartDate(),
														bi.getIntervalEndDate(),
														bi.getInterval(),
														bi.getAmount(), 
														Util.convertDateToInt(startDay), 
														Util.convertDateToInt(endDay));
				if (budgetedAmount == 0) {
					continue;
				}
				
				final Integer accNum = new Integer(a.getAccountNum());
				DetailedBudgetItem item = txnMap.get(accNum);
				if (item == null) {
					item = new DetailedBudgetItem(accNum.intValue(),budgetedAmount,0);
					txnMap.put(accNum,item);
				} else {
					item.budgetAmount += budgetedAmount;
				}
			}
		}
		
		return txnMap;
	}
	
	static class IntervalInfo 
	{
		public IntervalInfo(final int y, final int m, final int d, final boolean p) 
		{
			years = y;
			months = m;
			days = d;
			prorate = p;
		}
		public int years;
		public int months;
		public int days;
		public boolean prorate;
		
		@Override
		public String toString()
		{
			return "" + years + ", " + months + ", " + days + ", " + prorate;
		}
	}
	
	static private Map<Integer, IntervalInfo> intervalMap = null;
	
	static private void buildIntervalMap() 
	{
		intervalMap = new HashMap<Integer, IntervalInfo>();
		
		intervalMap.put(BudgetItem.INTERVAL_NO_REPEAT, new IntervalInfo(3000, 0, 0, false));
		intervalMap.put(BudgetItem.INTERVAL_ANNUALLY, new IntervalInfo(1, 0, 0, true));
		intervalMap.put(BudgetItem.INTERVAL_ONCE_ANNUALLY, new IntervalInfo(1, 0, 0, false));
		intervalMap.put(BudgetItem.INTERVAL_SEMI_ANNUALLY, new IntervalInfo(0, 6, 0, true));
		intervalMap.put(BudgetItem.INTERVAL_ONCE_SEMI_ANNUALLY, new IntervalInfo(0, 6, 0, false));
		intervalMap.put(BudgetItem.INTERVAL_TRI_MONTHLY, new IntervalInfo(0, 3, 0, true));
		intervalMap.put(BudgetItem.INTERVAL_ONCE_TRI_MONTHLY, new IntervalInfo(0, 3, 0, false));
		intervalMap.put(BudgetItem.INTERVAL_MONTHLY, new IntervalInfo(0, 1, 0, true));
		intervalMap.put(BudgetItem.INTERVAL_ONCE_MONTHLY, new IntervalInfo(0, 1, 0, false));
		intervalMap.put(BudgetItem.INTERVAL_SEMI_MONTHLY, new IntervalInfo(0, 1, 0, true));
		intervalMap.put(BudgetItem.INTERVAL_ONCE_SEMI_MONTHLY, new IntervalInfo(0, 1, 0, false));
		intervalMap.put(BudgetItem.INTERVAL_TRI_WEEKLY, new IntervalInfo(0, 0, 21, true));
		intervalMap.put(BudgetItem.INTERVAL_ONCE_TRI_WEEKLY, new IntervalInfo(0, 0, 21, true));
		intervalMap.put(BudgetItem.INTERVAL_BI_WEEKLY, new IntervalInfo(0, 0, 14, true));
		intervalMap.put(BudgetItem.INTERVAL_ONCE_BI_WEEKLY, new IntervalInfo(0, 0, 14, true));
		intervalMap.put(BudgetItem.INTERVAL_WEEKLY, new IntervalInfo(0, 0, 7, true));
		intervalMap.put(BudgetItem.INTERVAL_ONCE_WEEKLY, new IntervalInfo(0, 0, 7, false));
	}

	/**
	 * What is the budgeted amount for the given time period
	 * @param budStart Budget start date
	 * @param budEnd Budget end date
	 * @param interval Budget interval type
	 * @param intervalAmount Budget amount per interval
	 * @param repStart Report start date
	 * @param repEnd Report end date
	 * @return
	 */
	static long getBudgetedAmount(final int budStart, int budEnd, 
								  final int interval, final long intervalAmount, 
								  final int repStart, int repEnd) 
	{
		repEnd = Util.incrementDate(repEnd);
		budEnd = Util.incrementDate(budEnd);

		if (intervalMap == null) {
			buildIntervalMap();
		}
				
		int perStart = budStart;
		int perEnd = perStart;

		// Do the report period and budget period overlap?
		if (budStart > repEnd || budEnd < repStart) {
			return 0;
		}

		// Special handling for INTERVAL_DAILY (very easy case)
		if (interval == BudgetItem.INTERVAL_DAILY)
		{
			perStart = Math.max(perStart, repStart);
			perEnd = Math.min(budEnd, repEnd);
			
			return intervalAmount * (Util.calculateDaysBetween(perStart, perEnd));
		}
		
		final IntervalInfo i = intervalMap.get(new Integer(interval));
		
		long amount = 0;
		while (perEnd < repEnd) 
		{
			// budDt is the beginning of one budget period.  Find the
			// end of the period.
			perStart = perEnd;
			perEnd = Util.incrementDate(perStart, i.years, i.months, i.days);
			
			if (perEnd <= repStart) {
				continue;
			}
			if (perStart > budEnd) {
				break;
			}

			// Determine if we have a partial period, and what the
			// start and end dates are.
			int calcStartDt = perStart, calcEndDt = perEnd;
			boolean partial = false;
			
			if (calcStartDt < repStart) 
			{
				calcStartDt = repStart;
				partial = true;
			}
			if (calcEndDt > repEnd)
			{
				calcEndDt = repEnd;
				partial = true;
			}
			if (calcEndDt > budEnd)
			{
				calcEndDt = budEnd;
				partial = true;
			}
			
			final int periodLen = Util.calculateDaysBetween(perStart, perEnd);
			final int calcLen   = Util.calculateDaysBetween(calcStartDt, calcEndDt);

			// Special handling for semi-monthly:
			if (interval == BudgetItem.INTERVAL_SEMI_MONTHLY ||
				interval == BudgetItem.INTERVAL_ONCE_SEMI_MONTHLY)
			{
				if (!partial) {
					amount += intervalAmount * 2;
				} else if (i.prorate) {
					amount += (intervalAmount * 20 * calcLen / periodLen + 5) / 10;
				} else
				{
					if (calcStartDt == perStart) {
						amount += intervalAmount;
					}
					final int endFirst = Util.incrementDate(calcStartDt, 0, 0, 
													  periodLen / 2);
					if (endFirst < calcEndDt) {
						amount += intervalAmount;
					}
				}
				continue;
			}

			if (!partial || (!i.prorate && calcStartDt == perStart)) {
				amount += intervalAmount;
			} else if (i.prorate) {
				amount += (10 * intervalAmount * calcLen / periodLen + 5) / 10;
			}
		}
		
		return amount;
	}

	/**
	 * Add a transaction to the Map
	 * @param txnMap
	 * @param t
	 */
	private void addTransaction(final Map<Integer, DetailedBudgetItem> txnMap, final AbstractTxn t) {
		if (t == null) {
			return;
		}

		// Get txn account
		final Account a = t.getAccount();
		long amount = t.getValue();
		if (t.getAccount().getAccountType() == AccountType.INCOME) {
			amount = -amount;
		}
		// Get current actual amount
		final Integer accNum = new Integer(a.getAccountNum());
		DetailedBudgetItem item = txnMap.get(accNum);
		if (item == null) {
			item = new DetailedBudgetItem(accNum.intValue(),0,amount);
			txnMap.put(accNum,item);
		} else {
			item.actualAmount += amount;
		}
	}

	/** Get all categories (A category is actually an Account object) based on 
	 * Budget selected */
	private List<Account> getCategories() {
		final List<Account> categoryList = new ArrayList<Account>();
		
		// Do we get all the categories?
		if (!budgetName.equals("ALL")) {
			// Get only specified budget
			final BudgetList budList = extension.getUnprotectedContext().getCurrentAccountBook().getBudgets();
			Budget b = null;
			for (final Budget temp : budList.getAllBudgets()) {
				if (temp.getName().equals(budgetName)) {
					b = temp;
					break;
				}
			}
			// Found Budget, now just get categories for this budget
			if (b != null) {
				for (final BudgetItem bi : b.getItemList()) {
					final Account a = bi.getTransferAccount();
					addAccountAndSubaccounts(categoryList,a);
				}
				
				sortCategories(categoryList);
				return categoryList;
			}

		}
		// Get all categories
		try {
			for (final Account a : extension.getUnprotectedContext().getCurrentAccountBook().getRootAccount()
					.getSubAccounts()) {
				addAccountAndSubaccounts(categoryList,a);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		
		sortCategories(categoryList);
		return categoryList;
	}
	
	private void sortCategories(final List<Account> categoryList) {
		Collections.sort(categoryList,new Comparator<Account>() {
			@Override
			public int compare(final Account arg0, final Account arg1) {
				return getFullAccountName(arg0).compareTo(getFullAccountName(arg1));
			}
		});

	}

	/** Add an account to the list if it isnt there already */
	private void addAccountAndSubaccounts(final List<Account> categoryList, final Account account) {
		if (account == null) {
			return;
		}
		if (account.getAccountType() != AccountType.EXPENSE &&
			account.getAccountType() != AccountType.INCOME) {
			return;
		}
		
		// If not in list, add it
		if (!categoryList.contains(account)) {
			categoryList.add(account);
		}
		
		for (final Account suba : account.getSubAccounts()) {
			if (suba == null) {
				continue;
			}
			addAccountAndSubaccounts(categoryList, suba);
		}
		
	}

	/** Save the Report */
	protected void save() {
		//Create a file chooser
		final JFileChooser fc = new JFileChooser();
		final File defFile = new File(getBudgetPeriodStr()+".html");
		fc.setSelectedFile(defFile);
		fc.setFileFilter(new FileFilter() {
			@Override
			public String getDescription() {
				return "HTML Files";
			}
		
			@Override
			public boolean accept(final File f) {
				if (f.getName().toLowerCase().endsWith("html")) {
					return true;
				}
				return false;
			}
		});
		
		//In response to a button click:
		final int returnVal = fc.showSaveDialog(this);
		
		if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File file = fc.getSelectedFile();
            
            try {
                final BufferedWriter out = new BufferedWriter(new FileWriter(file));
                out.write(txtReport.getText());
                out.close();
            } catch (final IOException e) {
            	// TODO(divegeek) Figure out what's appropriate here.
            }
        }
	}

	/** Close Window */
	protected void close() {
		this.setVisible(false);
		this.dispose();
	}
	
	/**
	 * Budget Item contains account (category) and actual, budget amounts for time period. 
	 */
	class DetailedBudgetItem {
		int accountNum = 0;
		long budgetAmount = 0;
		long actualAmount = 0;
		
		public DetailedBudgetItem(final int accountNum, final long budgetAmount, final long actualAmount) {
			this.accountNum = accountNum;
			this.budgetAmount = budgetAmount;
			this.actualAmount = actualAmount;
		}
	}

	/** Represents a subtotal column in the report including all income and
	 * expense categories.
	 * @author rolf
	 *
	 */ 
	class DetailedBudgetColumn {
		Date startDay = null;
		Date endDay = null;
		// Map of account (int) to DetailedBudgetItem for income accounts
		Map<Integer, DetailedBudgetItem> detIncomeItems = null;
		// Map of account (int) to DetailedBudgetItem for expense accounts
		Map<Integer, DetailedBudgetItem> detExpenseItems = null;
		
		public DetailedBudgetColumn(final Date startDay, final Date endDay) {
			this.startDay = startDay;
			this.endDay = endDay;
		}
		
		/** Total Budget amount for Income Accounts */
		public long getTotalIncomeBudgetAmount() {
			long total = 0;
			for (final Iterator<Account> iterator = categories.iterator(); iterator.hasNext();) {
				final Account account = iterator.next();
				final Integer accNum = new Integer(account.getAccountNum());

				final DetailedBudgetItem item = detIncomeItems.get(accNum);
				if (item != null) {
					total += item.budgetAmount;
				}
			}
			return total;
		}
		
		/** Total Actual amount for Income Accounts */
		public long getTotalIncomeActualAmount() {
			long total = 0;
			for (final Iterator<Account> iterator = categories.iterator(); iterator.hasNext();) {
				final Account account = iterator.next();
				final Integer accNum = new Integer(account.getAccountNum());

				final DetailedBudgetItem item = detIncomeItems.get(accNum);
				if (item != null) {
					total += item.actualAmount;
				}
			}
			return total;
		}
		
		/** Total Budget amount for Expense Accounts */
		public long getTotalExpenseBudgetAmount() {
			long total = 0;
			for (final Iterator<Account> iterator = categories.iterator(); iterator.hasNext();) {
				final Account account = iterator.next();
				final Integer accNum = new Integer(account.getAccountNum());

				final DetailedBudgetItem item = detExpenseItems.get(accNum);
				if (item != null) {
					total += item.budgetAmount;
				}
			}
			return total;
		}
		
		/** Total Actual amount for Expense Accounts */
		public long getTotalExpenseActualAmount() {
			long total = 0;
			for (final Iterator<Account> iterator = categories.iterator(); iterator.hasNext();) {
				final Account account = iterator.next();
				final Integer accNum = new Integer(account.getAccountNum());

				final DetailedBudgetItem item = detExpenseItems.get(accNum);
				if (item != null) {
					total += item.actualAmount;
				}
			}
			return total;
		}
		
	}
}

// Local Variables:
// tab-width: 4
// End: