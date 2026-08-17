import { useEffect, useRef } from 'react';

/* ═══════════════════════════════════════════
   WEATHER EFFECTS
   ═══════════════════════════════════════════ */
export default function WeatherEffect({type,speed}){
  const canvasRef=useRef(null);
  const speedRef=useRef(speed);
  useEffect(()=>{speedRef.current=speed},[speed]);
  useEffect(()=>{
    if(!type||type==='none')return;
    const canvas=canvasRef.current;
    const ctx=canvas.getContext('2d');
    let raf,w,h;
    const resize=()=>{
      w=canvas.width=window.innerWidth;
      h=canvas.height=window.innerHeight;
    };
    resize();
    window.addEventListener('resize',resize);

    const count=type==='snow'?120:220;
    const mk=()=>type==='snow'?{
      x:Math.random()*w,y:Math.random()*h,
      r:Math.random()*2.4+1.1,
      vy:Math.random()*0.6+0.35,
      vx:Math.random()*0.5-0.25,
      drift:Math.random()*Math.PI*2,
      o:Math.random()*0.5+0.4,
    }:{
      x:Math.random()*w,y:Math.random()*h,
      len:Math.random()*16+12,
      v:Math.random()*5+9,
      o:Math.random()*0.25+0.15,
    };
    const particles=Array.from({length:count},mk);

    const tick=()=>{
      const s=speedRef.current||1;
      ctx.clearRect(0,0,w,h);
      if(type==='snow'){
        ctx.fillStyle='#ffffff';
        for(const p of particles){
          p.drift+=0.012*s;
          p.y+=p.vy*s;
          p.x+=p.vx*s+Math.sin(p.drift)*0.35*s;
          if(p.y>h+5){p.y=-5;p.x=Math.random()*w}
          if(p.x>w+5)p.x=-5;
          if(p.x<-5)p.x=w+5;
          ctx.globalAlpha=p.o;
          ctx.beginPath();
          ctx.arc(p.x,p.y,p.r,0,Math.PI*2);
          ctx.fill();
        }
      }else if(type==='rain'){
        ctx.strokeStyle='#bcd6f5';
        ctx.lineWidth=1;
        for(const p of particles){
          p.y+=p.v*s;
          p.x-=p.v*0.22*s;
          if(p.y>h+20){p.y=-20;p.x=Math.random()*w}
          if(p.x<-20)p.x=w+20;
          ctx.globalAlpha=p.o;
          ctx.beginPath();
          ctx.moveTo(p.x,p.y);
          ctx.lineTo(p.x+p.len*0.22,p.y-p.len);
          ctx.stroke();
        }
      }
      ctx.globalAlpha=1;
      raf=window.requestAnimationFrame(tick);
    };
    tick();
    return()=>{window.cancelAnimationFrame(raf);window.removeEventListener('resize',resize)};
  },[type]);

  if(!type||type==='none')return null;
  return<canvas ref={canvasRef} className="weather-canvas"/>;
}
