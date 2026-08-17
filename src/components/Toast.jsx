export default function Toast({toast,onDismiss}){
  if(!toast)return null;
  return(
    <div className={`toast ${toast.type}`}>
      <span className="toast-dot"/>
      <span>{toast.msg}</span>
      {toast.action&&<button className="toast-action" onClick={()=>{toast.action.run();onDismiss(toast.id)}}>{toast.action.label}</button>}
    </div>
  );
}
