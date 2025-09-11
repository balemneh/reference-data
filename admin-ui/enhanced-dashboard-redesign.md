# Enhanced Dashboard Components

## 6.1 Redesigned Metric Cards

### Design Specifications:
```scss
.cbp-metric-card-v2 {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  padding: 24px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  
  // Gradient border effect
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    padding: 1px;
    background: linear-gradient(135deg, 
      rgba(0, 51, 102, 0.3) 0%, 
      rgba(0, 90, 156, 0.2) 50%, 
      rgba(255, 255, 255, 0.1) 100%
    );
    border-radius: inherit;
    mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
    mask-composite: xor;
  }
  
  &:hover {
    transform: translateY(-4px) scale(1.02);
    box-shadow: 
      0 20px 40px rgba(0, 0, 0, 0.1),
      0 8px 16px rgba(0, 51, 102, 0.15);
      
    &::before {
      background: linear-gradient(135deg, 
        rgba(0, 51, 102, 0.5) 0%, 
        rgba(0, 90, 156, 0.3) 100%
      );
    }
  }
  
  // Interactive floating elements
  &::after {
    content: '';
    position: absolute;
    top: -50%;
    right: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle, 
      rgba(0, 90, 156, 0.05) 0%, 
      transparent 70%
    );
    transition: transform 0.5s ease;
    pointer-events: none;
  }
  
  &:hover::after {
    transform: scale(1.2) rotate(45deg);
  }
}

// Animated data visualizations within cards
.cbp-metric-viz {
  position: relative;
  height: 60px;
  margin-top: 16px;
  
  &__sparkline {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 40px;
    
    svg {
      width: 100%;
      height: 100%;
      
      path {
        stroke: var(--cbp-primary);
        stroke-width: 2;
        fill: none;
        filter: drop-shadow(0 2px 4px rgba(0, 51, 102, 0.2));
        animation: drawPath 1.5s ease-in-out;
      }
      
      .fill-area {
        fill: url(#sparklineGradient);
        opacity: 0.3;
        animation: fadeInUp 1s ease-in-out 0.5s both;
      }
    }
    
    // Gradient definition for sparkline
    defs linearGradient#sparklineGradient {
      stop[offset="0%"] { stop-color: var(--cbp-primary); stop-opacity: 0.3; }
      stop[offset="100%"] { stop-color: var(--cbp-primary); stop-opacity: 0; }
    }
  }
  
  &__trend-indicator {
    position: absolute;
    top: 0;
    right: 0;
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 4px 8px;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 12px;
    font-size: 12px;
    font-weight: 600;
    backdrop-filter: blur(8px);
    
    &--positive {
      color: var(--cbp-success);
      
      .trend-icon {
        animation: bounceUp 2s infinite;
      }
    }
    
    &--negative {
      color: var(--cbp-error);
      
      .trend-icon {
        animation: bounceDown 2s infinite;
      }
    }
  }
}

@keyframes drawPath {
  0% {
    stroke-dasharray: 1000;
    stroke-dashoffset: 1000;
  }
  100% {
    stroke-dasharray: 1000;
    stroke-dashoffset: 0;
  }
}

@keyframes bounceUp {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-2px); }
}

@keyframes bounceDown {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(2px); }
}
```

## 6.2 Interactive Data Table Enhancement

### Enhanced Table Component:
```typescript
interface EnhancedTableColumn<T> {
  key: keyof T;
  title: string;
  sortable?: boolean;
  filterable?: boolean;
  render?: (value: any, record: T) => ReactNode;
  width?: string | number;
  align?: 'left' | 'center' | 'right';
  sticky?: boolean;
  ellipsis?: boolean;
}

interface TableFeatures {
  virtualScrolling: boolean;
  infiniteLoad: boolean;
  bulkActions: string[];
  columnResizing: boolean;
  columnReordering: boolean;
  advancedFilters: boolean;
  globalSearch: boolean;
  exportOptions: string[];
}

const enhancedTableConfig: TableFeatures = {
  virtualScrolling: true,     // Handle 10,000+ records
  infiniteLoad: true,         // Progressive loading
  bulkActions: ['edit', 'delete', 'export'],
  columnResizing: true,       // Drag to resize
  columnReordering: true,     // Drag to reorder
  advancedFilters: true,      // Multi-criteria filtering
  globalSearch: true,         // Fuzzy search across all fields
  exportOptions: ['csv', 'excel', 'json']
};
```

