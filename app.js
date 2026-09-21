const activitiesEl=document.getElementById("activities");
const resultEl=document.getElementById("result");
const errorEl=document.getElementById("error");
let lastPlan=null;

function activityRow(name="",duration=60){
  const row=document.createElement("div");
  row.className="activity";
  row.innerHTML='<label>Activity<input class="activity-name" placeholder="Dinner" value="'+escapeHtml(name)+'"></label><label>Minutes<input class="activity-duration" type="number" min="1" value="'+duration+'"></label><button class="remove" title="Remove">×</button>';
  row.querySelector(".remove").onclick=()=>{if(activitiesEl.children.length>1)row.remove()};
  activitiesEl.appendChild(row);
}
function escapeHtml(text){return String(text).replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[c]))}
function minutes(id){return Math.max(0,Number(document.getElementById(id).value)||0)}
function fmt(date){return date.toLocaleTimeString([],{hour:"numeric",minute:"2-digit"})}
function fullFmt(date){return date.toLocaleString([],{month:"short",day:"numeric",hour:"numeric",minute:"2-digit"})}
function getActivities(){return [...document.querySelectorAll(".activity")].map((row,i)=>({name:row.querySelector(".activity-name").value.trim()||"Activity "+(i+1),duration:Math.max(1,Number(row.querySelector(".activity-duration").value)||1)}))}
function calculate(){
  errorEl.textContent="";
  const raw=document.getElementById("returnTime").value;
  if(!raw){errorEl.textContent="Choose the time you must be back.";return}
  const returnTime=new Date(raw);
  if(Number.isNaN(returnTime.getTime())){errorEl.textContent="Enter a valid return time.";return}
  const activities=getActivities();
  const travelOut=minutes("travelOut"),travelHome=minutes("travelHome"),parking=minutes("parking"),buffer=minutes("buffer");
  const activityMinutes=activities.reduce((sum,a)=>sum+a.duration,0);
  const total=activityMinutes+travelOut+travelHome+parking+buffer;
  const leave=new Date(returnTime.getTime()-total*60000);
  const now=new Date();
  const feasible=leave>now;
  let cursor=new Date(leave);
  const events=[{time:new Date(cursor),label:"Leave",detail:"Start the outing"}];
  cursor=new Date(cursor.getTime()+travelOut*60000);
  if(travelOut)events.push({time:new Date(cursor),label:"Arrive at first stop",detail:travelOut+" min travel"});
  activities.forEach((a,index)=>{const start=new Date(cursor);cursor=new Date(cursor.getTime()+a.duration*60000);events.push({time:start,label:a.name,detail:fmt(start)+" – "+fmt(cursor)+" ("+a.duration+" min)"});if(index===activities.length-1&&parking)cursor=new Date(cursor.getTime()+parking*60000)});
  const homeDeparture=new Date(cursor.getTime()+buffer*60000);
  events.push({time:new Date(cursor),label:"Wrap up",detail:"Includes "+buffer+" min safety buffer"});
  events.push({time:homeDeparture,label:"Head home",detail:travelHome+" min travel"});
  events.push({time:returnTime,label:"Back by deadline",detail:fullFmt(returnTime)});
  lastPlan={createdAt:new Date().toISOString(),returnTime:returnTime.toISOString(),leave:leave.toISOString(),total,activities,travelOut,travelHome,parking,buffer};
  document.getElementById("leaveBy").textContent="Leave by "+fmt(leave);
  const status=document.getElementById("feasibility");status.textContent=feasible?"Plan fits":"Start time has passed";status.className="status"+(feasible?"":" warning");
  document.getElementById("summary").innerHTML='<div><strong>'+total+' min</strong><span>Total outing time</span></div><div><strong>'+activityMinutes+' min</strong><span>Activities</span></div><div><strong>'+buffer+' min</strong><span>Safety buffer</span></div>';
  document.getElementById("timeline").innerHTML=events.map(e=>'<div class="event"><strong>'+fmt(e.time)+' — '+escapeHtml(e.label)+'</strong><span>'+escapeHtml(e.detail)+'</span></div>').join("");
  if(!document.getElementById("savePlan")){const b=document.createElement("button");b.id="savePlan";b.className="secondary";b.textContent="Save this plan";b.onclick=savePlan;resultEl.appendChild(b)}
  resultEl.classList.remove("hidden");resultEl.scrollIntoView({behavior:"smooth",block:"start"});
}
function savePlan(){if(!lastPlan)return;const plans=JSON.parse(localStorage.getItem("exitplan-plans")||"[]");plans.unshift(lastPlan);localStorage.setItem("exitplan-plans",JSON.stringify(plans.slice(0,10)));renderSaved()}
function renderSaved(){const el=document.getElementById("savedPlans");const plans=JSON.parse(localStorage.getItem("exitplan-plans")||"[]");if(!plans.length){el.innerHTML='<p class="empty">No saved plans yet.</p>';return}el.innerHTML=plans.map((p,i)=>'<div class="saved-item"><div><strong>Leave '+fmt(new Date(p.leave))+'</strong><span>Back '+fullFmt(new Date(p.returnTime))+' · '+p.activities.length+' activity(s)</span></div><div class="saved-actions"><button onclick="loadSaved('+i+')">Load</button><button onclick="deleteSaved('+i+')">Delete</button></div></div>').join("")}
window.loadSaved=i=>{const plans=JSON.parse(localStorage.getItem("exitplan-plans")||"[]");const p=plans[i];if(!p)return;document.getElementById("returnTime").value=new Date(new Date(p.returnTime).getTime()-new Date().getTimezoneOffset()*60000).toISOString().slice(0,16);["travelOut","travelHome","parking","buffer"].forEach(id=>document.getElementById(id).value=p[id]);activitiesEl.innerHTML="";p.activities.forEach(a=>activityRow(a.name,a.duration));calculate()};
window.deleteSaved=i=>{const plans=JSON.parse(localStorage.getItem("exitplan-plans")||"[]");plans.splice(i,1);localStorage.setItem("exitplan-plans",JSON.stringify(plans));renderSaved()};
document.getElementById("addActivity").onclick=()=>activityRow();
document.getElementById("calculate").onclick=calculate;
document.getElementById("clearSaved").onclick=()=>{localStorage.removeItem("exitplan-plans");renderSaved()};
document.getElementById("exampleBtn").onclick=()=>{const d=new Date(Date.now()+4*60*60*1000);document.getElementById("returnTime").value=new Date(d.getTime()-d.getTimezoneOffset()*60000).toISOString().slice(0,16);document.getElementById("travelOut").value=25;document.getElementById("travelHome").value=25;document.getElementById("parking").value=10;document.getElementById("buffer").value=15;activitiesEl.innerHTML="";activityRow("Dinner",75);activityRow("Coffee",45)};
setInterval(()=>document.getElementById("clock").textContent=new Date().toLocaleTimeString([],{hour:"numeric",minute:"2-digit"}),1000);
activityRow("Activity",60);renderSaved();
