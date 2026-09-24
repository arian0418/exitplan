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
- Multiple editable activities
- Reverse leave-by calculation
- Generated timeline
- Feasibility warning
- Input validation for edited dates, durations, and activity names
- Dates shown in timeline rows so overnight outings stay clear

## Run
Requires a JDK 17 or newer (the application uses Java records).

```
javac ExitPlan.java
java ExitPlan
```

This project demonstrates object-oriented Java, event-driven GUI programming, collections, validation, and date/time logic.
