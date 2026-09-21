from datetime import datetime, timedelta
import pandas as pd
import streamlit as st

st.set_page_config(page_title="ExitPlan", page_icon="⏱️", layout="wide")
st.markdown("""<style>.stApp{background:#080b10}.block-container{max-width:1100px;padding-top:2rem}[data-testid="stMetric"]{background:#101720;border:1px solid #24313e;padding:16px;border-radius:10px}</style>""",unsafe_allow_html=True)
st.title("⏱️ ExitPlan")
st.caption("Reverse-plan an outing from the time you absolutely need to be back.")

with st.sidebar:
    st.header("Return deadline")
    return_date=st.date_input("Return date")
    return_time=st.time_input("Must be back by")
    st.divider()
    outbound=st.number_input("Outbound travel (minutes)",0,300,20)
    return_travel=st.number_input("Return travel (minutes)",0,300,20)
    parking=st.number_input("Parking / walking each way (minutes)",0,120,5)
    buffer=st.number_input("Safety buffer (minutes)",0,180,15)

if "activities" not in st.session_state:
    st.session_state.activities=[{"name":"Dinner","minutes":60}]

st.subheader("Activities")
for i,item in enumerate(st.session_state.activities):
    c1,c2,c3=st.columns([3,1,.5])
    item["name"]=c1.text_input("Activity",item["name"],key=f"name{i}",label_visibility="collapsed")
    item["minutes"]=c2.number_input("Minutes",5,480,item["minutes"],5,key=f"mins{i}",label_visibility="collapsed")
    if c3.button("✕",key=f"del{i}") and len(st.session_state.activities)>1:
        st.session_state.activities.pop(i);st.rerun()
if st.button("+ Add activity"):
    st.session_state.activities.append({"name":f"Stop {len(st.session_state.activities)+1}","minutes":30});st.rerun()

deadline=datetime.combine(return_date,return_time)
activity_total=sum(x["minutes"] for x in st.session_state.activities)
total=outbound+return_travel+parking*2+buffer+activity_total
leave=deadline-timedelta(minutes=total)

st.divider()
a,b,c=st.columns(3)
a.metric("Leave by",leave.strftime("%I:%M %p"))
b.metric("Planned outing",f"{total//60}h {total%60}m")
c.metric("Safety buffer",f"{buffer} min")

if leave<=datetime.now():
    st.error("This plan requires leaving now or earlier. Reduce durations or move the return deadline.")
else:
    st.success(f"You have {str(leave-datetime.now()).split('.')[0]} until your leave-by time.")

st.subheader("Reverse-planned timeline")
cursor=leave
rows=[{"Step":"Leave","Start":cursor.strftime("%I:%M %p"),"End":"—","Duration":f"{outbound} min travel"}]
cursor+=timedelta(minutes=outbound+parking)
for item in st.session_state.activities:
    end=cursor+timedelta(minutes=item["minutes"])
    rows.append({"Step":item["name"],"Start":cursor.strftime("%I:%M %p"),"End":end.strftime("%I:%M %p"),"Duration":f"{item['minutes']} min"})
    cursor=end
rows.append({"Step":"Head home","Start":cursor.strftime("%I:%M %p"),"End":deadline.strftime("%I:%M %p"),"Duration":f"{parking+return_travel+buffer} min incl. buffer"})
st.dataframe(pd.DataFrame(rows),use_container_width=True,hide_index=True)
st.caption("ExitPlan calculates backward from your required return time. It is a planning estimate; real travel conditions can change.")
