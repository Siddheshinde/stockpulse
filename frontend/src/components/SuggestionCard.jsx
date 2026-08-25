import React, { useState } from 'react';

const SuggestionCard = ({ suggestion, product, onAction }) => {
  const [processing, setProcessing] = useState(false);

  const handleAction = async (action) => {
    setProcessing(true);
    await onAction(action);
  };

  const isPricing = suggestion.type === 'PRICING';
  
  const getTriggerBadgeType = (trigger) => {
    if (trigger === 'INVENTORY_LOW') return 'badge-danger';
    if (trigger === 'DEMAND_SPIKE') return 'badge-warning';
    return 'badge-neutral';
  };

  return (
    <div className={`suggestion-card type-${suggestion.type.toLowerCase()}`}>
      <div className="card-header">
        <div>
          <div className="card-title">{product?.name || suggestion.productId}</div>
          <div className="card-subtitle">{isPricing ? 'Price Optimization' : 'Inventory Replenishment'}</div>
        </div>
        <span className={`badge ${getTriggerBadgeType(suggestion.triggerReason)}`}>
          {suggestion.triggerReason.replace('_', ' ')}
        </span>
      </div>

      <div className="card-body">
        <div className="card-value-change">
          {isPricing ? (
            <>
              <span style={{ color: 'var(--text-muted)', textDecoration: 'line-through' }}>
                ${parseFloat(suggestion.currentValue).toFixed(2)}
              </span>
              <span style={{ color: 'var(--text-secondary)' }}>→</span>
              <span style={{ color: suggestion.direction === 'INCREASE' ? 'var(--success)' : 'var(--danger)' }}>
                ${parseFloat(suggestion.recommendedValue).toFixed(2)}
              </span>
            </>
          ) : (
            <>
              <span style={{ color: 'var(--text-muted)' }}>{suggestion.currentValue} units</span>
              <span style={{ color: 'var(--text-secondary)' }}>→</span>
              <span style={{ color: 'var(--warning)' }}>+{suggestion.recommendedValue}</span>
            </>
          )}
        </div>
        
        <div className="card-confidence">
          Confidence {Math.round(suggestion.confidence * 100)}%
        </div>

        <div className="card-reasoning">
          {suggestion.reasoning}
        </div>
      </div>

      <div className="card-actions">
        <button 
          className="btn btn-outline" 
          onClick={() => handleAction('ACCEPTED')}
          disabled={processing}
          style={{ flex: 1, borderColor: 'var(--success)', color: 'var(--success)' }}
        >
          {processing ? '...' : 'ACCEPT'}
        </button>
        <button 
          className="btn btn-outline" 
          onClick={() => handleAction('REJECTED')}
          disabled={processing}
          style={{ flex: 1, borderColor: 'var(--danger)', color: 'var(--danger)' }}
        >
          {processing ? '...' : 'REJECT'}
        </button>
      </div>
    </div>
  );
};

export default SuggestionCard;
