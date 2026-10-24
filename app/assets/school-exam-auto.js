(function(){
 var table=document.getElementById('tabGrid'),button=document.getElementById('search_go');
 if(!table||!button||!table.grid||!window.jQuery)return JSON.stringify({phase:'waiting'});
 var state=window.__rainExamQuery;
 if(!state){
  var busy=document.getElementById('load_tabGrid');if(busy&&getComputedStyle(busy).display!=='none')return JSON.stringify({phase:'waiting'});
  state=window.__rainExamQuery={done:false,error:false};
  window.jQuery(table).on('jqGridLoadComplete.rainReminder',function(){state.done=true;});
  button.click();return JSON.stringify({phase:'querying'});
 }
 return JSON.stringify({phase:state.done?'ready':'querying'});
})();