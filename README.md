# ExitPlan

ExitPlan is a reverse-planning web application for outings with a strict return deadline. Instead of starting with an arrival time, the user enters when they must be back and the application works backward to calculate when they should leave.

## Features

- Set a required return date and time
- Add one or more activities and durations
- Include outbound and return travel time
- Include parking/walking time
- Add a safety buffer for unexpected delays
- Calculate a recommended leave-by time
- Generate a chronological outing timeline
- Warn when the calculated start time has already passed
- Save up to 10 plans in the browser
- Reload or delete saved plans
- Responsive interface for desktop and mobile

## Technology

- HTML
- CSS
- JavaScript
- Browser Local Storage

This version intentionally uses a straightforward stack so the project stays practical and understandable as an undergraduate software engineering project.

## Run

No installation is required. Open `index.html` in a web browser.

For a simple local server, Python can also be used:

```bash
python -m http.server 8000
```

Then open `http://localhost:8000`.

## How the Reverse Calculation Works

ExitPlan adds the planned activity durations, outbound travel, return travel, parking/walking time, and safety buffer. That total duration is subtracted from the required return time.

For example, if a user must be home at 11:00 PM and the complete outing requires 3 hours, the recommended leave time is 8:00 PM.

## Current Scope

Travel times are entered by the user rather than fetched from a maps service. This keeps the first version focused on the project's main idea: reverse scheduling around a fixed deadline. A future version could integrate a maps API for live travel estimates.

## What This Project Demonstrates

ExitPlan demonstrates requirements-driven application design, date/time calculations, DOM manipulation, input validation, responsive interface design, browser persistence, and a scheduling algorithm built around a practical problem.
