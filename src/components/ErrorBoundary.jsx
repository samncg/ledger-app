import React from 'react';

/* If a render ever throws (e.g. from a bad synced copy), show a recoverable
   screen instead of a black/blank page. */
export default class LedgerErrorBoundary extends React.Component{
  constructor(props){super(props);this.state={err:null}}
  static getDerivedStateFromError(err){return{err}}
  componentDidCatch(err,info){console.error("Ledger crashed:",err,info)}
  render(){
    if(this.state.err){
      const btn={fontFamily:'inherit',fontSize:13,fontWeight:600,padding:'9px 14px',borderRadius:8,border:'none',cursor:'pointer',color:'#0b0d10',background:'#e8e8e8',marginRight:8};
      return(
        <div style={{maxWidth:520,margin:'80px auto',padding:28,fontFamily:"system-ui,-apple-system,sans-serif",color:'#e8e8e8',background:'#0b0d10',borderRadius:14}}>
          <div style={{fontSize:20,fontWeight:700,marginBottom:8}}>Something went wrong</div>
          <div style={{fontSize:13,color:'#9a9a9a',marginBottom:14}}>Usually caused by a corrupt synced copy — reloading usually fixes it. If it keeps happening, reset local data (your cloud copy is preserved).</div>
          <code style={{display:'block',fontSize:11,color:'#f88',marginBottom:18,wordBreak:'break-word'}}>{String((this.state.err&&this.state.err.message)||this.state.err)}</code>
          <button style={btn} onClick={()=>location.reload()}>Reload</button>
          <button style={{...btn,background:'#c0392b',color:'#fff'}} onClick={()=>{Object.keys(localStorage).filter(k=>k.startsWith('ledger-')).forEach(k=>localStorage.removeItem(k));location.reload()}}>Reset local data &amp; reload</button>
        </div>
      );
    }
    return this.props.children;
  }
}
