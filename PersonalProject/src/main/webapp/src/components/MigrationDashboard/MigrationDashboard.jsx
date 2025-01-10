import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Play, Pause, RotateCcw, AlertCircle, CheckCircle, Clock } from 'lucide-react';
import { Alert, AlertDescription, AlertTitle } from '../../components/ui/alert';

const MigrationDashboard = () => {
    // State management for migration status and metrics
    const [status, setStatus] = useState({
        state: 'INITIALIZED',
        progress: 0
    });
    const [metrics, setMetrics] = useState({
        totalRecords: 0,
        processedRecords: 0,
        migrationSpeed: 0,
        estimatedTimeRemaining: 0
    });
    const [error, setError] = useState(null);

    // Fetch migration status periodically
    useEffect(() => {
        const fetchStatus = async () => {
            try {
                const response = await fetch('/livemigrate/api/v1/migration/status');
                const data = await response.json();
                setStatus(data);
                calculateMetrics(data);
            } catch (err) {
                setError('Failed to fetch migration status');
            }
        };

        // Update every 2 seconds
        const interval = setInterval(fetchStatus, 2000);
        return () => clearInterval(interval);
    }, []);

    // Calculate derived metrics
    const calculateMetrics = (statusData) => {
        // In a real implementation, these would come from the backend
        const processed = Math.floor(statusData.progress * 100);
        const total = 1000;
        const speed = 50; // records per second
        const remaining = Math.ceil((total - processed) / speed);

        setMetrics({
            totalRecords: total,
            processedRecords: processed,
            migrationSpeed: speed,
            estimatedTimeRemaining: remaining
        });
    };

    // Helper function to format time
    const formatTime = (seconds) => {
        if (seconds < 60) return `${seconds}s`;
        const minutes = Math.floor(seconds / 60);
        const remainingSeconds = seconds % 60;
        return `${minutes}m ${remainingSeconds}s`;
    };

    // Handle migration control actions
    const handleStart = async () => {
        try {
            await fetch('/livemigrate/api/v1/migration/start', { method: 'POST' });
        } catch (err) {
            setError('Failed to start migration');
        }
    };

    const handlePause = async () => {
        try {
            await fetch('/livemigrate/api/v1/migration/pause', { method: 'POST' });
        } catch (err) {
            setError('Failed to pause migration');
        }
    };

    const handleResume = async () => {
        try {
            await fetch('/livemigrate/api/v1/migration/resume', { method: 'POST' });
        } catch (err) {
            setError('Failed to resume migration');
        }
    };

    // Get status indicator color
    const getStatusColor = () => {
        switch (status.state) {
            case 'COMPLETED': return 'text-green-500';
            case 'ERROR': return 'text-red-500';
            case 'PAUSED': return 'text-yellow-500';
            case 'MIGRATING': return 'text-blue-500';
            default: return 'text-gray-500';
        }
    };

    return (
        <div className="w-full max-w-4xl mx-auto p-4 space-y-4">
            {/* Status Card */}
            <Card>
                <CardHeader>
                    <CardTitle className="flex items-center justify-between">
                        <span>Migration Status</span>
                        <span className={`flex items-center ${getStatusColor()}`}>
              {status.state === 'COMPLETED' && <CheckCircle className="mr-2" />}
                            {status.state === 'ERROR' && <AlertCircle className="mr-2" />}
                            {status.state === 'PAUSED' && <Pause className="mr-2" />}
                            {status.state === 'MIGRATING' && <RotateCcw className="mr-2 animate-spin" />}
                            {status.state}
            </span>
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    {/* Progress Bar */}
                    <div className="w-full bg-gray-200 rounded-full h-4 mb-4">
                        <div
                            className="bg-blue-500 h-4 rounded-full transition-all duration-500"
                            style={{ width: `${status.progress}%` }}
                        />
                    </div>

                    {/* Metrics Grid */}
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                        <div className="text-center">
                            <div className="text-sm text-gray-600">Processed</div>
                            <div className="text-xl font-bold">
                                {metrics.processedRecords}/{metrics.totalRecords}
                            </div>
                        </div>
                        <div className="text-center">
                            <div className="text-sm text-gray-600">Progress</div>
                            <div className="text-xl font-bold">
                                {(status.progress * 100).toFixed(1)}%
                            </div>
                        </div>
                        <div className="text-center">
                            <div className="text-sm text-gray-600">Speed</div>
                            <div className="text-xl font-bold">
                                {metrics.migrationSpeed}/s
                            </div>
                        </div>
                        <div className="text-center">
                            <div className="text-sm text-gray-600">Remaining</div>
                            <div className="text-xl font-bold">
                                {formatTime(metrics.estimatedTimeRemaining)}
                            </div>
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Control Buttons */}
            <div className="flex justify-center space-x-4">
                {status.state === 'INITIALIZED' && (
                    <button
                        onClick={handleStart}
                        className="flex items-center px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600"
                    >
                        <Play className="mr-2 h-4 w-4" />
                        Start Migration
                    </button>
                )}
                {status.state === 'MIGRATING' && (
                    <button
                        onClick={handlePause}
                        className="flex items-center px-4 py-2 bg-yellow-500 text-white rounded hover:bg-yellow-600"
                    >
                        <Pause className="mr-2 h-4 w-4" />
                        Pause Migration
                    </button>
                )}
                {status.state === 'PAUSED' && (
                    <button
                        onClick={handleResume}
                        className="flex items-center px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
                    >
                        <Play className="mr-2 h-4 w-4" />
                        Resume Migration
                    </button>
                )}
            </div>

            {/* Error Alert */}
            {error && (
                <Alert variant="destructive">
                    <AlertCircle className="h-4 w-4" />
                    <AlertTitle>Error</AlertTitle>
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}
        </div>
    );
};

export default MigrationDashboard;