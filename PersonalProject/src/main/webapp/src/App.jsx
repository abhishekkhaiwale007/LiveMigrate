import React from 'react';
import MigrationDashboard from './components/MigrationDashboard/MigrationDashboard';

function App() {
    return (
        <div className="min-h-screen bg-gray-50">
            <div className="container mx-auto py-8">
                <h1 className="text-3xl font-bold text-center mb-8">LiveMigrate Dashboard</h1>
                <MigrationDashboard />
            </div>
        </div>
    );
}

export default App;