import React from 'react';

const ProductCard = ({ product, onSimulateSale }) => {
  const isLowStock = product.stockLevel < product.reorderThreshold;
  
  const maxStock = Math.max(product.reorderThreshold * 2, product.stockLevel, 20);
  const percentFull = Math.min(100, Math.max(0, (product.stockLevel / maxStock) * 100));
  
  // Create ASCII style bar for stock
  const barLength = 12;
  const filledBlocks = Math.round((percentFull / 100) * barLength);
  const emptyBlocks = barLength - filledBlocks;
  const asciiBar = '█'.repeat(filledBlocks) + '░'.repeat(emptyBlocks);

  return (
    <tr>
      <td>
        <div style={{ fontWeight: '500', color: 'var(--text-primary)' }}>{product.name}</div>
        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: '0.125rem' }}>{product.sku}</div>
      </td>
      <td>
        <span className={`badge badge-${product.status === 'ACTIVE' ? 'success' : 'neutral'}`}>
          {product.status}
        </span>
      </td>
      <td style={{ fontWeight: '500' }}>${product.currentPrice.toFixed(2)}</td>
      <td>
        <div className="stock-bar-container" style={{ color: isLowStock ? 'var(--warning)' : 'var(--success)' }}>
          <div className="stock-bar-text">
            {product.stockLevel} / {product.reorderThreshold}
          </div>
          <div className="stock-bar-visual">
            {asciiBar}
          </div>
        </div>
      </td>
      <td style={{ color: product.demandVelocity > 5 ? 'var(--warning)' : 'inherit' }}>
        {product.demandVelocity}/day
      </td>
      <td>
        <button 
          className="btn btn-outline" 
          onClick={onSimulateSale}
          style={{ fontSize: '0.75rem', padding: '0.375rem 0.625rem' }}
        >
          Simulate Sale
        </button>
      </td>
    </tr>
  );
};

export default ProductCard;
