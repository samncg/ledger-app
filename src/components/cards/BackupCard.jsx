import { I } from '../../lib/icons';

/* Data & backup */
export default function BackupCard({
  exportData,exportCSV,triggerImport,settings,setDraftBudget,setDraftDays,
  setDraftStartDate,setDraftBalance,setShowSetup,setMoveMode,setShowTopUp,
  balancesOn,handleClearAll,balance,
}){
  return(
    <div className="card fade-in">
      <div className="card-title">
        <span className="card-title-left">
          <span className="card-title-icon"><I.Database/></span>
          Data & backup
        </span>
      </div>
      <div className="backup-grid">
        <button className="btn btn-secondary" onClick={exportData}><I.Download/> Backup (JSON)</button>
        <button className="btn btn-secondary" onClick={exportCSV}><I.Download/> Export CSV</button>
        <button className="btn btn-ghost" onClick={triggerImport}><I.Upload/> Load backup</button>
        <button className="btn btn-ghost" onClick={()=>{setDraftBudget(String(settings.monthlyBudget));setDraftDays(String(settings.periodDays));setDraftStartDate(settings.startDate);setDraftBalance(String(balance?.start||0));setShowSetup(true)}}><I.Edit/> Edit budget</button>
        <button className="btn btn-ghost" onClick={()=>{setMoveMode("budget");setShowTopUp(true)}}>{balancesOn?<I.Wallet/>:<I.Zap/>} {balancesOn?'Move money':'Top up'}</button>
      </div>
      <div className="danger-zone">
        <span className="danger-zone-label">Your data is stored locally in your browser. Nothing is sent anywhere.</span>
        <button className="btn btn-danger btn-sm" onClick={handleClearAll}><I.Trash/> Clear all</button>
      </div>
    </div>
  );
}
