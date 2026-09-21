import React, { useState, useRef } from 'react';
import { I } from '../../lib/icons';
import { PIGGY_GIF } from '../../lib/constants';
import { playPiggySound } from '../../lib/sound';
import { t } from '../../lib/i18n';

/* Piggy bank — multiple savings goals with customizable picture, sounds, and names */
export default function PiggyCard({
  piggies = [],
  activePiggyId,
  setActivePiggyId,
  addPiggy,
  renamePiggy,
  savePiggyTarget,
  depositPiggy,
  breakPiggy,
  deletePiggy,
  updatePiggyTexture,
  updatePiggySound,
  MYR,
  showToast,
}){
  const safePiggies = Array.isArray(piggies) && piggies.length > 0
    ? piggies
    : [{ id: 'default', name: t('card.piggy.defaultName'), target: 0, saved: 0 }];

  const activePiggy = safePiggies.find(p => p.id === activePiggyId) || safePiggies[0];
  const activeId = activePiggy.id;

  const [isCreating, setIsCreating] = useState(false);
  const [newName, setNewName] = useState('');
  const [newTarget, setNewTarget] = useState('');

  const [isRenaming, setIsRenaming] = useState(false);
  const [renameDraft, setRenameDraft] = useState('');

  const [isEditingGoal, setIsEditingGoal] = useState(false);
  const [goalDraft, setGoalDraft] = useState('');

  const [isAddOpen, setIsAddOpen] = useState(false);
  const [addAmount, setAddAmount] = useState('');

  const [showSettings, setShowSettings] = useState(false);

  const textureInputRef = useRef(null);
  const soundInputRef = useRef(null);

  const saved = activePiggy.saved || 0;
  const target = activePiggy.target || 0;
  const pct = target > 0 ? Math.min(100, (saved / target) * 100) : 0;

  const handleStartRename = () => {
    setRenameDraft(activePiggy.name || t('card.piggy.defaultName'));
    setIsRenaming(true);
  };

  const handleSaveRename = () => {
    if (renameDraft.trim()) {
      renamePiggy(activeId, renameDraft.trim());
    }
    setIsRenaming(false);
  };

  const handleStartGoalEdit = () => {
    setGoalDraft(target > 0 ? String(target) : '');
    setIsEditingGoal(true);
  };

  const handleSaveGoal = () => {
    const val = parseFloat(goalDraft);
    if (!isFinite(val) || val < 0) {
      if (showToast) showToast(t('card.piggy.invalidGoal'), 'error');
      return;
    }
    savePiggyTarget(activeId, val);
    setIsEditingGoal(false);
  };

  const handleCreatePiggy = () => {
    const name = newName.trim() || t('card.piggy.newName', { n: safePiggies.length + 1 });
    const tVal = parseFloat(newTarget) || 0;
    addPiggy(name, tVal);
    setNewName('');
    setNewTarget('');
    setIsCreating(false);
  };

  const handleDeposit = () => {
    const val = parseFloat(addAmount);
    if (!val || val <= 0) {
      if (showToast) showToast(t('card.piggy.invalidAmount'), 'error');
      return;
    }
    depositPiggy(activeId, val);
    setAddAmount('');
    setIsAddOpen(false);
  };

  const handleTextureFile = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) {
      if (showToast) showToast(t('card.piggy.imageTooLarge'), 'error');
      e.target.value = '';
      return;
    }
    const reader = new FileReader();
    reader.onload = (ev) => {
      updatePiggyTexture(activeId, ev.target.result);
      if (showToast) showToast(t('card.piggy.pictureUpdated'), 'success');
    };
    reader.readAsDataURL(file);
    e.target.value = '';
  };

  const handleSoundFile = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) {
      if (showToast) showToast(t('card.piggy.audioTooLarge'), 'error');
      e.target.value = '';
      return;
    }
    const reader = new FileReader();
    reader.onload = (ev) => {
      updatePiggySound(activeId, 'custom', ev.target.result);
      if (showToast) showToast(t('card.piggy.soundUploaded'), 'success');
    };
    reader.readAsDataURL(file);
    e.target.value = '';
  };

  return (
    <div className="card fade-in">
      <input ref={textureInputRef} type="file" accept="image/*" style={{ display: 'none' }} onChange={handleTextureFile} />
      <input ref={soundInputRef} type="file" accept="audio/*" style={{ display: 'none' }} onChange={handleSoundFile} />

      <div className="card-title">
        <span className="card-title-left">
          <span className="card-title-icon"><I.Target /></span>
          {t('card.piggy.title')}
          {safePiggies.length > 1 && (
            <span className="card-title-count">{safePiggies.length}</span>
          )}
        </span>
        <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
          {!isEditingGoal && (
            <button className="link-btn" onClick={handleStartGoalEdit}>
              {target > 0 ? t('card.piggy.editGoal') : t('card.piggy.setGoal')}
            </button>
          )}
          <button
            className={`icon-btn ${showSettings ? 'active' : ''}`}
            onClick={() => setShowSettings(s => !s)}
            title={t('card.piggy.settingsTitle')}
            style={{ width: 28, height: 28 }}
          >
            <I.Palette style={{ width: 14, height: 14 }} />
          </button>
        </div>
      </div>

      {/* Piggy tabs */}
      <div className="piggy-tabs-row">
        {safePiggies.map(p => (
          <button
            key={p.id}
            className={`piggy-tab-btn ${p.id === activeId ? 'active' : ''}`}
            onClick={() => { setActivePiggyId(p.id); setIsRenaming(false); setIsEditingGoal(false); setIsAddOpen(false); }}
          >
            <span>{p.name || t('card.piggy.defaultName')}</span>
            <span style={{ opacity: 0.8, fontSize: 11, fontFamily: 'var(--font-mono, monospace)' }}>{MYR(p.saved || 0)}</span>
          </button>
        ))}
        {!isCreating && (
          <button className="piggy-tab-new" onClick={() => setIsCreating(true)}>
            <I.Plus style={{ width: 13, height: 13 }} /> {t('card.piggy.new')}
          </button>
        )}
      </div>

      {/* New Piggy form */}
      {isCreating && (
        <div className="piggy-settings-panel" style={{ marginTop: 4, marginBottom: 12 }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--text)' }}>{t('card.piggy.createTitle')}</div>
          <div className="filter-row" style={{ marginBottom: 6 }}>
            <input
              className="input"
              type="text"
              placeholder={t('card.piggy.namePlaceholder')}
              value={newName}
              onChange={e => setNewName(e.target.value)}
              autoFocus
            />
            <input
              className="input mono"
              type="number"
              inputMode="decimal"
              placeholder={t('card.piggy.targetPlaceholder')}
              value={newTarget}
              onChange={e => setNewTarget(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleCreatePiggy()}
            />
          </div>
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
            <button className="btn btn-secondary btn-sm" onClick={handleCreatePiggy}>{t('card.piggy.create')}</button>
            <button className="btn btn-ghost btn-sm" onClick={() => { setIsCreating(false); setNewName(''); setNewTarget(''); }}>{t('card.piggy.cancel')}</button>
          </div>
        </div>
      )}

      {/* Active piggy header & name */}
      <div className="piggy-header-row">
        <div className="piggy-title-box">
          {isRenaming ? (
            <div style={{ display: 'flex', gap: 6, alignItems: 'center', flex: 1 }}>
              <input
                className="input"
                type="text"
                value={renameDraft}
                onChange={e => setRenameDraft(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleSaveRename()}
                autoFocus
                style={{ padding: '4px 8px', height: 32, fontSize: 14 }}
              />
              <button className="btn btn-sm" onClick={handleSaveRename} title={t('card.piggy.saveName')}><I.Check style={{ width: 13, height: 13 }} /></button>
              <button className="btn btn-ghost btn-sm" onClick={() => setIsRenaming(false)} title={t('card.piggy.cancel')}><I.Close style={{ width: 13, height: 13 }} /></button>
            </div>
          ) : (
            <>
              <span className="piggy-name">{activePiggy.name || t('card.piggy.defaultName')}</span>
              <button className="piggy-rename-btn" onClick={handleStartRename} title={t('card.piggy.renameTitle')}>
                <I.Edit style={{ width: 13, height: 13 }} />
              </button>
            </>
          )}
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          {target > 0 && <span className="card-title-count">{Math.round(pct)}%</span>}
          {safePiggies.length > 1 && (
            <button
              className="btn btn-ghost btn-sm"
              onClick={() => deletePiggy(activeId)}
              title={t('card.piggy.deleteTitle')}
              style={{ color: 'var(--negative)', padding: '3px 7px' }}
            >
              <I.Trash style={{ width: 13, height: 13 }} />
            </button>
          )}
        </div>
      </div>

      {/* Main Piggy Stage */}
      <div className="piggy-wrap">
        <div className="piggy-stage">
          <div style={{ width: 120, maxWidth: '40%', flexShrink: 0 }}>
            <img
              className={activePiggy.texture ? 'piggy-img' : 'piggy-img default'}
              src={activePiggy.texture || PIGGY_GIF}
              alt={activePiggy.name || t('card.piggy.defaultName')}
            />
          </div>

          <div className="piggy-info">
            {isEditingGoal ? (
              <div className="currency-row">
                <input
                  className="input mono"
                  type="number"
                  inputMode="decimal"
                  placeholder="200"
                  value={goalDraft}
                  onChange={e => setGoalDraft(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && handleSaveGoal()}
                  autoFocus
                />
                <button className="btn btn-sm" onClick={handleSaveGoal} title={t('card.piggy.saveGoal')}><I.Check /></button>
                <button className="btn btn-ghost btn-sm" onClick={() => setIsEditingGoal(false)} title={t('card.piggy.cancel')}><I.Close /></button>
              </div>
            ) : (
              <div className="totals-row" style={{ paddingBottom: 0, borderBottom: 'none', marginBottom: 0 }}>
                <div>
                  <span className="totals-label">{target > 0 ? t('card.piggy.saved') : t('card.piggy.noGoal')}</span>
                  <div className="totals-value mono" style={{ fontSize: 20 }}>
                    {MYR(saved)}
                    {target > 0 && (
                      <span style={{ color: 'var(--text-muted)', fontSize: 12, fontWeight: 600 }}> / {MYR(target)}</span>
                    )}
                  </div>
                </div>
              </div>
            )}

            <div className="piggy-progress" style={{ marginTop: 10 }}>
              <div className="piggy-progress-fill" style={{ width: `${pct}%` }} />
            </div>

            <div className="hero-stat-note" style={{ marginTop: 6 }}>
              {target > 0
                ? (saved >= target ? t('card.piggy.goalComplete') : t('card.piggy.toGo', { amount: MYR(target - saved) }))
                : t('card.piggy.setGoalHint')}
            </div>

            <div className="piggy-actions" style={{ marginTop: 10 }}>
              <button className="btn btn-secondary btn-sm" onClick={() => setIsAddOpen(o => !o)}>
                <I.Plus /> {t('card.piggy.addFunds')}
              </button>
              <button className="btn btn-ghost btn-sm" onClick={() => breakPiggy(activeId)} disabled={saved <= 0}>
                {t('card.piggy.break')}
              </button>
            </div>

            {isAddOpen && (
              <div className="piggy-amt">
                <input
                  className="input mono"
                  type="number"
                  inputMode="decimal"
                  placeholder="20"
                  value={addAmount}
                  onChange={e => setAddAmount(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && handleDeposit()}
                  autoFocus
                />
                <button className="btn btn-sm" onClick={handleDeposit}>{t('card.piggy.deposit')}</button>
                <button className="btn btn-ghost btn-sm" onClick={() => setIsAddOpen(false)}><I.Close /></button>
              </div>
            )}
          </div>
        </div>

        {/* Customizable picture and sound settings panel */}
        {showSettings && (
          <div className="piggy-settings-panel">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: 12, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-dim)' }}>
                {t('card.piggy.customize', { name: activePiggy.name || t('card.piggy.defaultName') })}
              </span>
              <button className="icon-btn" onClick={() => setShowSettings(false)} style={{ width: 22, height: 22 }}>
                <I.Close style={{ width: 12, height: 12 }} />
              </button>
            </div>

            {/* Picture customization */}
            <div>
              <div className="field-label" style={{ marginBottom: 6 }}>{t('card.piggy.pictureLabel')}</div>
              <div className="piggy-settings-grid">
                <button className="btn btn-secondary btn-sm" onClick={() => textureInputRef.current?.click()}>
                  <I.Upload style={{ width: 13, height: 13 }} /> {t('card.piggy.customPicture')}
                </button>
                {activePiggy.texture && (
                  <button className="btn btn-ghost btn-sm" onClick={() => updatePiggyTexture(activeId, null)}>
                    <I.Trash style={{ width: 13, height: 13 }} /> {t('card.piggy.resetPicture')}
                  </button>
                )}
              </div>
            </div>

            {/* Sound customization */}
            <div>
              <div className="field-label" style={{ marginBottom: 6 }}>{t('card.piggy.depositSound')}</div>
              <div className="drawer-tabs" style={{ marginBottom: 8 }}>
                <button
                  className={`drawer-tab ${(activePiggy.soundId || 'coin') === 'coin' ? 'active' : ''}`}
                  onClick={() => updatePiggySound(activeId, 'coin')}
                >
                  {t('card.piggy.soundCoin')}
                </button>
                <button
                  className={`drawer-tab ${(activePiggy.soundId || 'coin') === 'chime' ? 'active' : ''}`}
                  onClick={() => updatePiggySound(activeId, 'chime')}
                >
                  {t('card.piggy.soundChime')}
                </button>
                <button
                  className={`drawer-tab ${(activePiggy.soundId || 'coin') === 'custom' ? 'active' : ''}`}
                  onClick={() => {
                    if (activePiggy.soundCustom) {
                      updatePiggySound(activeId, 'custom');
                    } else {
                      if (showToast) showToast(t('card.piggy.uploadFirst'), 'info');
                      soundInputRef.current?.click();
                    }
                  }}
                >
                  {t('card.piggy.soundCustom')}
                </button>
                <button
                  className={`drawer-tab ${(activePiggy.soundId || 'coin') === 'none' ? 'active' : ''}`}
                  onClick={() => updatePiggySound(activeId, 'none')}
                >
                  {t('card.piggy.soundMute')}
                </button>
              </div>

              <div className="piggy-settings-grid">
                <button className="btn btn-secondary btn-sm" onClick={() => soundInputRef.current?.click()}>
                  <I.Upload style={{ width: 13, height: 13 }} /> {t('card.piggy.uploadSound')}
                </button>
                {activePiggy.soundCustom && (
                  <button
                    className="btn btn-ghost btn-sm"
                    onClick={() => updatePiggySound(activeId, activePiggy.soundId === 'custom' ? 'coin' : activePiggy.soundId, null)}
                  >
                    <I.Trash style={{ width: 13, height: 13 }} /> {t('card.piggy.removeSound')}
                  </button>
                )}
              </div>

              {activePiggy.soundId !== 'none' && (
                <button
                  className="link-btn"
                  style={{ marginTop: 6 }}
                  onClick={() => playPiggySound(activePiggy.soundId || 'coin', activePiggy.soundCustom)}
                >
                  {t('card.piggy.previewSound')}
                </button>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
