import React from 'react'

interface HeaderProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
}

export const Header = ({ activeTab, setActiveTab }: HeaderProps) => {
    const tabs = ['Sync', 'Your Tracks', 'Analytics'];
    
    return (
    <header style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      padding: '0 20px',
      height: '50px',
      borderBottom: '1px solid #eee',
      backgroundColor: '#fff',
      width: '100%',
      boxSizing: 'border-box'
    }}>
      <div style={{ fontWeight: 'bold', fontSize: '18px' }}>The Project</div>
      <nav style={{ display: 'flex', gap: '20px' }}>
        {tabs.map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            style={{
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              fontSize: '14px',
              padding: '5px 0',
              color: activeTab === tab ? '#000' : '#888',
              borderBottom: activeTab === tab ? '2px solid #000' : '2px solid transparent',
              fontWeight: activeTab === tab ? '600' : '400'
            }}
          >
            {tab}
          </button>
        ))}
      </nav>
    </header>
  );
}
