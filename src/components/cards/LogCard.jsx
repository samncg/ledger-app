import { I } from '../../lib/icons';
import { CURRENCIES } from '../../lib/constants';

/* Log a spend — quick-log form with frequent suggestions */
export default function LogCard({
  cats,cur,editingId,amount,setAmount,note,setNote,entryDate,setEntryDate,today,
  selCats,toggleSelCat,frequentEntries,applyFrequent,addExpense,updateExpense,cancelEdit,addFormRef,
}){
  return(
    <div className="card fade-in stagger-1" ref={addFormRef}>
      <div className="card-title">
        <span className="card-title-left">
          <span className="card-title-icon"><I.Wallet/></span>
          {editingId?"Edit spend":"Log a spend"}
        </span>
        {editingId?<span className="edit-badge">Editing</span>:<span className="kbd">⌘N</span>}
      </div>

      {!editingId&&frequentEntries.length>0&&(
        <div className="frequent-wrap">
          <div className="field-label">Frequent</div>
          <div className="frequent-chips">
            {frequentEntries.map((f,i)=>{
              const fc=cats.find(c=>c.id===f.category);
              return(
                <button key={i} type="button" className="frequent-chip" onClick={()=>applyFrequent(f)}>
                  <span className="cat-dot" style={{background:fc?.color||'var(--accent)'}}/>
                  <span className="frequent-chip-note">{f.note||fc?.label||f.category}</span>
                  <span className="frequent-chip-amt mono">{CURRENCIES[cur].symbol}{f.amount}</span>
                </button>
              );
            })}
          </div>
        </div>
      )}

      <div className="field-label">Amount</div>
      <div className="form-row">
        <input className="input mono amount-field" type="number" inputMode="decimal" placeholder="0.00" value={amount}
          onChange={e=>setAmount(e.target.value)}
          onKeyDown={e=>e.key==='Enter'&&(editingId?updateExpense():addExpense())}
        />
        <input className="input" type="text" placeholder="Note (optional)" value={note}
          onChange={e=>setNote(e.target.value)}
          onKeyDown={e=>e.key==='Enter'&&(editingId?updateExpense():addExpense())}
        />
      </div>

      <div className="quick-amounts">
        {[5,10,15,20,50,100].map(v=>(
          <button key={v} className="quick-amt" type="button" onClick={()=>setAmount(String(v))}>{CURRENCIES[cur].symbol}{v}</button>
        ))}
      </div>

      <div className="field-label">Date</div>
      <input className="input mono" type="date" value={entryDate} max={today} onChange={e=>setEntryDate(e.target.value)} style={{marginBottom:8}}/>

      <div className="field-label">Categories <span style={{color:'var(--text-muted)',fontWeight:400}}>— pick one or more</span></div>
      <div className="cat-pills">
        {cats.map(c=>(
          <button key={c.id} className={`cat-pill ${selCats.includes(c.id)?"active":""}`}
            onClick={()=>toggleSelCat(c.id)} style={{'--cat-color':c.color}}>
            <span className="cat-dot"/> {c.label}
          </button>
        ))}
      </div>

      {editingId?(
        <div style={{display:'flex',gap:8}}>
          <button className="btn btn-block" onClick={updateExpense} disabled={!amount||parseFloat(amount)<=0}>
            <I.Check/> Update
          </button>
          <button className="btn btn-ghost" onClick={cancelEdit} style={{flex:'0 0 auto'}}>Cancel</button>
        </div>
      ):(
        <button className="btn btn-block" onClick={addExpense} disabled={!amount||parseFloat(amount)<=0}>
          <I.Plus/> Add spend
        </button>
      )}
    </div>
  );
}
