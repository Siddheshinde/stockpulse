import React from 'react';
import ProductCard from './ProductCard';
import SuggestionCard from './SuggestionCard';
import ActivityFeed from './ActivityFeed';

const Dashboard = ({ products, suggestions, activities, onSimulateSale, onAction }) => {
  const lowStockCount = products.filter(p => p.stockLevel < p.reorderThreshold).length;

  return (
    <>
      <div className="kpi-row">
        <div className="kpi-card">
          <div className="kpi-label">Total Products</div>
          <div className="kpi-value">{products.length}</div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Low Stock</div>
          <div className={`kpi-value ${lowStockCount > 0 ? 'warning' : 'success'}`}>
            {lowStockCount}
          </div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Pending Actions</div>
          <div className={`kpi-value ${suggestions.length > 0 ? 'warning' : 'neutral'}`}>
            {suggestions.length}
          </div>
        </div>
        <div className="kpi-card">
          <div className="kpi-label">Active AI Monitoring</div>
          <div className="kpi-value success">ON</div>
        </div>
      </div>

      <div className="dashboard-grid">
        <div className="main-column" style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          <div className="panel">
            <div className="panel-header">
              <h2 className="panel-title">Inventory Operations</h2>
            </div>
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Status</th>
                    <th>Price</th>
                    <th>Stock / Threshold</th>
                    <th>Velocity</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {products.map(p => (
                    <ProductCard 
                      key={p.id} 
                      product={p} 
                      onSimulateSale={() => onSimulateSale(p)} 
                    />
                  ))}
                </tbody>
              </table>
            </div>
          </div>
          
          <div className="panel">
            <div className="panel-header">
              <h2 className="panel-title">Operations Activity</h2>
            </div>
            <ActivityFeed activities={activities} />
          </div>
        </div>

        <div className="side-column">
          <div className="panel" style={{ position: 'sticky', top: '1.5rem' }}>
            <div className="panel-header">
              <h2 className="panel-title" style={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
                Decision Queue
                {suggestions.length > 0 && (
                  <span className="badge badge-warning">
                    {suggestions.length}
                  </span>
                )}
              </h2>
            </div>
            
            <div className="suggestion-list">
              {suggestions.length === 0 ? (
                <div className="empty-state">
                  <div className="empty-state-title">No pending actions</div>
                  <div className="empty-state-text">
                    The agentic engine is monitoring inventory and demand.
                  </div>
                  <div className="monitoring-status">
                    <div className="monitoring-item">
                      <span>✓</span> Event-driven monitoring: ACTIVE
                    </div>
                    <div className="monitoring-item">
                      <span>✓</span> AI strategy: ACTIVE
                    </div>
                  </div>
                </div>
              ) : (
                suggestions.map(s => (
                  <SuggestionCard 
                    key={`${s.type}-${s.id}`} 
                    suggestion={s} 
                    product={products.find(p => p.id === s.productId)}
                    onAction={(action) => onAction(s, action)} 
                  />
                ))
              )}
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default Dashboard;
