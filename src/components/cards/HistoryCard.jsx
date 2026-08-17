import { Fragment } from 'react';
import { I } from '../../lib/icons';
import { expCats, relativeDate } from '../../lib/helpers';

/* History — filterable, searchable, sortable transaction list */
export default function HistoryCard({
  expenses,topUps,cats,filterCats,toggleFilterCat,setFilterCats,showFilters,setShowFilters,
  historySearch,setHistorySearch,historySort,setHistorySort,dateFrom,setDateFrom,dateTo,setDateTo,
  activeFilterCount,resetFilters,historyList,historySpentTotal,historyToppedTotal,groupedHistory,
  MYR,today,balancesOn,startEdit,duplicateExpense,removeExpense,removeTopUp,
}){
  return(
    <div className="card fade-in stagger-4">
      <div className="card-title">
        <span className="card-title-left">
          <span className="card-title-icon"><I.History/></span>
          History
          {(expenses.length+topUps.length)>0&&<span className="card-title-count">({expenses.length+topUps.length})</span>}
        </span>
        {(expenses.length+topUps.length)>0&&<button className="link-btn" onClick={resetFilters}>Reset filters</button>}
      </div>

      <div className="cat-pills">
        <button className={`cat-pill ${filterCats.length===0?"active":""}`} onClick={()=>setFilterCats([])}>All</button>
        {cats.map(c=>(
          <button key={c.id} className={`cat-pill ${filterCats.includes(c.id)?"active":""}`}
            onClick={()=>toggleFilterCat(c.id)} style={{'--cat-color':c.color}}>
            <span className="cat-dot"/> {c.label}
          </button>
        ))}
      </div>

      <div className="filter-toggle-row">
        <button className="filter-toggle-btn" onClick={()=>setShowFilters(v=>!v)}>
          {showFilters?"Hide":"Sort & filter"}
          {!showFilters&&activeFilterCount>0?` (${activeFilterCount})`:""}
          <span className={`chevron ${showFilters?"open":""}`}>▾</span>
        </button>
      </div>

      {showFilters&&(
        <div style={{marginTop:10}}>
          <div className="filter-row">
            <input className="input history-search-field" type="text" placeholder="Search notes or categories…" value={historySearch} onChange={e=>setHistorySearch(e.target.value)}/>
            <select className="input sort-select" value={historySort} onChange={e=>setHistorySort(e.target.value)}>
              <option value="date-desc">Newest first</option>
              <option value="date-asc">Oldest first</option>
              <option value="amount-desc">Amount ↓</option>
              <option value="amount-asc">Amount ↑</option>
            </select>
          </div>
          <div className="filter-row">
            <div><div className="field-label">From</div><input className="input mono" type="date" value={dateFrom} max={dateTo||today} onChange={e=>setDateFrom(e.target.value)}/></div>
            <div><div className="field-label">To</div><input className="input mono" type="date" value={dateTo} min={dateFrom} max={today} onChange={e=>setDateTo(e.target.value)}/></div>
          </div>
          {filterCats.length>0&&<div className="hero-stat-note" style={{marginTop:8}}>Transfers are hidden while a category filter is active.</div>}
        </div>
      )}

      <div className="history-summary">
        <span>{historyList.length} {historyList.length===1?"entry":"entries"} · {MYR(historySpentTotal)} spent{historyToppedTotal!==0?` · ${historyToppedTotal>0?'+':''}${MYR(historyToppedTotal)} ${balancesOn?'moved to budget':'topped up'}`:""}</span>
        {filterCats.length>0&&<span style={{color:'var(--text-muted)'}}>{filterCats.length} category filter{filterCats.length===1?"":"s"}</span>}
      </div>

      <div className="tx-list scroll">
        {historyList.length===0&&(
          <div className="empty">
            <div className="empty-illustration">{(expenses.length+topUps.length)===0?"◌":"∅"}</div>
            {(expenses.length+topUps.length)===0?"No spends yet. Add your first one above!":"No entries match your filters."}
            <div className="empty-sub">{(expenses.length+topUps.length)===0?"Data stays on your device.":"Try adjusting search or dates."}</div>
          </div>
        )}
        {groupedHistory.map((g,gi)=>(
          <Fragment key={g.label||gi}>
            {g.label&&(
              <div className="tx-group-header">
                <span>{g.label}</span>
                <span className="tx-group-total mono">{MYR(g.total)}</span>
              </div>
            )}
            {g.items.map(e=>{
              if(e.type==='topup'){
                return(
                  <div className="tx-row" key={`topup-${e.id}`} style={{'--cat-color':e.amount>=0?'var(--positive)':'var(--warning)'}}>
                    <span className="tx-glyph">{e.amount>=0?(balancesOn?<I.Wallet style={{width:15,height:15}}/>:<I.Zap style={{width:15,height:15}}/>):<I.Wallet style={{width:15,height:15}}/>}</span>
                    <div className="tx-main">
                      <div className="tx-cat">{e.amount>=0?(balancesOn?'Move to budget':'Top up'):'Return to balance'}</div>
                      {e.note&&<div className="tx-note">{e.note}</div>}
                    </div>
                    <span className="tx-date">{relativeDate(e.date,today)}</span>
                    <span className="tx-amount" style={{color:e.amount>=0?'var(--positive)':'var(--warning)'}}>{e.amount>=0?'+':''}{MYR(e.amount)}</span>
                    <div className="tx-actions">
                      <button className="tx-action-btn danger" onClick={()=>removeTopUp(e.id)} title="Remove transfer"><I.Trash/></button>
                    </div>
                  </div>
                );
              }
              const es=expCats(e);
              const cat=cats.find(c=>c.id===es[0]);
              return(
                <div className="tx-row" key={e.id} style={{'--cat-color':cat?.color}}>
                  <span className="tx-glyph">{cat?.glyph}</span>
                  <div className="tx-main">
                    <div className="tx-cat">{cat?.label||es[0]}{es.length>1&&<span style={{color:'var(--text-muted)',fontWeight:400}}> +{es.length-1}</span>}</div>
                    {e.note&&<div className="tx-note">{e.note}</div>}
                  </div>
                  <span className="tx-date">{relativeDate(e.date,today)}</span>
                  <span className="tx-amount">{MYR(e.amount)}</span>
                  <div className="tx-actions">
                    <button className="tx-action-btn" onClick={()=>startEdit(e)} title="Edit"><I.Edit/></button>
                    <button className="tx-action-btn" onClick={()=>duplicateExpense(e)} title="Duplicate"><I.Copy/></button>
                    <button className="tx-action-btn danger" onClick={()=>removeExpense(e.id)} title="Delete"><I.Trash/></button>
                  </div>
                </div>
              );
            })}
          </Fragment>
        ))}
      </div>
    </div>
  );
}
