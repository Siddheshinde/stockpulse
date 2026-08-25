import React from 'react';

const ActivityFeed = ({ activities }) => {
  return (
    <div className="activity-feed">
      {activities.length === 0 ? (
        <div style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
          No recent activity in this session.
        </div>
      ) : (
        activities.map((act) => (
          <div key={act.id} className={`activity-item type-${act.type}`}>
            <div className="activity-time">{act.time}</div>
            <div className="activity-title">{act.title}</div>
            {act.detail && <div className="activity-detail">{act.detail}</div>}
          </div>
        ))
      )}
    </div>
  );
};

export default ActivityFeed;
