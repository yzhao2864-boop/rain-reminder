(function(){
 if(!document.querySelector('.login-wraper'))return false;
 var meta=document.querySelector('meta[name=viewport]');
 if(meta)meta.content='width=device-width, initial-scale=1, minimum-scale=0.5, maximum-scale=3, user-scalable=yes';
 if(!document.getElementById('rain-mobile-style')){
  var style=document.createElement('style');style.id='rain-mobile-style';
  style.textContent=`
   html,body{width:100%!important;min-width:0!important;height:auto!important;min-height:100%!important;overflow-x:hidden!important;background:#f4f7f2!important;}
   .login-wraper{position:relative!important;width:100%!important;min-width:0!important;min-height:100vh!important;height:auto!important;background:#f4f7f2!important;padding:20px 16px!important;box-sizing:border-box!important;}
   .login-wraper .masker,.login-wraper .server-wrapper,.login-wraper .L_title,.login-wraper .changeImg,.login-wraper .way-tips{display:none!important;}
   .wrapper-inner{position:static!important;transform:none!important;width:100%!important;height:auto!important;min-height:0!important;padding:0!important;margin:0!important;box-sizing:border-box!important;}
   .login-box{position:static!important;float:none!important;transform:none!important;width:100%!important;max-width:480px!important;height:auto!important;min-height:0!important;margin:0 auto!important;padding:24px 16px!important;box-sizing:border-box!important;border-radius:20px!important;background:white!important;box-shadow:none!important;}
   .account-box{position:static!important;float:none!important;transform:none!important;width:100%!important;height:auto!important;min-height:0!important;padding:0!important;margin:0!important;box-sizing:border-box!important;}
   .content-box{width:100%!important;height:auto!important;min-height:0!important;box-sizing:border-box!important;margin:0!important;padding:0!important;}
   .tab-box{display:flex!important;width:100%!important;height:48px!important;padding:0!important;margin:0 0 24px!important;align-items:center!important;justify-content:space-between!important;}
   .tab-box .item{float:none!important;flex:1!important;margin:0!important;padding:10px 0!important;font-size:16px!important;text-align:center!important;line-height:26px!important;}
   .input-box{width:100%!important;min-height:52px!important;height:auto!important;box-sizing:border-box!important;margin:0 0 16px!important;display:flex!important;align-items:center!important;}
   .input-box .left{width:auto!important;flex:1!important;min-width:0!important;}
   .input-box input{width:100%!important;min-width:0!important;box-sizing:border-box!important;height:50px!important;font-size:17px!important;}
   .verify_code{font-size:14px!important;white-space:nowrap!important;flex-shrink:0!important;}
   .login-btn,.reset-btn{width:100%!important;height:52px!important;line-height:52px!important;margin:20px 0 14px!important;border-radius:12px!important;font-size:17px!important;box-sizing:border-box!important;}
   .agreement-box{position:static!important;transform:none!important;left:auto!important;top:auto!important;width:100%!important;max-width:480px!important;height:auto!important;margin:18px auto!important;line-height:24px!important;font-size:12px!important;text-align:left!important;box-sizing:border-box!important;white-space:normal!important;}
   .agreement-box .agreement-text,.agreement-box a{font-size:12px!important;line-height:24px!important;}
   .login-wraper .footer,.copyright,.copyright_thu,.pic_author{display:none!important;}
  `;
  document.head.appendChild(style);
 }
 if(!document.documentElement.hasAttribute('data-rain-phone-ready')){
  var corner=document.querySelector('img[alt="账号密码登录"]');
  if(corner)corner.click();
  var sms=Array.from(document.querySelectorAll('.account-box .tab-box .item')).find(e=>e.textContent.trim()==='短信登录');
  if(sms)sms.click();
  if(document.querySelector('.account-box')&&getComputedStyle(document.querySelector('.account-box')).display!=='none')document.documentElement.setAttribute('data-rain-phone-ready','1');
 }
 return true;
})();
