/* ═══════════════════════════════════════════
   3D TILT — panels lean toward the cursor
   ═══════════════════════════════════════════ */
const TILT_MAX=7; // max tilt in degrees
let tiltActive=false;
const tiltTarget=e=>e.target.closest('.card,.hero,.setup-card');
const tiltMove=e=>{
  if(!tiltActive)return;
  const el=tiltTarget(e);
  if(!el)return;
  const r=el.getBoundingClientRect();
  if(!r.width||!r.height)return;
  const px=(e.clientX-r.left)/r.width-0.5; // -0.5 .. 0.5
  const py=(e.clientY-r.top)/r.height-0.5;
  el.style.setProperty('--tiltY',(px*TILT_MAX*2).toFixed(3)+'deg'); // lean left/right toward the cursor
  el.style.setProperty('--tiltX',(-py*TILT_MAX*2).toFixed(3)+'deg'); // lean up/down toward the cursor
};
const tiltLeave=e=>{
  if(!tiltActive)return;
  const el=tiltTarget(e);
  if(!el||(e.relatedTarget&&el.contains(e.relatedTarget)))return;
  el.style.setProperty('--tiltX','0deg');
  el.style.setProperty('--tiltY','0deg');
};
export const tiltEnable=()=>{
  if(tiltActive)return;
  tiltActive=true;
  document.body.classList.add('tilt-on');
  document.addEventListener('mousemove',tiltMove);
  document.addEventListener('mouseout',tiltLeave);
};
export const tiltDisable=()=>{
  if(!tiltActive)return;
  tiltActive=false;
  document.body.classList.remove('tilt-on');
  document.removeEventListener('mousemove',tiltMove);
  document.removeEventListener('mouseout',tiltLeave);
  document.querySelectorAll('.card,.hero,.setup-card').forEach(el=>{el.style.removeProperty('--tiltX');el.style.removeProperty('--tiltY')});
};
