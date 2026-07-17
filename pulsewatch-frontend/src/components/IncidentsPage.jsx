import React, { useState, useEffect, useCallback } from 'react';
import { AlertTriangle, Clock, ShieldAlert, ShieldCheck, Search, ChevronLeft, ChevronRight, Info, CheckCircle } from 'lucide-react';
import { api } from '../api/axiosConfig';
import { INCIDENT_EVENT_LABELS, SEVERITY_LABELS, INCIDENT_STATUS_LABELS, labelFor } from '../utils/enumLabels';

const IncidentsPage = () => {
    const [incidents, setIncidents] = useState([]);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [statusFilter, setStatusFilter] = useState('');
    const [severityFilter, setSeverityFilter] = useState('');
    const [selectedIncident, setSelectedIncident] = useState(null);
    const [timeline, setTimeline] = useState([]);
    const [timelineLoading, setTimelineLoading] = useState(false);

    const fetchIncidents = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            const params = new URLSearchParams({
                page,
                size: 10,
            });
            if (statusFilter) params.append('status', statusFilter);
            if (severityFilter) params.append('severity', severityFilter);

            const res = await api.get(`/incidents?${params}`);
            const content = res.data.content || [];
            setIncidents(content);
            setTotalPages(res.data.totalPages || 0);
            setTotalElements(res.data.totalElements || 0);

            // Maintain current selection if possible, otherwise default to first
            if (content.length > 0) {
                if (selectedIncident) {
                    const match = content.find(c => c.id === selectedIncident.id);
                    setSelectedIncident(match || content[0]);
                } else {
                    setSelectedIncident(content[0]);
                }
            } else {
                setSelectedIncident(null);
            }
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to fetch incidents.');
        } finally {
            setLoading(false);
        }
    }, [page, statusFilter, severityFilter]);

    useEffect(() => {
        fetchIncidents();
    }, [fetchIncidents]);

    const [refreshTrigger, setRefreshTrigger] = useState(0);

    // Fetch timeline events when selectedIncident changes
    useEffect(() => {
        if (!selectedIncident) {
            setTimeline([]);
            return;
        }
        let active = true;
        const fetchTimeline = async () => {
            setTimelineLoading(true);
            try {
                const res = await api.get(`/incidents/${selectedIncident.id}/events`);
                if (active) {
                    setTimeline(res.data || []);
                }
            } catch (err) {
                console.error('Failed to load incident timeline', err);
            } finally {
                if (active) setTimelineLoading(false);
            }
        };
        fetchTimeline();
        return () => { active = false; };
    }, [selectedIncident, refreshTrigger]);

    // Real-time update listener
    useEffect(() => {
        const handleRealtimeEvent = (e) => {
            const { type, payload } = e.detail;
            if (type === 'INCIDENT_UPDATED') {
                fetchIncidents();
                if (selectedIncident && payload && payload.id === selectedIncident.id) {
                    setRefreshTrigger(prev => prev + 1);
                }
            }
        };

        window.addEventListener('pulsewatch-realtime', handleRealtimeEvent);
        return () => {
            window.removeEventListener('pulsewatch-realtime', handleRealtimeEvent);
        };
    }, [fetchIncidents, selectedIncident]);

    const formatTime = (timeStr) => {
        if (!timeStr) return '—';
        return new Date(timeStr).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' });
    };

    const formatDuration = (sec) => {
        if (sec === null || sec === undefined) return 'Active';
        if (sec < 60) return `${sec}s`;
        const mins = Math.floor(sec / 60);
        if (mins < 60) return `${mins}m ${sec % 60}s`;
        const hrs = Math.floor(mins / 60);
        return `${hrs}h ${mins % 60}m`;
    };

    const getStatusColor = (status) => ({
        OPEN:         'bg-red-50 text-red-700 border-red-200',
        ACKNOWLEDGED: 'bg-amber-55 text-amber-750 border-amber-200',
        RESOLVED:     'bg-emerald-50 text-emerald-850 border-emerald-250',
    })[status] || 'bg-gray-50 text-gray-700 border-gray-200';

    const getSeverityColor = (severity) => ({
        CRITICAL: 'bg-rose-50 text-rose-800 border-rose-200',
        HIGH:     'bg-orange-50 text-orange-700 border-orange-200',
        MEDIUM:   'bg-amber-50 text-amber-800 border-amber-200',
        LOW:      'bg-gray-100 text-gray-600 border-gray-300',
    })[severity] || 'bg-gray-50 text-gray-700 border-gray-200';

    const handleAcknowledge = async (id) => {
        try {
            const res = await api.patch(`/incidents/${id}/acknowledge`);
            setIncidents(prev => prev.map(inc => inc.id === id ? res.data : inc));
            setSelectedIncident(res.data);
            fetchIncidents();
        } catch (err) {
            console.error('Failed to acknowledge incident', err);
        }
    };

    const handleResolve = async (id) => {
        try {
            const res = await api.patch(`/incidents/${id}/resolve`);
            setIncidents(prev => prev.map(inc => inc.id === id ? res.data : inc));
            setSelectedIncident(res.data);
            fetchIncidents();
        } catch (err) {
            console.error('Failed to resolve incident', err);
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 p-6 md:p-8">
            <div className="max-w-7xl mx-auto space-y-6">
                
                {/* Header */}
                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200 flex items-center gap-3">
                    <ShieldAlert className="w-8 h-8 text-red-600 animate-pulse" />
                    <div>
                        <h1 className="text-2xl font-bold text-gray-900">Incidents Log</h1>
                        <p className="text-gray-555 text-sm mt-0.5">Track, audit and investigate service downtime incidents and resolution timeline history.</p>
                    </div>
                </div>

                {error && (
                  <div className="bg-red-50 border border-red-200 text-red-800 p-4 rounded-lg text-sm">
                      {error}
                  </div>
                )}

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* Left: Incident list and filters */}
                    <div className="lg:col-span-2 space-y-4">
                        
                        {/* Filters */}
                        <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-200 flex flex-wrap gap-3">
                            <div className="flex items-center gap-2">
                                <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">Status:</span>
                                <select
                                    value={statusFilter}
                                    onChange={e => { setStatusFilter(e.target.value); setPage(0); }}
                                    className="border border-gray-205 rounded-lg px-3 py-1.5 text-xs text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                >
                                    <option value="">All Statuses</option>
                                    <option value="OPEN">Open</option>
                                    <option value="ACKNOWLEDGED">Acknowledged</option>
                                    <option value="RESOLVED">Resolved</option>
                                </select>
                            </div>

                            <div className="flex items-center gap-2">
                                <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">Severity:</span>
                                <select
                                    value={severityFilter}
                                    onChange={e => { setSeverityFilter(e.target.value); setPage(0); }}
                                    className="border border-gray-205 rounded-lg px-3 py-1.5 text-xs text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                >
                                    <option value="">All Severities</option>
                                    <option value="CRITICAL">Critical</option>
                                    <option value="HIGH">High</option>
                                    <option value="MEDIUM">Medium</option>
                                    <option value="LOW">Low</option>
                                </select>
                            </div>
                        </div>

                        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                            {loading ? (
                                <div className="flex justify-center py-20">
                                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" />
                                </div>
                            ) : incidents.length === 0 ? (
                                <div className="text-center py-20 text-gray-400">
                                    <ShieldCheck className="w-12 h-12 mx-auto mb-3 text-emerald-500 opacity-80" />
                                    <p className="font-semibold text-gray-900">All systems operational</p>
                                    <p className="text-sm text-gray-500 mt-1">No active incidents found matching the criteria.</p>
                                </div>
                            ) : (
                                <div className="overflow-x-auto">
                                    <table className="min-w-full divide-y divide-gray-200">
                                        <thead className="bg-gray-50">
                                            <tr>
                                                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">Monitor</th>
                                                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">Status</th>
                                                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">Severity</th>
                                                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">Duration</th>
                                                <th className="px-6 py-3 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">Triggered At</th>
                                            </tr>
                                        </thead>
                                        <tbody className="bg-white divide-y divide-gray-200">
                                            {incidents.map(i => (
                                                <tr
                                                    key={i.id}
                                                    onClick={() => setSelectedIncident(i)}
                                                    className={`cursor-pointer hover:bg-gray-50 transition-colors ${
                                                        selectedIncident?.id === i.id ? 'bg-blue-50/50 font-medium' : ''
                                                    }`}
                                                >
                                                    <td className="px-6 py-4 text-sm text-gray-900 font-semibold truncate max-w-xs">{i.monitorName || 'Unknown Monitor'}</td>
                                                    <td className="px-6 py-4">
                                                        <span className={`px-2 py-0.5 border rounded-full text-xs font-semibold ${getStatusColor(i.status)}`}>
                                                            {labelFor(i.status, INCIDENT_STATUS_LABELS)}
                                                        </span>
                                                    </td>
                                                    <td className="px-6 py-4">
                                                        <span className={`px-2 py-0.5 border rounded-full text-xs font-semibold ${getSeverityColor(i.severity)}`}>
                                                            {labelFor(i.severity, SEVERITY_LABELS)}
                                                        </span>
                                                    </td>
                                                    <td className="px-6 py-4 text-sm text-gray-500">{formatDuration(i.durationSeconds)}</td>
                                                    <td className="px-6 py-4 text-sm text-gray-500 font-mono">{formatTime(i.startedAt)}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}

                            {/* Pagination */}
                            {totalPages > 1 && (
                                <div className="flex items-center justify-between px-6 py-4 border-t border-gray-150 bg-gray-50/50">
                                    <span className="text-xs text-gray-500">{totalElements} total incidents</span>
                                    <div className="flex items-center gap-2">
                                        <button
                                            onClick={() => setPage(p => Math.max(0, p - 1))}
                                            disabled={page === 0}
                                            className="p-1 border border-gray-200 rounded hover:bg-gray-100 disabled:opacity-40"
                                        >
                                            <ChevronLeft className="w-4 h-4" />
                                        </button>
                                        <span className="text-xs text-gray-600">Page {page + 1} / {totalPages}</span>
                                        <button
                                            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                                            disabled={page >= totalPages - 1}
                                            className="p-1 border border-gray-200 rounded hover:bg-gray-100 disabled:opacity-40"
                                        >
                                            <ChevronRight className="w-4 h-4" />
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Right: Selected Incident details and timeline events */}
                    <div className="lg:col-span-1">
                        {selectedIncident ? (
                            <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 space-y-6">
                                <div>
                                    <h2 className="text-lg font-bold text-gray-900">Incident Report</h2>
                                    <p className="text-[10px] text-gray-400 font-mono mt-0.5">UUID: {selectedIncident.id}</p>
                                </div>

                                <div className="border-t border-gray-150 pt-4 space-y-3.5 text-sm text-gray-600">
                                    <div className="flex justify-between items-center">
                                        <span className="text-gray-400">Monitor Target</span>
                                        <span className="font-semibold text-gray-800">{selectedIncident.monitorName || 'Unknown Monitor'}</span>
                                    </div>
                                    <div className="flex justify-between items-center">
                                        <span className="text-gray-400">Current Status</span>
                                        <span className={`px-2 py-0.5 border rounded-full text-xs font-semibold ${getStatusColor(selectedIncident.status)}`}>
                                            {labelFor(selectedIncident.status, INCIDENT_STATUS_LABELS)}
                                        </span>
                                    </div>
                                    <div className="flex justify-between items-center">
                                        <span className="text-gray-400">Severity Level</span>
                                        <span className={`px-2 py-0.5 border rounded-full text-xs font-semibold ${getSeverityColor(selectedIncident.severity)}`}>
                                            {labelFor(selectedIncident.severity, SEVERITY_LABELS)}
                                        </span>
                                    </div>
                                    <div className="flex justify-between items-center">
                                        <span className="text-gray-400">Triggered At</span>
                                        <span className="font-semibold text-gray-800 font-mono text-xs">{formatTime(selectedIncident.startedAt)}</span>
                                    </div>
                                    <div className="flex justify-between items-center">
                                        <span className="text-gray-400">Resolved At</span>
                                        <span className="font-semibold text-gray-800 font-mono text-xs">{formatTime(selectedIncident.resolvedAt)}</span>
                                    </div>
                                    <div className="flex justify-between items-center">
                                        <span className="text-gray-400">Downtime Duration</span>
                                        <span className="font-semibold text-gray-800">{formatDuration(selectedIncident.durationSeconds)}</span>
                                    </div>
                                    {selectedIncident.failureReason && (
                                        <div className="bg-red-50 border border-red-200 rounded-lg p-3 space-y-1">
                                            <span className="text-xs font-bold text-red-800 block">Incident Reason</span>
                                            <p className="text-xs text-red-950 font-mono break-all">{selectedIncident.failureReason}</p>
                                        </div>
                                    )}
                                </div>

                                {/* Action buttons */}
                                {(selectedIncident.status === 'OPEN' || selectedIncident.status === 'ACKNOWLEDGED') && (
                                    <div className="flex gap-2 pt-1 border-t border-gray-150">
                                        {selectedIncident.status === 'OPEN' && (
                                            <button
                                                onClick={() => handleAcknowledge(selectedIncident.id)}
                                                className="flex-1 bg-amber-500 hover:bg-amber-600 text-white font-bold py-2 rounded-xl text-xs transition-colors flex items-center justify-center gap-1.5 shadow-sm"
                                            >
                                                Acknowledge
                                            </button>
                                        )}
                                        <button
                                            onClick={() => handleResolve(selectedIncident.id)}
                                            className="flex-1 bg-emerald-600 hover:bg-emerald-700 text-white font-bold py-2 rounded-xl text-xs transition-colors flex items-center justify-center gap-1.5 shadow-sm"
                                        >
                                            Resolve Incident
                                        </button>
                                    </div>
                                )}

                                <div className="border-t border-gray-150 pt-4">
                                    <h3 className="font-bold text-gray-900 mb-4 flex items-center gap-2">
                                        <Clock className="w-4 h-4 text-blue-600" />
                                        Activity Timeline
                                    </h3>
                                    {timelineLoading ? (
                                        <div className="flex justify-center py-6">
                                            <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-600" />
                                        </div>
                                    ) : timeline.length === 0 ? (
                                        <p className="text-xs text-gray-400 text-center py-4">No logged activity timeline events.</p>
                                    ) : (
                                        <div className="relative border-l border-gray-200 pl-4 ml-2 space-y-4">
                                            {timeline.map(event => (
                                                <div key={event.id} className="relative text-xs">
                                                    <span className={`absolute -left-6 top-1 w-3.5 h-3.5 rounded-full border-2 border-white flex items-center justify-center ${
                                                        event.eventType === 'INCIDENT_OPENED' ? 'bg-red-500' :
                                                        event.eventType === 'INCIDENT_RESOLVED' ? 'bg-emerald-500' : 'bg-blue-500'
                                                    }`} />
                                                    <div className="font-semibold text-gray-850">{labelFor(event.eventType, INCIDENT_EVENT_LABELS)}</div>
                                                    <div className="text-gray-500 mt-0.5 leading-relaxed">{event.message}</div>
                                                    <div className="text-[10px] text-gray-400 mt-1">{formatTime(event.createdAt)}</div>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            </div>
                        ) : (
                            <div className="bg-white rounded-xl border border-gray-200 p-8 text-center text-gray-400 h-64 flex flex-col items-center justify-center gap-2">
                                <Info className="w-8 h-8 text-gray-300" />
                                <p>Select an incident to view details and timeline.</p>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default IncidentsPage;
