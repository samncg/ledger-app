import { I } from '../../lib/icons';
import { t } from '../../lib/i18n';

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
          {t('card.backup.title')}
        </span>
      </div>
      <div className="backup-grid">
        <button className="btn btn-secondary" onClick={exportData}><I.Download/> {t('card.backup.backupJson')}</button>
        <button className="btn btn-secondary" onClick={exportCSV}><I.Download/> {t('card.backup.exportCsv')}</button>
        <button className="btn btn-ghost" onClick={triggerImport}><I.Upload/> {t('card.backup.loadBackup')}</button>
        <button className="btn btn-ghost" onClick={()=>{setDraftBudget(String(settings.monthlyBudget));setDraftDays(String(settings.periodDays));setDraftStartDate(settings.startDate);setDraftBalance(String(balance?.start||0));setShowSetup(true)}}><I.Edit/> {t('card.backup.editBudget')}</button>
        <button className="btn btn-ghost" onClick={()=>{setMoveMode("budget");setShowTopUp(true)}}>{balancesOn?<I.Wallet/>:<I.Zap/>} {balancesOn?t('card.backup.moveMoney'):t('card.backup.topUp')}</button>
      </div>
      <div className="danger-zone">
        <span className="danger-zone-label">{t('card.backup.localNote')}</span>
        <button className="btn btn-danger btn-sm" onClick={handleClearAll}><I.Trash/> {t('card.backup.clearAll')}</button>
      </div>
    </div>
  );
}
