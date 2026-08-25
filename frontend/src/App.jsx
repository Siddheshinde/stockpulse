import React, { useState, useEffect, useRef } from 'react';
import Dashboard from './components/Dashboard';
import './App.css';

function App() {
  const [products, setProducts] = useState([]);
  const [suggestions, setSuggestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Frontend-only session activity tracking
  const [activities, setActivities] = useState([]);
  const knownSuggestionIds = useRef(new Set());

  const addActivity = (type, title, detail) => {
    const now = new Date();
    const timeString = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;
    
    setActivities(prev => [{
      id: Date.now() + Math.random(),
      time: timeString,
      type,
      title,
      detail
    }, ...prev].slice(0, 20)); // Keep last 20
  };

  const fetchData = async () => {
    try {
      const [productsRes, suggestionsRes] = await Promise.all([
        fetch('/api/products'),
        fetch('/api/suggestions?status=PENDING')
      ]);

      if (!productsRes.ok || !suggestionsRes.ok) {
        throw new Error('Failed to fetch data');
      }

      const productsData = await productsRes.json();
      const suggestionsData = await suggestionsRes.json();

      setProducts(productsData);
      
      // Check for new suggestions to log
      suggestionsData.forEach(s => {
        const uniqueId = `${s.type}-${s.id}`;
        if (!knownSuggestionIds.current.has(uniqueId)) {
          knownSuggestionIds.current.add(uniqueId);
          addActivity(
            'suggestion', 
            `AI Recommendation Generated`, 
            `${s.type} optimization for ${s.productId || 'product'}`
          );
        }
      });
      
      setSuggestions(suggestionsData);
      setError(null);
    } catch (err) {
      console.error(err);
      setError('Could not connect to StockPulse backend. Ensure it is running.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 3000);
    return () => clearInterval(interval);
  }, []);

  const handleSimulateSale = async (product) => {
    try {
      const oldStock = product.stockLevel;
      addActivity('order', 'Order Simulated', `${product.name} (Stock: ${oldStock} → ${oldStock - 1})`);
      
      const res = await fetch(`/api/products/${product.id}/orders`, {
        method: 'POST',
      });
      if (res.ok) {
        fetchData(); // Refresh immediately
      }
    } catch (err) {
      console.error('Simulation failed', err);
    }
  };

  const handleAcceptReject = async (suggestion, action) => {
    try {
      const endpoint = suggestion.type === 'PRICING' 
        ? `/api/pricing-suggestions/${suggestion.id}`
        : `/api/reorder-suggestions/${suggestion.id}`;

      addActivity(
        action === 'ACCEPTED' ? 'accept' : 'reject',
        `Recommendation ${action === 'ACCEPTED' ? 'Accepted' : 'Rejected'}`,
        `${suggestion.type} for ${suggestion.productId}`
      );

      const res = await fetch(endpoint, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: action })
      });

      if (res.ok) {
        fetchData();
      } else {
        alert('Action failed. Suggestion may have already been processed.');
      }
    } catch (err) {
      console.error('Failed to update suggestion', err);
    }
  };

  if (loading && products.length === 0) {
    return (
      <div className="dashboard-container">
        <div className="top-header">
          <div className="brand">
            <h1>StockPulse</h1>
            <div className="brand-subtitle">AI Inventory & Pricing Operations</div>
          </div>
        </div>
        <div className="empty-state">Loading dashboard data...</div>
      </div>
    );
  }

  return (
    <div className="dashboard-container">
      <div className="top-header">
        <div className="brand">
          <h1>StockPulse</h1>
          <div className="brand-subtitle">AI Inventory & Pricing Operations</div>
        </div>
        <div className="header-status">
          <div className="engine-status">AI Engine: Active</div>
          <div>Event-driven monitoring enabled</div>
        </div>
      </div>
      
      {error && (
        <div className="panel" style={{ backgroundColor: 'var(--danger-bg)', borderColor: 'var(--danger)', padding: '1rem', color: 'var(--danger)' }}>
          {error}
        </div>
      )}

      <Dashboard 
        products={products} 
        suggestions={suggestions} 
        activities={activities}
        onSimulateSale={handleSimulateSale}
        onAction={handleAcceptReject}
      />
    </div>
  );
}

export default App;
