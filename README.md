# LifeFlow — Personal Life Organisation App

A native Android app (Kotlin) built to help manage work tasks, daily habits,
home chores/groceries, and personal budget — all in one place with push notifications.

---

## Features

### 🔬 Work Tab
- Add research tasks with category (Research, Paper Writing, Meeting, UKAEA Prep, etc.)
- Set priority levels: Low / Medium / High / Critical
- Set deadlines with a date picker
- **Postpone tracking** — every time you postpone, the count is shown on the card
  as a guilt-trip reminder ("Postponed 3×")
- Mark tasks as complete

### 🔥 Habits Tab
- Quick-add common habits: Morning Run, Meditation, Workout, Reading, etc.
- Streak tracking — current streak and all-time best
- Daily completion state resets automatically
- Push notification reminder at your chosen time
- Combined streak counter at the top

### 🏠 Home Tab
Two sub-tabs:
- **Chores** — add cleaning tasks per room (Kitchen, Washroom, Bedroom, etc.)
  with frequency (daily, weekly, monthly). Marks "OVERDUE" in red when due date passed.
- **Grocery** — shopping list with categories, quantity, and checkbox to mark purchased.
  Clear purchased items with one tap.

### 💰 Budget Tab
- Add income and expense transactions
- Monthly navigation (← →) to browse history
- Real-time balance calculation shown at top
- Colour-coded amounts (green = income, red = expense)
- Expense categories: Groceries, Rent, Transport, Health, Dining, etc.

### 🔔 Notifications
- Daily 8 AM morning summary notification
- Per-habit reminders at your chosen time
- Chore overdue reminders
- Notifications survive phone restarts via BootReceiver

---

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android device or emulator with API 26+

### Steps

1. **Open the project**
   - Launch Android Studio → Open → select the `LifeFlow` folder

2. **Sync Gradle**
   - Click "Sync Now" when prompted, or go to File → Sync Project with Gradle Files

3. **Add JitPack repository** (for MPAndroidChart)
   The `settings.gradle` already includes JitPack. If you get a sync error, verify:
   ```gradle
   maven { url 'https://jitpack.io' }
   ```
   is present in your `settings.gradle` repositories block.

4. **Build and Run**
   - Select your device/emulator from the toolbar
   - Click ▶ Run

5. **Grant notification permission**
   - On first launch (Android 13+), the app will request notification permission
   - Tap "Allow" to enable reminders

---

## Project Structure

```
app/src/main/
├── java/com/srikar/lifeflow/
│   ├── MainActivity.kt                  # Entry point, bottom nav setup
│   ├── data/
│   │   ├── model/Models.kt              # Task, Habit, Chore, GroceryItem, Transaction
│   │   ├── db/
│   │   │   ├── Daos.kt                  # Room DAO interfaces
│   │   │   └── AppDatabase.kt           # Room database singleton
│   │   └── repository/Repositories.kt  # Data access layer
│   ├── ui/
│   │   ├── ViewModels.kt                # All 4 ViewModels (Work, Habits, Home, Budget)
│   │   ├── work/WorkFragment.kt         # Work tasks UI + TaskAdapter
│   │   ├── habits/HabitsFragment.kt     # Habits UI + HabitAdapter
│   │   ├── home/HomeFragment.kt         # Chores + Grocery UI + Adapters
│   │   └── budget/BudgetFragment.kt     # Budget UI + TransactionAdapter
│   └── notification/
│       └── NotificationHelper.kt        # Channels, Workers, BootReceiver
├── res/
│   ├── layout/                          # activity_main + fragment + item + dialog layouts
│   ├── menu/bottom_nav_menu.xml
│   ├── navigation/nav_graph.xml
│   └── values/strings.xml, colors.xml, themes.xml
└── AndroidManifest.xml
```

---

## Architecture

- **MVVM** — ViewModel + LiveData + Repository pattern
- **Room** — local SQLite database, no backend needed
- **WorkManager** — persistent background task scheduling for notifications
- **Navigation Component** — bottom tab navigation
- **ViewBinding** — type-safe view access

---

## Extending the App

### Add a new habit category
Edit `HabitsFragment.kt` → `showAddHabitDialog()` → add to `defaultHabits` list.

### Add a new expense category
Add to `ExpenseCategory` enum in `Models.kt` and `expense_categories` array in `strings.xml`.

### Add weekly review screen
Create a new `ReviewFragment` that queries:
- Tasks with `postponeCount > 0`
- Habits not completed on any day this week
- Overdue chores
This gives you a "accountability mirror" every Sunday.

### Sync to cloud (future)
Replace `AppDatabase` queries in repositories with Firebase Firestore calls.
The ViewModel and Fragment code stays the same.

---

## Notes

- All data is stored **locally** on your phone — no account needed, no internet required
- Currency uses € (Euro) — change "€" strings in BudgetFragment if needed
- The postpone system is intentionally visible and slightly guilt-inducing — by design!
- Habit streaks reset if you miss a day (streak goes back to 0 or 1)
