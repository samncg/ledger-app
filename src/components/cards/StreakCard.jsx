import { I } from '../../lib/icons';
import { t } from '../../lib/i18n';

/* ═══════════════════════════════════════════
   STREAK — log a spend every day to keep the run alive
   ═══════════════════════════════════════════ */
export default function StreakCard({streak,best,loggedToday,grace}){
  return(
    <div className="card fade-in stagger-3">
      <div className="card-title">
        <span className="card-title-left">
          <span className="card-title-icon"><I.Fire/></span>
          {t('card.streak.title')}
        </span>
      </div>

      <div className="streak-body">
        <div className="streak-big">
          <span className="streak-big-num mono">{streak}</span>
          <span className="streak-big-unit">{streak===1?t('card.streak.dayWord'):t('card.streak.daysWord')}</span>
        </div>
        <div className="streak-detail">
          <div>{t('card.streak.best')} <strong>{best} {best===1?t('card.streak.day',{n:1}):t('card.streak.days',{n:best})}</strong></div>
          <div>{loggedToday?t('card.streak.loggedToday'):t('card.streak.notLoggedToday')}</div>
          <div className="streak-hint">{t('card.streak.hint')}</div>
          {grace>0&&<div className="streak-hint">{t('card.streak.graceOn',{n:grace})}</div>}
        </div>
      </div>
    </div>
  );
}