### Advanced Filtering System:
```scss
.cbp-table-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  margin-bottom: 16px;
  
  &__search {
    flex: 1;
    min-width: 240px;
    position: relative;
    
    input {
      width: 100%;
      padding: 10px 40px 10px 12px;
      border: 1px solid var(--cbp-neutral-300);
      border-radius: 8px;
      background: rgba(255, 255, 255, 0.9);
      transition: all 0.2s ease;
      
      &:focus {
        border-color: var(--cbp-primary);
        box-shadow: 0 0 0 3px rgba(0, 51, 102, 0.1);
        background: white;
      }
    }
    
    .search-icon {
      position: absolute;
      right: 12px;
      top: 50%;
      transform: translateY(-50%);
      color: var(--cbp-neutral-500);
    }
  }
  
  &__filter-chip {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 12px;
    background: var(--cbp-primary);
    color: white;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 500;
    
    .remove-btn {
      background: none;
      border: none;
      color: white;
      padding: 2px;
      border-radius: 50%;
      cursor: pointer;
      
      &:hover {
        background: rgba(255, 255, 255, 0.2);
      }
    }
  }
  
  &__advanced-toggle {
    background: none;
    border: 1px solid var(--cbp-neutral-300);
    padding: 8px 16px;
    border-radius: 8px;
    color: var(--cbp-neutral-700);
    cursor: pointer;
    transition: all 0.2s ease;
    
    &:hover {
      border-color: var(--cbp-primary);
      color: var(--cbp-primary);
    }
    
    &--active {
      background: var(--cbp-primary);
      border-color: var(--cbp-primary);
      color: white;
    }
  }
}
```

## 6.3 Real-time Activity Feed

### Live Activity Component:
```typescript
interface ActivityFeedItem {
  id: string;
  type: 'CREATE' | 'UPDATE' | 'DELETE' | 'APPROVE' | 'REJECT';
  entity: string;
  description: string;
  user: {
    name: string;
    avatar?: string;
    role: string;
  };
  timestamp: Date;
  metadata?: Record<string, any>;
  severity: 'low' | 'medium' | 'high' | 'critical';
}

const ActivityFeedComponent = () => {
  const [activities, setActivities] = useState<ActivityFeedItem[]>([]);
  const [isLive, setIsLive] = useState(true);
  
  // Real-time updates via WebSocket
  useEffect(() => {
    const eventSource = new EventSource('/api/activity-stream');
    
    eventSource.onmessage = (event) => {
      const newActivity: ActivityFeedItem = JSON.parse(event.data);
      setActivities(prev => [newActivity, ...prev.slice(0, 49)]); // Keep latest 50
    };
    
    return () => eventSource.close();
  }, []);
  
  return (
    <div className="cbp-activity-feed">
      <div className="cbp-activity-feed__header">
        <h3>Live Activity</h3>
        <button 
          className={`live-indicator ${isLive ? 'active' : ''}`}
          onClick={() => setIsLive(!isLive)}
        >
          <span className="pulse-dot"></span>
          {isLive ? 'Live' : 'Paused'}
        </button>
      </div>
      
      <div className="cbp-activity-feed__timeline">
        {activities.map(activity => (
          <ActivityItem 
            key={activity.id} 
            activity={activity}
            animate={isLive}
          />
        ))}
      </div>
    </div>
  );
};
```

