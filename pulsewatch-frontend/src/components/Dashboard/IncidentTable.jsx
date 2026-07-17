import React, { useState, useEffect } from 'react';
import { api } from '../../api/axiosConfig';

const IncidentTable = ({ monitorId }) => {
    const [incidents, setIncidents] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!monitorId) return;

        const fetchIncidents = async () => {
            setLoading(true);
            try {
                const response = await api.get(`/incidents/monitor/${monitorId}`);
                setIncidents(response.data);
            } catch (err) {
                console.error("Failed to fetch incidents", err);
            } finally {
                setLoading(false);
            }
        };

        fetchIncidents();
        const interval = setInterval(fetchIncidents, 60000);
        return () => clearInterval(interval);
    }, [monitorId]);

    const formatTime = (timeStr) => {
        if (!timeStr) return '—';
        const date = new Date(timeStr);
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) + 
               ' (' + date.toLocaleDateString() + ')';
    };

    const formatDuration = (seconds) => {
        if (!seconds && seconds !== 0) return '—';
        if (seconds < 60) return `${seconds} sec`;
        const mins = Math.floor(seconds / 60);
        return `${mins} min`;
    };

    const getSeverityBadge = (severity) => {
        const colors = {
            'LOW': 'bg-gray-100 text-gray-800',
            'MEDIUM': 'bg-yellow-100 text-yellow-800',
            'HIGH': 'bg-orange-100 text-orange-800',
            'CRITICAL': 'bg-red-100 text-red-800'
        };
        const color = colors[severity] || colors['LOW'];
        return <span className={`px-2.5 py-0.5 rounded-full text-xs font-medium ${color}`}>{severity}</span>;
    };

    const getStatusBadge = (status) => {
        return (
            <span className={`px-2.5 py-0.5 rounded-full text-xs font-medium ${status === 'OPEN' ? 'bg-red-100 text-red-800 animate-pulse' : 'bg-green-100 text-green-800'}`}>
                {status}
            </span>
        );
    };

    if (loading && incidents.length === 0) {
        return <div className="text-gray-500 py-4 text-center">Loading incidents...</div>;
    }

    if (incidents.length === 0) {
        return (
            <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 text-center">
                <p className="text-gray-500">No incidents recorded for this monitor.</p>
            </div>
        );
    }

    return (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-100 bg-gray-50">
                <h3 className="text-lg font-semibold text-gray-800">Incident History</h3>
            </div>
            <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Severity</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Started</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Resolved</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Duration</th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {incidents.map(incident => (
                            <tr key={incident.id} className="hover:bg-gray-50">
                                <td className="px-6 py-4 whitespace-nowrap">{getStatusBadge(incident.status)}</td>
                                <td className="px-6 py-4 whitespace-nowrap">{getSeverityBadge(incident.severity)}</td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{formatTime(incident.startedAt)}</td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{formatTime(incident.resolvedAt)}</td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 font-mono">{formatDuration(incident.durationSeconds)}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default IncidentTable;
