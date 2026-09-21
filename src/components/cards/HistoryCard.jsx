import { Fragment } from 'react';
import { I } from '../../lib/icons';
import { expCats, fmt, relativeDate } from '../../lib/helpers';
import { t } from '../../lib/i18n';

/* History — filterable, searchable, sortable transaction list */
export default function HistoryCard({
  expenses,topUps,cats,filterCats,toggleFilterCat,setFilterCats,showFilters,setShowFilters,
  historySearch,setHistorySearch,historySort,setHistorySort,dateFrom,setDateFrom,dateTo,setDateTo,
  activeFilterCount,resetFilters,historyList,historySpentTotal,historyToppedTotal,groupedHistory,
  MYR,today,balancesOn,startEdit,duplicateExpense,removeExpense,removeTopUp,onViewReceipt,
  filterTags=[],toggleFilterTag,setFilterTags,allTags=[],
}){
  return(
    <div className="card fade-in stagger-4">
      <div className="card-title">
        <span className="card-title-left">
          <span className="card-title-icon"><I.History/></span>
          {t('history.title')}
          {(expenses.length+topUps.length)>0&&<span className="card-title-count">({expenses.length+topUps.length})</span>}
        </span>
        {(expenses.length+topUps.length)>0&&<button className="link-btn" onClick={resetFilters}>{t('history.resetFilters')}</button>}
      </div>

      <div className="cat-pills">
        <button className={`cat-pill ${filterCats.length===0?"active":""}`} onClick={()=>setFilterCats([])}>{t('history.all')}</button>
        {cats.map(c=>(
          <button key={c.id} className={`cat-pill ${filterCats.includes(c.id)?"active":""}`}
            onClick={()=>toggleFilterCat(c.id)} style={{'--cat-color':c.color}}>
            <span className="cat-dot"/> {c.label}
          </button>
        ))}
      </div>

      <div className="filter-toggle-row">
        <button className="filter-toggle-btn" onClick={()=>setShowFilters(v=>!v)}>
          {showFilters?t('history.hide'):t('history.sortFilter')}
          {!showFilters&&activeFilterCount>0?` (${activeFilterCount})`:""}
          <span className={`chevron ${showFilters?"open":""}`}>▾</span>
        </button>
      </div>

      {showFilters&&(
        <div style={{marginTop:10}}>
          <div className="filter-row">
            <input className="input history-search-field" type="text" placeholder={t('history.search')} value={historySearch} onChange={e=>setHistorySearch(e.target.value)}/>
            <select className="input sort-select" value={historySort} onChange={e=>setHistorySort(e.target.value)}>
              <option value="date-desc">{t('history.newest')}</option>
              <option value="date-asc">{t('history.oldest')}</option>
              <option value="amount-desc">{t('history.amountDown')}</option>
              <option value="amount-asc">{t('history.amountUp')}</option>
            </select>
          </div>
          <div className="filter-row">
            <div><div className="field-label">{t('history.from')}</div><input className="input mono" type="date" value={dateFrom} max={dateTo||today} onChange={e=>setDateFrom(e.target.value)}/></div>
            <div><div className="field-label">{t('history.to')}</div><input className="input mono" type="date" value={dateTo} min={dateFrom} max={today} onChange={e=>setDateTo(e.target.value)}/></div>
          </div>
          {allTags.length>0&&(
            <div style={{marginTop:10}}>
              <div className="field-label">{t('history.tags')}</div>
              <div className="cat-pills tag-filter-pills">
                <button className={`cat-pill ${filterTags.length===0?"active":""}`} onClick={()=>setFilterTags&&setFilterTags([])}>{t('history.any')}</button>
                {allTags.map(tg=>(
                  <button key={tg} className={`cat-pill ${filterTags.includes(tg)?"active":""}`} onClick={()=>toggleFilterTag&&toggleFilterTag(tg)}>#{tg}</button>
                ))}
              </div>
            </div>
          )}
          {filterCats.length>0&&<div className="hero-stat-note" style={{marginTop:8}}>{t('history.transfersHidden')}</div>}
        </div>
      )}

      <div className="history-summary">
        <span>{historyList.length} {historyList.length===1?t('history.entry'):t('history.entries')} · {MYR(historySpentTotal)} {t('history.spent')}{historyToppedTotal!==0?` · ${historyToppedTotal>0?'+':''}${MYR(historyToppedTotal)} ${balancesOn?t('history.movedToBudget'):t('history.toppedUp')}`:""}</span>
        {filterCats.length>0&&<span style={{color:'var(--text-muted)'}}>{filterCats.length} {filterCats.length===1?t('history.categoryFilter'):t('history.categoryFilters')}</span>}
        {filterTags.length>0&&<span style={{color:'var(--text-muted)'}}>{filterTags.length} {filterTags.length===1?t('history.tagFilter'):t('history.tagFilters')}</span>}
      </div>

      <div className="tx-list scroll">
        {historyList.length===0&&(
          <div className="empty">
            <div className="empty-illustration">{(expenses.length+topUps.length)===0?"◌":"∅"}</div>
            {(expenses.length+topUps.length)===0?t('history.noSpends'):t('history.noMatch')}
            <div className="empty-sub">{(expenses.length+topUps.length)===0?t('history.dataStays'):t('history.tryAdjust')}</div>
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
                      <div className="tx-cat">{e.amount>=0?(balancesOn?t('history.moveToBudget'):t('history.topUp')):t('history.returnToBalance')}</div>
                      {e.note&&<div className="tx-note">{e.note}</div>}
                    </div>
                    <span className="tx-date">{relativeDate(e.date,today)}</span>
                    <span className="tx-amount" style={{color:e.amount>=0?'var(--positive)':'var(--warning)'}}>{e.amount>=0?'+':''}{MYR(e.amount)}</span>
                    <div className="tx-actions">
                      <button className="tx-action-btn danger" onClick={()=>removeTopUp(e.id)} title={t('common.removeTransfer')}><I.Trash/></button>
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
                    {Array.isArray(e.tags)&&e.tags.length>0&&(
                      <div className="tx-tags">{e.tags.map(t=><span className="tx-tag" key={t}>#{t}</span>)}</div>
                    )}
                    {e.receipt&&<img className="tx-receipt" src={e.receipt} alt={t('common.receipt')} onClick={()=>onViewReceipt&&onViewReceipt(e.receipt)}/>}
                  </div>
                  <span className="tx-date">{relativeDate(e.date,today)}</span>
                  <span className="tx-amount">
                    {e.currency&&typeof e.foreignAmount==='number'?(
                      <>
                        <span className="tx-amount-foreign">{fmt(e.foreignAmount,e.currency)}</span>
                        <span className="tx-amount-home">≈ {MYR(e.amount)}</span>
                      </>
                    ):MYR(e.amount)}
                  </span>
                  <div className="tx-actions">
                    <button className="tx-action-btn" onClick={()=>startEdit(e)} title={t('common.edit')}><I.Edit/></button>
                    <button className="tx-action-btn" onClick={()=>duplicateExpense(e)} title={t('common.duplicate')}><I.Copy/></button>
                    <button className="tx-action-btn danger" onClick={()=>removeExpense(e.id)} title={t('common.delete')}><I.Trash/></button>
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
