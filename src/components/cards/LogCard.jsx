import { useEffect, useRef, useState } from 'react';
import { I } from '../../lib/icons';
import { CURRENCIES } from '../../lib/constants';
import { t } from '../../lib/i18n';

/* Log a spend — quick-log form with frequent suggestions */
export default function LogCard({
  cats,cur,editingId,amount,setAmount,note,setNote,entryDate,setEntryDate,today,
  selCats,toggleSelCat,frequentEntries,applyFrequent,addExpense,updateExpense,cancelEdit,addFormRef,
  receipt,onReceiptFile,onRemoveReceipt,onViewReceipt,
  tags=[],addTag,removeTag,
}){
  const receiptRef=useRef(null);
  const [tagDraft,setTagDraft]=useState('');
  useEffect(()=>{setTagDraft('')},[editingId]);
  const commitTag=()=>{
    const v=tagDraft.trim();
    if(v&&addTag)addTag(v);
    setTagDraft('');
  };
  return(
    <div className="card fade-in stagger-1" ref={addFormRef}>
      <div className="card-title">
        <span className="card-title-left">
          <span className="card-title-icon"><I.Wallet/></span>
          {editingId?t('log.edit'):t('log.title')}
        </span>
        {editingId?<span className="edit-badge">{t('log.editing')}</span>:<span className="kbd">⌘N</span>}
      </div>

      {!editingId&&frequentEntries.length>0&&(
        <div className="frequent-wrap">
          <div className="field-label">{t('log.frequent')}</div>
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

      <div className="field-label">{t('log.amount')}</div>
      <div className="form-row">
        <input className="input mono amount-field" type="number" inputMode="decimal" placeholder="0.00" value={amount}
          onChange={e=>setAmount(e.target.value)}
          onKeyDown={e=>e.key==='Enter'&&(editingId?updateExpense():addExpense())}
        />
        <input className="input" type="text" placeholder={t('log.notePlaceholder')} value={note}
          onChange={e=>setNote(e.target.value)}
          onKeyDown={e=>e.key==='Enter'&&(editingId?updateExpense():addExpense())}
        />
      </div>

      <div className="quick-amounts">
        {[5,10,15,20,50,100].map(v=>(
          <button key={v} className="quick-amt" type="button" onClick={()=>setAmount(String(v))}>{CURRENCIES[cur].symbol}{v}</button>
        ))}
      </div>

      <div className="field-label">{t('log.date')}</div>
      <input className="input mono" type="date" value={entryDate} max={today} onChange={e=>setEntryDate(e.target.value)} style={{marginBottom:8}}/>

      <div className="field-label">{t('log.receipt')} <span style={{color:'var(--text-muted)',fontWeight:400}}>— {t('log.optional')}</span></div>
      <input ref={receiptRef} type="file" accept="image/*" style={{display:'none'}}
        onChange={e=>{const f=e.target.files?.[0];if(f&&onReceiptFile)onReceiptFile(f);e.target.value=''}}/>
      {receipt?(
        <div className="receipt-preview">
          <img src={receipt} alt={t('common.receipt')} onClick={()=>onViewReceipt&&onViewReceipt(receipt)}/>
          <button type="button" className="btn btn-ghost btn-sm" onClick={onRemoveReceipt}>{t('log.remove')}</button>
        </div>
      ):(
        <button type="button" className="btn btn-secondary btn-sm" onClick={()=>receiptRef.current&&receiptRef.current.click()}><I.Upload/> {t('log.attachPhoto')}</button>
      )}

      <div className="field-label">{t('log.tags')} <span style={{color:'var(--text-muted)',fontWeight:400}}>— {t('log.optional')}</span></div>
      {tags.length>0&&(
        <div className="tag-chips">
          {tags.map(tg=>(
            <span className="tag-chip" key={tg}>
              #{tg}
              <button type="button" className="tag-chip-x" onClick={()=>removeTag&&removeTag(tg)} title={t('log.removeTag')}>×</button>
            </span>
          ))}
        </div>
      )}
      <input className="input tag-input" type="text" placeholder={t('log.tagPlaceholder')} value={tagDraft}
        onChange={e=>setTagDraft(e.target.value)}
        onKeyDown={e=>{
          if(e.key==='Enter'){e.preventDefault();commitTag();}
          else if(e.key===','||e.key==='Tab'){if(tagDraft.trim()){e.preventDefault();commitTag();}}
          else if(e.key==='Backspace'&&!tagDraft&&tags.length>0&&removeTag){removeTag(tags[tags.length-1]);}
        }}
        onBlur={commitTag}
      />

      <div className="field-label">{t('log.categories')} <span style={{color:'var(--text-muted)',fontWeight:400}}>— {t('log.pickOne')}</span></div>
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
            <I.Check/> {t('log.update')}
          </button>
          <button className="btn btn-ghost" onClick={cancelEdit} style={{flex:'0 0 auto'}}>{t('log.cancel')}</button>
        </div>
      ):(
        <button className="btn btn-block" onClick={addExpense} disabled={!amount||parseFloat(amount)<=0}>
          <I.Plus/> {t('log.addSpend')}
        </button>
      )}
    </div>
  );
}
