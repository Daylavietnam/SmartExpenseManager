# Implementation Plan - SmartExpenseManager Enhancements

This plan outlines the changes to improve the category selection in `AddActivity`, enhance charts in `ReportActivity`, and add comprehensive filtering in `DetailActivity`.

## User Review Required

> [!IMPORTANT]
> The current project uses an in-memory repository (`TransactionRepository`). All changes will be based on this structure. If the app is restarted, changes will be lost unless a database (like Room) is integrated later.

## Proposed Changes

### 1. Add Expense Screen (`AddActivity`)

Separate categories for Expense and Income.

#### [MODIFY] [activity_add.xml](file:///D:/Projects/Android/SmartExpenseManager/app/src/main/res/layout/activity_add.xml)
- Reorganize category buttons into two groups (Expense Categories and Income Categories) or use a dynamic container. I will use two `FlowLayout`-like structures or simple `LinearLayout`s that toggle visibility.

#### [MODIFY] [AddActivity.java](file:///D:/Projects/Android/SmartExpenseManager/app/src/main/java/com/group14/expensemanager/AddActivity.java)
- Add logic to show/hide category buttons based on selected transaction type.
- Update `setupCategoryButtons` to handle the new category sets.
- Ensure the default selected category updates when switching types.

---

### 2. Report Screen (`ReportActivity`)

Enhance Pie Chart and fix/implement Line Chart.

#### [MODIFY] [ReportActivity.java](file:///D:/Projects/Android/SmartExpenseManager/app/src/main/java/com/group14/expensemanager/ReportActivity.java)
- Update Pie Chart configuration:
    - Disable entry labels inside slices.
    - Enable value labels (amounts).
    - Configure legend to show below the chart.
- Implement `updateLineChart` logic:
    - Weekly: Group transactions by week, X-axis showing Sunday.
    - Monthly: Group transactions by month, X-axis showing last day of month.
    - Show two lines: Expense and Income.

#### [MODIFY] [viewmodel/ReportViewModel.java](file:///D:/Projects/Android/SmartExpenseManager/app/src/main/java/com/group14/expensemanager/viewmodel/ReportViewModel.java)
- Add data processing logic for weekly and monthly line charts.

---

### 3. Detail Screen (`DetailActivity`)

Implement advanced filtering and sorting.

#### [MODIFY] [DetailActivity.java](file:///D:/Projects/Android/SmartExpenseManager/app/src/main/java/com/group14/expensemanager/DetailActivity.java)
- Implement Date Picker for "Day" filter.
- Implement Date Picker + week calculation for "Week" filter.
- Implement a month/year selection dialog or custom scroll for "Month" filter.
- Add "All" option to the filter menu.

#### [MODIFY] [viewmodel/DetailViewModel.java](file:///D:/Projects/Android/SmartExpenseManager/app/src/main/java/com/group14/expensemanager/viewmodel/DetailViewModel.java)
- Update filtering logic to support the new types (Day, Week, Month, All).
- Ensure transactions are sorted by timestamp descending (newest first).

## Verification Plan

### Manual Verification
- **Add Screen:** Switch between Expense and Income and verify categories change correctly. Save transactions and check if they appear in the list.
- **Report Screen:** Verify Pie Chart labels. Check Line Chart when switching between Week and Month ranges.
- **Detail Screen:** Test each filter option (Day, Week, Month, All) and verify the displayed transactions match the selected time range. Verify the newest transactions are at the top.
