# ExitPlan — Python Reverse Planner

ExitPlan plans an outing backward from a fixed return deadline. Instead of asking when you want to arrive somewhere, it asks when you **must be back** and calculates the latest safe leave time.

## Stack
Python • Streamlit • pandas

## Features
- Fixed return date/time
- Outbound and return travel
- Parking/walking time
- Safety buffer
- Multiple activities with editable durations
- Automatic leave-by calculation
- Reverse-planned timeline
- Warning when the plan already requires leaving

## Run on Windows
```
python -m pip install -r requirements.txt
python -m streamlit run app.py
```

No JavaScript, Node.js, database, or build step is required.
