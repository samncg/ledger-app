(function(){
  const messages=["Ledger","Track every ringgit.","Stay on budget.","Simple. Private. Yours.","Log a spend →"];
  const baseTitle=document.title;
  let msgIndex=0,charIndex=0,deleting=false,running=false,timer=null;

  function schedule(fn,delay){timer=setTimeout(fn,delay)}

  function tick(){
    const current=messages[msgIndex];
    if(!deleting){
      charIndex++;
      document.title=current.slice(0,charIndex)+"▍";
      if(charIndex>=current.length){deleting=true;schedule(tick,1100);return}
      schedule(tick,85+Math.random()*45);
    }else{
      charIndex--;
      document.title=(current.slice(0,charIndex)||"")+"▍";
      if(charIndex<=0){
        deleting=false;
        msgIndex=(msgIndex+1)%messages.length;
        schedule(tick,350);
        return;
      }
      schedule(tick,35+Math.random()*20);
    }
  }

  function start(){
    if(running)return;
    running=true;
    charIndex=0;deleting=false;
    tick();
  }
  function stop(){
    running=false;
    clearTimeout(timer);
  }

  document.addEventListener("visibilitychange",function(){
    if(document.hidden){
      stop();
      document.title="👋 come back?";
    }else{
      document.title=baseTitle;
      start();
    }
  });

  if(!document.hidden)start();
})();
