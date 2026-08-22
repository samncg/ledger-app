import React, { useState, useRef } from 'react';
import { I } from '../../lib/icons';
import { PIGGY_GIF } from '../../lib/constants';
import { playPiggySound } from '../../lib/sound';

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
    : [{ id: 'default', name: 'Piggy bank', target: 0, saved: 0 }];

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
    setRenameDraft(activePiggy.name || 'Piggy bank');
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
      if (showToast) showToast('Enter a valid goal amount.', 'error');
      return;
    }
    savePiggyTarget(activeId, val);
    setIsEditingGoal(false);
  };

  const handleCreatePiggy = () => {
    const name = newName.trim() || `Piggy #${safePiggies.length + 1}`;
    const tVal = parseFloat(newTarget) || 0;
    addPiggy(name, tVal);
    setNewName('');
    setNewTarget('');
    setIsCreating(false);
  };

  const handleDeposit = () => {
    const val = parseFloat(addAmount);
    if (!val || val <= 0) {
      if (showToast) showToast('Enter a valid amount.', 'error');
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
      if (showToast) showToast('Image file is too large (max 2MB).', 'error');
      return;
    }
    const reader = new FileReader();
    reader.onload = (ev) => {
      updatePiggyTexture(activeId, ev.target.result);
      if (showToast) showToast('Custom picture updated.', 'success');
    };
    reader.readAsDataURL(file);
    e.target.value = '';
  };

  const handleSoundFile = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) {
      if (showToast) showToast('Audio file is too large (max 2MB).', 'error');
      return;
    }
    const reader = new FileReader();
    reader.onload = (ev) => {
      updatePiggySound(activeId, 'custom', ev.target.result);
      if (showToast) showToast('Custom sound uploaded.', 'success');
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
          Piggy banks
          {safePiggies.length > 1 && (
            <span className="card-title-count">{safePiggies.length}</span>
          )}
        </span>
        <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
          {!isEditingGoal && (
            <button className="link-btn" onClick={handleStartGoalEdit}>
              {target > 0 ? 'Edit goal' : 'Set goal'}
            </button>
          )}
          <button
            className={`icon-btn ${showSettings ? 'active' : ''}`}
            onClick={() => setShowSettings(s => !s)}
            title="Picture & sound settings"
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
            <span>{p.name || 'Piggy bank'}</span>
            <span style={{ opacity: 0.8, fontSize: 11, fontFamily: 'var(--font-mono, monospace)' }}>{MYR(p.saved || 0)}</span>
          </button>
        ))}
        {!isCreating && (
          <button className="piggy-tab-new" onClick={() => setIsCreating(true)}>
            <I.Plus style={{ width: 13, height: 13 }} /> New
          </button>
        )}
      </div>

      {/* New Piggy form */}
      {isCreating && (
        <div className="piggy-settings-panel" style={{ marginTop: 4, marginBottom: 12 }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--text)' }}>Create new piggy bank</div>
          <div className="filter-row" style={{ marginBottom: 6 }}>
            <input
              className="input"
              type="text"
              placeholder="Name (e.g. Vacation fund)"
              value={newName}
              onChange={e => setNewName(e.target.value)}
              autoFocus
            />
            <input
              className="input mono"
              type="number"
              inputMode="decimal"
              placeholder="Target goal (e.g. 500)"
              value={newTarget}
              onChange={e => setNewTarget(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleCreatePiggy()}
            />
          </div>
          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
            <button className="btn btn-secondary btn-sm" onClick={handleCreatePiggy}>Create</button>
            <button className="btn btn-ghost btn-sm" onClick={() => { setIsCreating(false); setNewName(''); setNewTarget(''); }}>Cancel</button>
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
              <button className="btn btn-sm" onClick={handleSaveRename} title="Save name"><I.Check style={{ width: 13, height: 13 }} /></button>
              <button className="btn btn-ghost btn-sm" onClick={() => setIsRenaming(false)} title="Cancel"><I.Close style={{ width: 13, height: 13 }} /></button>
            </div>
          ) : (
            <>
              <span className="piggy-name">{activePiggy.name || 'Piggy bank'}</span>
              <button className="piggy-rename-btn" onClick={handleStartRename} title="Rename piggy bank">
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
              title="Delete piggy bank"
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
              alt={activePiggy.name || 'Piggy bank'}
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
                <button className="btn btn-sm" onClick={handleSaveGoal} title="Save goal"><I.Check /></button>
                <button className="btn btn-ghost btn-sm" onClick={() => setIsEditingGoal(false)} title="Cancel"><I.Close /></button>
              </div>
            ) : (
              <div className="totals-row" style={{ paddingBottom: 0, borderBottom: 'none', marginBottom: 0 }}>
                <div>
                  <span className="totals-label">{target > 0 ? 'Saved' : 'No goal yet'}</span>
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
                ? (saved >= target ? 'Goal complete! 🎉' : `${MYR(target - saved)} to go`)
                : 'Set a goal and watch it fill up.'}
            </div>

            <div className="piggy-actions" style={{ marginTop: 10 }}>
              <button className="btn btn-secondary btn-sm" onClick={() => setIsAddOpen(o => !o)}>
                <I.Plus /> Add funds
              </button>
              <button className="btn btn-ghost btn-sm" onClick={() => breakPiggy(activeId)} disabled={saved <= 0}>
                Break
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
                <button className="btn btn-sm" onClick={handleDeposit}>Deposit</button>
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
                Customize {activePiggy.name || 'Piggy bank'}
              </span>
              <button className="icon-btn" onClick={() => setShowSettings(false)} style={{ width: 22, height: 22 }}>
                <I.Close style={{ width: 12, height: 12 }} />
              </button>
            </div>

            {/* Picture customization */}
            <div>
              <div className="field-label" style={{ marginBottom: 6 }}>Picture / Texture</div>
              <div className="piggy-settings-grid">
                <button className="btn btn-secondary btn-sm" onClick={() => textureInputRef.current?.click()}>
                  <I.Upload style={{ width: 13, height: 13 }} /> Custom picture
                </button>
                {activePiggy.texture && (
                  <button className="btn btn-ghost btn-sm" onClick={() => updatePiggyTexture(activeId, null)}>
                    <I.Trash style={{ width: 13, height: 13 }} /> Reset picture
                  </button>
                )}
              </div>
            </div>

            {/* Sound customization */}
            <div>
              <div className="field-label" style={{ marginBottom: 6 }}>Deposit Sound</div>
              <div className="drawer-tabs" style={{ marginBottom: 8 }}>
                <button
                  className={`drawer-tab ${(activePiggy.soundId || 'coin') === 'coin' ? 'active' : ''}`}
                  onClick={() => updatePiggySound(activeId, 'coin')}
                >
                  Coin
                </button>
                <button
                  className={`drawer-tab ${(activePiggy.soundId || 'coin') === 'chime' ? 'active' : ''}`}
                  onClick={() => updatePiggySound(activeId, 'chime')}
                >
                  Chime
                </button>
                <button
                  className={`drawer-tab ${(activePiggy.soundId || 'coin') === 'custom' ? 'active' : ''}`}
                  onClick={() => {
                    if (activePiggy.soundCustom) {
                      updatePiggySound(activeId, 'custom');
                    } else {
                      if (showToast) showToast('Upload a custom sound file first.', 'info');
                      soundInputRef.current?.click();
                    }
                  }}
                >
                  Custom
                </button>
                <button
                  className={`drawer-tab ${(activePiggy.soundId || 'coin') === 'none' ? 'active' : ''}`}
                  onClick={() => updatePiggySound(activeId, 'none')}
                >
                  Mute
                </button>
              </div>

              <div className="piggy-settings-grid">
                <button className="btn btn-secondary btn-sm" onClick={() => soundInputRef.current?.click()}>
                  <I.Upload style={{ width: 13, height: 13 }} /> Upload sound
                </button>
                {activePiggy.soundCustom && (
                  <button
                    className="btn btn-ghost btn-sm"
                    onClick={() => updatePiggySound(activeId, activePiggy.soundId === 'custom' ? 'coin' : activePiggy.soundId, null)}
                  >
                    <I.Trash style={{ width: 13, height: 13 }} /> Remove sound
                  </button>
                )}
              </div>

              {activePiggy.soundId !== 'none' && (
                <button
                  className="link-btn"
                  style={{ marginTop: 6 }}
                  onClick={() => playPiggySound(activePiggy.soundId || 'coin', activePiggy.soundCustom)}
                >
                  Preview sound
                </button>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
