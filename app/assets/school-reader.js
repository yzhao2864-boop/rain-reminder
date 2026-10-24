(function(){
 var card=Array.from(document.querySelectorAll('.card-Common_Schedule,.card-Common_SemesterCourse')).find(function(node){return node.getClientRects().length&&getComputedStyle(node).visibility!=='hidden';});
 if(!card)return JSON.stringify({error:'请登录后进入教育教学在线个人中心，等待课表加载。'});
 var week=card.querySelector('.schedule-week'),term=card.querySelector('.schedule-term'),records=[];
 if(week){var anchor=week.textContent.trim();if(!/第\s*\d+\s*周/.test(anchor)||!(/\d{4}-\d{1,2}-\d{1,2}/g.test(anchor)))return JSON.stringify({error:'正在等待学校周次和日期加载完整。'});return JSON.stringify({week:anchor});}
 var table=card.querySelector('table');if(!table||!term)return JSON.stringify({error:'课表尚未加载，请稍候再读取。'});
 Array.from(table.rows).slice(1).forEach(function(row){Array.from(row.cells).slice(1).forEach(function(cell,day){cell.querySelectorAll('.course-item').forEach(function(item){var name=item.querySelector('.name span'),time=item.querySelector('.el-icon-time'),room=item.querySelector('.el-icon-location-outline');if(name&&time)records.push({title:name.textContent.trim(),day:day+1,periodWeeks:time.parentElement.textContent.trim(),place:room?room.parentElement.textContent.trim():''});});});});
 if(!records.length)return JSON.stringify({error:'正在等待学期课程加载。'});
 return JSON.stringify({term:term.textContent.trim(),records:records});
})();