### Activity Feed Styles:
```scss
.cbp-activity-feed {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  padding: 24px;
  height: 400px;
  display: flex;
  flex-direction: column;
  
  &__header {
    display: flex;
    justify-content: between;
    align-items: center;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--cbp-neutral-200);
    
    h3 {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: var(--cbp-neutral-900);
    }
    
    .live-indicator {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      border: 1px solid var(--cbp-neutral-300);
      border-radius: 20px;
      background: white;
      color: var(--cbp-neutral-700);
      font-size: 12px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;
      
      &.active {
        border-color: var(--cbp-success);
        color: var(--cbp-success);
        
        .pulse-dot {
          background: var(--cbp-success);
          animation: pulse 2s infinite;
        }
      }
      
      .pulse-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: var(--cbp-neutral-400);
      }
    }
  }
  
  &__timeline {
    flex: 1;
    overflow-y: auto;
    padding-right: 8px;
    
    // Custom scrollbar
    &::-webkit-scrollbar {
      width: 4px;
    }
    
    &::-webkit-scrollbar-track {
      background: transparent;
    }
    
    &::-webkit-scrollbar-thumb {
      background: var(--cbp-neutral-300);
      border-radius: 2px;
      
      &:hover {
        background: var(--cbp-neutral-400);
      }
    }
  }
}

.cbp-activity-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--cbp-neutral-100);
  animation: slideInRight 0.3s ease-out;
  
  &:last-child {
    border-bottom: none;
  }
  
  &__indicator {
    flex-shrink: 0;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    
    &--create {
      background: linear-gradient(135deg, var(--cbp-success) 0%, #22c55e 100%);
      color: white;
    }
    
    &--update {
      background: linear-gradient(135deg, var(--cbp-info) 0%, #0ea5e9 100%);
      color: white;
    }
    
    &--delete {
      background: linear-gradient(135deg, var(--cbp-error) 0%, #f87171 100%);
      color: white;
    }
    
    .activity-icon {
      width: 16px;
      height: 16px;
    }
    
    // Ripple effect for new items
    &::after {
      content: '';
      position: absolute;
      top: -4px;
      left: -4px;
      right: -4px;
      bottom: -4px;
      border: 2px solid currentColor;
      border-radius: 50%;
      opacity: 0;
      animation: ripple 0.6s ease-out;
    }
  }
  
  &__content {
    flex: 1;
    min-width: 0;
  }
  
  &__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 4px;
    gap: 8px;
  }
  
  &__user {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: var(--cbp-neutral-600);
    
    .user-avatar {
      width: 20px;
      height: 20px;
      border-radius: 50%;
      background: var(--cbp-neutral-200);
    }
    
    .user-name {
      font-weight: 500;
    }
    
    .user-role {
      opacity: 0.8;
    }
  }
  
  &__time {
    font-size: 11px;
    color: var(--cbp-neutral-500);
    white-space: nowrap;
  }
  
  &__description {
    font-size: 14px;
    color: var(--cbp-neutral-800);
    line-height: 1.4;
  }
  
  &__metadata {
    margin-top: 6px;
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    
    .metadata-tag {
      padding: 2px 6px;
      background: var(--cbp-neutral-100);
      border-radius: 10px;
      font-size: 10px;
      color: var(--cbp-neutral-600);
    }
  }
}

@keyframes slideInRight {
  from {
    transform: translateX(20px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes ripple {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  100% {
    transform: scale(1.4);
    opacity: 0;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
```

## 6.4 Advanced Data Visualization Charts

### Chart Configuration:
```typescript
interface ChartConfig {
  type: 'line' | 'bar' | 'pie' | 'area' | 'scatter' | 'heatmap';
  title: string;
  data: ChartDataPoint[];
  options: {
    responsive: boolean;
    interactive: boolean;
    animations: boolean;
    theme: 'light' | 'dark' | 'auto';
    colorScheme: string[];
  };
}

const dashboardCharts: ChartConfig[] = [
  {
    type: 'area',
    title: 'Reference Data Growth',
    data: monthlyGrowthData,
    options: {
      responsive: true,
      interactive: true,
      animations: true,
      theme: 'light',
      colorScheme: ['#003366', '#005a9c', '#2980b9']
    }
  },
  {
    type: 'heatmap',
    title: 'System Activity Heatmap',
    data: activityHeatmapData,
    options: {
      responsive: true,
      interactive: true,
      animations: true,
      theme: 'light',
      colorScheme: ['#e3f2fd', '#003366']
    }
  }
];
```

### Chart Component Styles:
```scss
.cbp-chart-container {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  padding: 24px;
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, 
      transparent 0%, 
      var(--cbp-primary) 50%, 
      transparent 100%
    );
  }
  
  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h3 {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: var(--cbp-neutral-900);
    }
    
    .chart-controls {
      display: flex;
      gap: 8px;
      
      button {
        padding: 6px 12px;
        border: 1px solid var(--cbp-neutral-300);
        border-radius: 6px;
        background: white;
        color: var(--cbp-neutral-700);
        font-size: 12px;
        cursor: pointer;
        transition: all 0.2s ease;
        
        &:hover {
          border-color: var(--cbp-primary);
          color: var(--cbp-primary);
        }
        
        &.active {
          background: var(--cbp-primary);
          border-color: var(--cbp-primary);
          color: white;
        }
      }
    }
  }
  
  &__chart {
    height: 300px;
    position: relative;
    
    canvas {
      border-radius: 8px;
    }
    
    // Loading state
    &--loading {
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--cbp-neutral-50);
      border-radius: 8px;
      
      .loading-spinner {
        width: 40px;
        height: 40px;
        border: 3px solid var(--cbp-neutral-200);
        border-top-color: var(--cbp-primary);
        border-radius: 50%;
        animation: spin 1s linear infinite;
      }
    }
  }
  
  &__legend {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid var(--cbp-neutral-200);
    
    .legend-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      color: var(--cbp-neutral-700);
      
      .legend-color {
        width: 12px;
        height: 12px;
        border-radius: 2px;
      }
    }
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
```