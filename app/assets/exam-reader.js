(function(){
 var table=document.getElementById('tabGrid');if(!table)return JSON.stringify({error:'请在个人中心打开考试信息，先查询本学期，再点读取。'});
 var records=[];table.querySelectorAll('tr.jqgrow').forEach(function(row){function field(key){var cell=row.querySelector('td[aria-describedby="tabGrid_'+key+'"]');return cell?cell.textContent.trim():'';}records.push({title:field('kcmc'),kind:field('ksmc'),time:field('kssj'),place:field('cdmc')});});
 var pages=document.getElementById('sp_1_pager'),page=document.querySelector('#pager .ui-pg-input');return JSON.stringify({exams:records,pages:pages?pages.textContent.trim():'1',page:page?page.value:'1'});
})();
