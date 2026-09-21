# ExitPlan — Java Reverse Planner

ExitPlan is a desktop planning application that works backward from a required return deadline and calculates the latest time a user can leave.

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

## Run
Requires a JDK.

```
javac ExitPlan.java
java ExitPlan
```

This project is intentionally small enough to explain clearly while demonstrating object-oriented Java, event-driven GUI programming, collections, validation, and date/time logic.
