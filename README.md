# ExitPlan — Java Reverse Planner

ExitPlan is a desktop planning application that works backward from a required return deadline and calculates the latest time a user can leave.

## Why I Built This

I wanted an easier way to plan outings when the most important time is when I need to be back, not when I need to arrive somewhere. Normal navigation tools are useful for getting to a destination, but I still had to work backward myself when I had a fixed return time.

I built ExitPlan around a simple question: **If I need to be back by a certain time, when do I actually need to leave?** The app works backward from that deadline while accounting for activities, travel, parking or walking time, and a safety buffer.

## Main language
**Java**

The app uses Java Swing for the desktop interface and Java's `java.time` API for date/time calculations. There is no JavaScript, Node.js, Python dependency, or external framework.

## Features
- Required return date and time
- Outbound and return travel time
- Parking/walking allowance
- Safety buffer
- Multiple editable activities with travel time between consecutive stops
- Reverse leave-by calculation
- Generated timeline
- Feasibility warning
- Input validation for edited dates, durations, and activity names
- Dates shown in timeline rows so overnight outings stay clear

## Two-minute demo

1. Start the desktop app using the commands below. A sample dinner plan appears with a return deadline three hours from now.
2. Set **Must be back by** to October 1, 2026 at 12:30 AM. Enter outbound travel 20, return travel 20, parking or walking 5 each way, and buffer 10.
3. Use two activities: Dinner for 60 minutes with 15 minutes travel to next; Movie for 90 minutes with 0 minutes travel to next.
4. Click **Calculate reverse plan**. The leave-by result is **September 30, 2026 at 8:45 PM**, and the timeline includes the inter-stop travel. Try increasing a duration to see the result change.

Travel times are user-entered estimates; this desktop app does not fetch live traffic.

## Run
Requires a JDK 17 or newer (the application uses Java records).

```
javac ExitPlan.java PlanCalculator.java
java ExitPlan
```

This project demonstrates object-oriented Java, event-driven GUI programming, collections, validation, and date/time logic.

## Tests

Run `javac ExitPlan.java PlanCalculator.java PlanCalculatorTest.java` and `java PlanCalculatorTest`. GitHub Actions runs these commands on each pull request.

For each activity, enter the minutes of travel **to the next activity**. Set the last activity's travel to 0; the separate return travel setting covers the trip home. The timeline displays dates for overnight plans.
