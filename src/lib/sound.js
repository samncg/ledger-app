/* ═══════════════════════════════════════════
   PIGGY BANK — sounds, confetti
   ═══════════════════════════════════════════ */
let piggyAudio=null;
const getAudio=()=>{
  if(!piggyAudio)try{piggyAudio=new (window.AudioContext||window.webkitAudioContext)()}catch(e){return null}
  if(piggyAudio&&piggyAudio.state==='suspended')piggyAudio.resume();
  return piggyAudio;
};
const tone=(ctx,freq,at,dur,type='sine',vol=0.18)=>{
  const o=ctx.createOscillator(),g=ctx.createGain();
  o.type=type;o.frequency.value=freq;
  g.gain.setValueAtTime(0.0001,at);
  g.gain.exponentialRampToValueAtTime(vol,at+0.015);
  g.gain.exponentialRampToValueAtTime(0.0001,at+dur);
  o.connect(g).connect(ctx.destination);
  o.start(at);o.stop(at+dur+0.05);
};
export const playPiggySound=(id,customUrl)=>{
  if(id==='custom'){
    if(customUrl){const a=new Audio(customUrl);a.play().catch(()=>{})}
    else{const ctx=getAudio();if(ctx)tone(ctx,987.77,ctx.currentTime,0.12,'triangle',0.2)}
    return;
  }
  if(id==='none')return;
  const ctx=getAudio();if(!ctx)return;
  const t=ctx.currentTime;
  if(id==='chime'){
    [523.25,659.25,783.99,1046.5].forEach((f,i)=>tone(ctx,f,t+i*0.09,0.35,'sine',0.14));
  }else{
    tone(ctx,987.77,t,0.12,'triangle',0.2);
    tone(ctx,1318.5,t+0.07,0.2,'triangle',0.18);
  }
};
export const playCelebrate=()=>{
  const ctx=getAudio();if(!ctx)return;
  const t=ctx.currentTime;
  [523.25,659.25,783.99,1046.5,1318.5].forEach((f,i)=>tone(ctx,f,t+i*0.08,0.5,'triangle',0.16));
  [261.63,329.63,392,523.25].forEach((f,i)=>tone(ctx,f/2,t+i*0.08,0.7,'sine',0.1));
};
export const confettiBurst=()=>{
  const canvas=document.createElement('canvas');
  canvas.style.cssText='position:fixed;inset:0;width:100vw;height:100vh;pointer-events:none;z-index:99999';
  document.body.appendChild(canvas);
  const ctx=canvas.getContext('2d');
  canvas.width=window.innerWidth;canvas.height=window.innerHeight;
  const colors=['#ff5e7e','#ffd166','#06d6a0','#118ab2','#8338ec','#ff9f1c'];
  const parts=Array.from({length:180},()=>({
    x:Math.random()*canvas.width,
    y:-30-Math.random()*canvas.height*0.6,
    w:6+Math.random()*9,h:9+Math.random()*11,
    c:colors[Math.random()*colors.length|0],
    vy:2+Math.random()*3.5,vx:-1.6+Math.random()*3.2,
    rot:Math.random()*Math.PI,vr:-0.15+Math.random()*0.3
  }));
  const start=performance.now();
  const step=now=>{
    ctx.clearRect(0,0,canvas.width,canvas.height);
    for(const p of parts){
      p.y+=p.vy;p.x+=p.vx;p.rot+=p.vr;
      if(p.y>canvas.height+20){p.y=-20;p.x=Math.random()*canvas.width}
      ctx.save();ctx.translate(p.x,p.y);ctx.rotate(p.rot);ctx.fillStyle=p.c;ctx.fillRect(-p.w/2,-p.h/2,p.w,p.h);ctx.restore();
    }
    if(now-start<3200)requestAnimationFrame(step);
    else canvas.remove();
  };
  requestAnimationFrame(step);
};
