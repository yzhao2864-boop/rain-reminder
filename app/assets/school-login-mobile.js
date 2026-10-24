(function(){
 if(location.hostname!=='cas-paas.hzau.edu.cn'||!location.pathname.startsWith('/cas/'))return 'other';
 if(!document.getElementById('rain-school-mobile-style')){
 var meta=document.querySelector('meta[name="viewport"]');if(!meta){meta=document.createElement('meta');meta.name='viewport';document.head.appendChild(meta);}meta.content='width=device-width, initial-scale=1';
 var style=document.createElement('style');style.id='rain-school-mobile-style';style.textContent=`
 html.rain-school-login,html.rain-school-login body{width:100%!important;min-width:0!important;margin:0!important;overflow-x:hidden!important;background:#f7f8fa!important}
 html.rain-school-login main,html.rain-school-login #content,html.rain-school-login #vue_main,html.rain-school-login #layout{width:100%!important;min-width:0!important;margin:0!important;padding:0!important}
 .rain-school-login .sw-login{min-width:0!important;width:100%!important;min-height:100vh!important;height:auto!important;background:none!important;padding:18px 12px 24px!important;box-sizing:border-box!important}
 .rain-school-login .sw-login-wrap{height:auto!important;min-height:0!important;background:none!important}
 .rain-school-login .sw-login-wrap > .el-carousel{display:none!important}
 .rain-school-login .language-wrap{position:relative!important;top:auto!important;right:auto!important;text-align:right!important;margin:0 auto 8px!important;max-width:420px!important;height:28px!important;justify-content:flex-end!important}
 .rain-school-login .language-wrap,.rain-school-login .language-wrap *{color:#5c6d66!important}
 .rain-school-login .language-text{color:#5c6d66!important;font-size:12px!important}
 .rain-school-login .language-button{height:28px!important;border-color:#d2dad6!important}
 .rain-school-login .sw-login:before{content:'华中农业大学 · 统一身份认证';display:block;max-width:420px;margin:0 auto 16px;color:#215c52;font:600 17px/1.5 sans-serif;text-align:center}
 .rain-school-login .sw-login .sw-login-main{position:relative!important;left:auto!important;top:auto!important;right:auto!important;bottom:auto!important;transform:none!important;width:100%!important;max-width:420px!important;margin:0 auto!important;overflow:visible!important;display:flex!important;flex-direction:column!important;align-items:stretch!important}
 .rain-school-login .sw-login .sw-login-main .sw-login-main-content{width:100%!important;height:auto!important;min-height:0!important;padding-bottom:24px!important;box-sizing:border-box!important;background:#fff!important;border-radius:16px!important;order:0}
 .rain-school-login .sw-login .sw-login-main .sw-login-main-blur,.rain-school-login .sw-login-main-filter,.rain-school-login .logo-wrap{display:none!important}
 .rain-school-login .login-form{margin:24px 20px 64px!important}
 .rain-school-login .login-form .el-input__inner{font-size:16px!important}
 .rain-school-login .other-link{margin:0 20px!important;flex-wrap:wrap!important;gap:12px!important}
 .rain-school-login .other-login-ways{margin:20px 20px 0!important}
 .rain-school-login .sw-login-menu .el-menu-item{margin-left:18px!important;font-size:16px!important}
 .rain-school-login .notice-announcement{order:2!important;width:100%!important;height:auto!important;margin:14px 0 0!important;box-sizing:border-box!important}
 .rain-school-login .notice-announcement-content .content{height:auto!important;max-height:180px!important;overflow:auto!important}
 .rain-school-login .notice-wrap-content{width:100%!important;box-sizing:border-box!important}
 .rain-school-login .footer,.rain-school-login .browser-support{position:relative!important;left:auto!important;bottom:auto!important;transform:none!important;width:100%!important;max-width:420px!important;margin:16px auto 0!important;padding:8px!important;box-sizing:border-box!important;white-space:normal!important;font-size:11px!important}
 .rain-school-login .footer-top,.rain-school-login .footer-bottom{flex-wrap:wrap!important;gap:8px!important}
 .rain-school-login .el-dialog{max-width:calc(100% - 24px)!important;box-sizing:border-box!important}
 .rain-school-login .phone-style-wrap{padding-left:20px!important;padding-right:20px!important}
 .rain-school-login .captcha-style{width:auto!important;flex:1!important;min-width:0!important}
 .rain-school-login .captcha-img{flex-shrink:0!important}
 `;document.head.appendChild(style);
 var apply=function(){var present=!!document.querySelector('.sw-login');document.documentElement.classList.toggle('rain-school-login',present);};apply();
 new MutationObserver(apply).observe(document.body,{childList:true,subtree:true});
 }
 return document.querySelector('.sw-login')?'login':'waiting';
})();