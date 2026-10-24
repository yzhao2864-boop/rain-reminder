(function () {
  function docs(d) {
    var out = [d];
    Array.from(d.querySelectorAll('iframe')).forEach(function (f) {
      if(!f.getClientRects().length)return;
      try { if (f.contentDocument && f.contentDocument.body) out = out.concat(docs(f.contentDocument)); } catch (_) {}
    });
    return out;
  }
  var pages = docs(document), text = pages.map(function(d){return d.body ? d.body.innerText : '';}).join('\n');
  function visible(e) { return !!(e.getClientRects().length); }
  function exact(label) {
    for (var d of pages) {
      var e = Array.from(d.querySelectorAll('*')).find(function(n){return n.children.length === 0 && n.textContent.trim() === label && visible(n);});
      if (e) return e;
    }
    return null;
  }
  var cmd = __COMMAND__;
  if (/扫码登录|账号密码登录|短信登录/.test(text) && !/我听的课|开课时间/.test(text)) return JSON.stringify({error:'登录已过期，请在 App 内重新登录'});
  if(cmd.mode==='courses'||cmd.mode==='open'){
    var student=document.getElementById('tab-student');
    if(student&&student.getAttribute('aria-selected')!=='true'){student.click();return JSON.stringify({wait:true});}
  }
  if (cmd.mode === 'courses') {
    var panel=document.querySelector('#pane-student') || document.querySelector('[role="tabpanel"]');
    var heads=panel?Array.from(panel.querySelectorAll('h1')):[];
    if (!heads.length) return JSON.stringify({wait:true});
    return JSON.stringify({courses:heads.map(function(e,i){return {title:e.textContent.trim(),index:i};})});
  }
  if (cmd.mode === 'open') {
    var panel=document.querySelector('#pane-student') || document.querySelector('[role="tabpanel"]');
    var heads=panel?Array.from(panel.querySelectorAll('h1')):[];
    var h=heads[cmd.index];
    if(!h || h.textContent.trim()!==cmd.title) return JSON.stringify({wait:true});
    h.click();return JSON.stringify({clicked:true});
  }
  if (cmd.mode === 'unfinished') {
    var e=exact('未完成');
    if(!e)return JSON.stringify({wait:true});
    e.click();return JSON.stringify({clicked:true});
  }
  if (cmd.mode === 'read') {
    var scopes=[];
    for(var d of pages){
      var active=Array.from(d.querySelectorAll('.el-tab-pane.unfinished')).filter(visible);
      if(active.length)scopes=scopes.concat(active);
    }
    if(!scopes.length)return JSON.stringify({wait:true});
    text=scopes.map(function(s){return s.innerText;}).join('\n');
    if (/正在加载|加载中/.test(text)) return JSON.stringify({wait:true});
    if(!/全部待办/.test(text)) return JSON.stringify({wait:true});
    var found=[],unknown=0,total=0,hasMore=false;
    for(var d of scopes){
      var rows=Array.from(d.querySelectorAll('li.unfinished-study__item'));
      if(!rows.length) rows=Array.from(d.querySelectorAll('section.activity__wrap'));
      if(!rows.length) rows=Array.from(d.querySelectorAll('h2')).map(function(h){return h.closest('section') || h.parentElement.parentElement;});
      for(var row of rows){
        if(!visible(row))continue;
        total++;
        var content=row.innerText.trim(), icon=Array.from(row.querySelectorAll('use')).map(function(u){return u.getAttribute('href')||u.getAttribute('xlink:href')||'';}).join(' ');
        var type=(row.querySelector('.unfinished-study__type-text, .tag')||{}).innerText||'';
        if(type==='课堂'||/ketang|qiandao/.test(icon)||/已签到|未签到|缺勤/.test(content)) continue;
        if(/^(视频|音频|课件|PPT|图文|资料|公告|讨论)$/.test(type))continue;
        var title=row.querySelector('.unfinished-study__item-title, h2');
        var explicit=/作业|试卷|考试|测试|测验/.test(type+' '+icon+' '+content)||/shijuan|kaoshi|zuoye|test|exam|paper|exercise/i.test(icon);
        if(!explicit){unknown++;continue;}
        if(/已提交|已完成|已交卷|已批改/.test(content)&&!/未提交|未完成|未交卷/.test(content))continue;
        found.push({title:title?title.innerText.trim():content.split('\n')[0],detail:content.slice(0,1800)});
      }
      var next=Array.from(d.querySelectorAll('button')).find(function(b){return /下一页|加载更多/.test(b.innerText)&&!b.disabled;});
      if(next)hasMore=true;
    }
    // Unknown types must not silently become a successful empty result.
    var countMatch=text.match(/全部待办\s*(\d+)/);
    var expected=countMatch?Number(countMatch[1]):null;
    return JSON.stringify({tasks:found,unknown:unknown,total:total,expected:expected,hasMore:hasMore,url:location.href});
  }
  return JSON.stringify({error:'不支持的读取操作'});
})();
