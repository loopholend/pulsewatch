import React, { useState, useEffect, useCallback } from 'react';
import {
    Activity, Clock, Zap, AlertTriangle, Globe,
    ArrowUp, ArrowDown, Wrench, Search, ChevronLeft, ChevronRight, Plus,
    ShieldAlert, ShieldCheck, Mail, Send, CheckCircle2, AlertCircle, RefreshCw
} from 'lucide-react';
import MetricCard from './Dashboard/MetricCard';
import LatencyChart from './Dashboard/LatencyChart';
import UptimeChart from './Dashboard/UptimeChart';
import IncidentTable from './Dashboard/IncidentTable';
import { Link } from 'react-router-dom';
import { api } from '../api/axiosConfig';
import { ALERT_TYPE_LABELS, labelFor } from '../utils/enumLabels';

// ── Status badge helper ────────────────────────────────────────────────────────
const StatusBadge = ({ status }) => {
    const map = {
        UP:          'bg-emerald-100 text-emerald-800 border-emerald-200',
        DOWN:        'bg-red-100 text-red-800 border-red-200',
        MAINTENANCE: 'bg-amber-100 text-amber-800 border-amber-200',
        UNKNOWN:     'bg-gray-100 text-gray-650 border-gray-200',
    };
    return (
        <span className={`px-2 py-0.5 border rounded-full text-xs font-semibold ${map[status] || map.UNKNOWN}`}>
            {status}
        </span>
    );
};

// ── Health Distribution Progress Bar ───────────────────────────────────────────
const HealthDistribution = ({ summary }) => {
    if (!summary || summary.totalMonitors === 0) return null;
    
    const upPct = (summary.upCount / summary.totalMonitors) * 100;
    const downPct = (summary.downCount / summary.totalMonitors) * 100;
    const maintPct = (summary.maintenanceCount / summary.totalMonitors) * 100;
    const unkPct = (summary.unknownCount / summary.totalMonitors) * 100;

    return (
        <div className="bg-white p-5 rounded-xl border border-gray-200 shadow-sm space-y-3">
            <div className="flex justify-between items-center text-xs font-bold text-gray-400 uppercase tracking-wider">
                <span>System Health Distribution</span>
                <span>{summary.upCount}/{summary.totalMonitors} Healthy</span>
            </div>
            
            {/* Split Progress Bar */}
            <div className="h-3 w-full rounded-full bg-gray-100 overflow-hidden flex">
                {summary.upCount > 0 && (
                    <div style={{ width: `${upPct}%` }} className="bg-emerald-500 h-full" title={`UP: ${summary.upCount}`} />
                )}
                {summary.downCount > 0 && (
                    <div style={{ width: `${downPct}%` }} className="bg-red-500 h-full animate-pulse" title={`DOWN: ${summary.downCount}`} />
                )}
                {summary.maintenanceCount > 0 && (
                    <div style={{ width: `${maintPct}%` }} className="bg-amber-500 h-full" title={`MAINTENANCE: ${summary.maintenanceCount}`} />
                )}
                {summary.unknownCount > 0 && (
                    <div style={{ width: `${unkPct}%` }} className="bg-gray-400 h-full" title={`UNKNOWN: ${summary.unknownCount}`} />
                )}
            </div>

            <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs font-semibold mt-1">
                <span className="flex items-center gap-1.5 text-gray-700">
                    <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
                    UP ({summary.upCount})
                </span>
                <span className="flex items-center gap-1.5 text-gray-700">
                    <span className="w-2 h-2 rounded-full bg-red-500"></span>
                    DOWN ({summary.downCount})
                </span>
                <span className="flex items-center gap-1.5 text-gray-700">
                    <span className="w-2 h-2 rounded-full bg-amber-500"></span>
                    MAINTENANCE ({summary.maintenanceCount})
                </span>
                <span className="flex items-center gap-1.5 text-gray-700">
                    <span className="w-2 h-2 rounded-full bg-gray-400"></span>
                    UNKNOWN ({summary.unknownCount})
                </span>
            </div>
        </div>
    );
};

const Dashboard = () => {
    const [page, setPage]                   = useState(0);
    const [search, setSearch]               = useState('');
    const [statusFilter, setStatusFilter]   = useState('');
    const [monitorPage, setMonitorPage]     = useState(null); 
    const [selectedMonitorId, setSelectedMonitorId] = useState(null);
    
    // Analytics & Telemetry Charts
    const [stats, setStats]                 = useState(null);
    const [latencyData, setLatencyData]     = useState([]);
    const [uptimeData, setUptimeData]       = useState([]);
    const [refreshTrigger, setRefreshTrigger] = useState(0);
    
    // Dashboard Core Data
    const [summary, setSummary]             = useState(null);
    const [recentIncidents, setRecentIncidents] = useState([]);
    const [latestAlerts, setLatestAlerts]   = useState([]);
    // Global list of all DOWN monitors (not paginated — for "Requires Attention" panel)
    const [downMonitors, setDownMonitors]   = useState([]);
    
    const [loading, setLoading]             = useState(true);
    const [summaryLoading, setSummaryLoading] = useState(true);
    const [searchInput, setSearchInput]     = useState('');

    // Fetch dashboard core summary, logs, and all DOWN monitors globally
    const fetchCoreDashboard = useCallback(async () => {
        setSummaryLoading(true);
        try {
            const [summaryRes, incidentsRes, alertsRes, downRes] = await Promise.all([
                api.get('/analytics/summary'),
                api.get('/incidents?page=0&size=5'),
                api.get('/alerts/history?page=0&size=5'),
                api.get('/monitors?status=DOWN&size=100'),
            ]);
            setSummary(summaryRes.data);
            setRecentIncidents(incidentsRes.data?.content || []);
            setLatestAlerts(alertsRes.data?.content || []);
            setDownMonitors(downRes.data?.content || []);
        } catch (err) {
            console.error('Failed to load core dashboard summary', err);
        } finally {
            setSummaryLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchCoreDashboard();
    }, [fetchCoreDashboard]);

    // Fetch paginated monitors for selecting telemetry
    const fetchMonitors = useCallback(async () => {
        setLoading(true);
        try {
            const params = new URLSearchParams({ page, size: 8, sortBy: 'createdAt', sortDir: 'desc' });
            if (search)       params.append('search', search);
            if (statusFilter) params.append('status', statusFilter);
            const response = await api.get(`/monitors?${params}`);
            setMonitorPage(response.data);
            const content = response.data.content || [];
            if (content.length > 0 && !selectedMonitorId) {
                setSelectedMonitorId(content[0].id);
            } else if (content.length === 0) {
                setSelectedMonitorId(null);
            }
        } catch (err) {
            console.error('Failed to fetch monitors', err);
        } finally {
            setLoading(false);
        }
    }, [page, search, statusFilter]);

    useEffect(() => { fetchMonitors(); }, [fetchMonitors]);

    // Fetch per-monitor telemetry charts
    useEffect(() => {
        if (!selectedMonitorId) return;
        let cancelled = false;
        const fetch = async () => {
            try {
                const [statsRes, latencyRes, uptimeRes] = await Promise.all([
                    api.get(`/analytics/${selectedMonitorId}`),
                    api.get(`/analytics/${selectedMonitorId}/latency`),
                    api.get(`/analytics/${selectedMonitorId}/uptime`),
                ]);
                if (!cancelled) {
                    setStats(statsRes.data);
                    setLatencyData(latencyRes.data);
                    setUptimeData(uptimeRes.data);
                }
            } catch (err) { console.error('Analytics error', err); }
        };
        fetch();
        return () => { cancelled = true; };
    }, [selectedMonitorId, refreshTrigger]);

    // Real-time update listener
    useEffect(() => {
        const handleRealtimeEvent = (e) => {
            const { type, payload } = e.detail;
            
            if (type === 'MONITOR_UPDATED' || type === 'INCIDENT_UPDATED') {
                fetchCoreDashboard();
                fetchMonitors();
                
                const isSelectedMonitor = payload && (
                    payload.id === selectedMonitorId || 
                    payload.monitorId === selectedMonitorId
                );
                if (isSelectedMonitor) {
                    setRefreshTrigger(prev => prev + 1);
                }
            }
        };

        window.addEventListener('pulsewatch-realtime', handleRealtimeEvent);
        return () => {
            window.removeEventListener('pulsewatch-realtime', handleRealtimeEvent);
        };
    }, [fetchCoreDashboard, fetchMonitors, selectedMonitorId]);

    const monitors = monitorPage?.content || [];
    const totalPages = monitorPage?.totalPages || 0;
    const totalElements = monitorPage?.totalElements || 0;

    const handleSearch = (e) => {
        e.preventDefault();
        setSearch(searchInput);
        setPage(0);
    };

    const formatTime = (timeStr) => {
        if (!timeStr) return '—';
        return new Date(timeStr).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    };

    const formatDate = (timeStr) => {
        if (!timeStr) return '—';
        return new Date(timeStr).toLocaleDateString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    };

    const getSeverityColor = (severity) => ({
        CRITICAL: 'text-rose-700 bg-rose-50 border-rose-200',
        HIGH:     'text-orange-700 bg-orange-50 border-orange-250',
        MEDIUM:   'text-amber-700 bg-amber-50 border-amber-200',
        LOW:      'text-slate-600 bg-slate-50 border-slate-200',
    })[severity] || 'text-gray-700 bg-gray-50 border-gray-200';

    return (
        <div className="min-h-screen bg-gray-50 p-6 md:p-8 animate-fade-in">
            <div className="max-w-7xl mx-auto space-y-6">

                {/* Dashboard Header */}
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center bg-white p-6 rounded-xl shadow-sm border border-gray-200 gap-4">
                    <div className="flex items-center gap-3">
                        <Activity className="w-8 h-8 text-blue-600 animate-pulse" />
                        <div>
                            <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
                            <p className="text-gray-500 text-sm">Observability, runtime state, and active alerts overview.</p>
                        </div>
                    </div>
                    
                    <button
                        onClick={() => {
                            fetchCoreDashboard();
                            fetchMonitors();
                        }}
                        className="p-2 border border-gray-200 rounded-lg hover:bg-gray-100 text-gray-500 hover:text-gray-950 transition-colors flex items-center gap-2 text-xs font-semibold"
                        title="Reload metrics"
                    >
                        <RefreshCw className={`w-3.5 h-3.5 ${summaryLoading ? 'animate-spin' : ''}`} />
                        Refresh
                    </button>
                </div>

                {summary && summary.totalMonitors === 0 ? (
                    <div className="max-w-4xl mx-auto space-y-6 mt-8">
                        <div className="bg-gradient-to-br from-slate-900 to-indigo-950 text-white rounded-2xl p-8 border border-white/10 shadow-lg space-y-4">
                            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-500/10 border border-blue-500/20 text-blue-400 text-xs font-semibold uppercase tracking-wider">
                                <Globe className="w-3.5 h-3.5" />
                                Product Onboarding
                            </div>
                            <h2 className="text-3xl font-extrabold tracking-tight">Welcome to PulseWatch</h2>
                            <p className="text-slate-350 text-sm max-w-2xl leading-relaxed">
                                PulseWatch is a high-performance observability platform. Set up monitoring checks for your public portals, REST APIs, or health endpoints in under 30 seconds.
                            </p>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            {[
                                {
                                    title: 'Monitor a Website',
                                    desc: 'Check uptime and response latency of public landing pages.',
                                    url: 'https://github.com',
                                    name: 'GitHub Website',
                                    badge: 'Website'
                                },
                                {
                                    title: 'Monitor a Public API',
                                    desc: 'Track connectivity and response parameters of open REST APIs.',
                                    url: 'https://api.github.com',
                                    name: 'GitHub API',
                                    badge: 'REST API'
                                },
                                {
                                    title: 'Monitor a Mock endpoint',
                                    desc: 'Verify endpoint routing behavior with static REST data structures.',
                                    url: 'https://jsonplaceholder.typicode.com/posts/1',
                                    name: 'JSONPlaceholder API',
                                    badge: 'JSON API'
                                },
                                {
                                    title: 'Monitor a Health endpoint',
                                    desc: 'Trigger synthetic checks against custom Uptime test servers.',
                                    url: 'https://httpbin.org/status/200',
                                    name: 'HttpBin Health check',
                                    badge: 'Health endpoint'
                                }
                            ].map(template => (
                                <div key={template.title} className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm flex flex-col justify-between space-y-4">
                                    <div className="space-y-1.5">
                                        <div className="flex justify-between items-start">
                                            <h3 className="font-bold text-gray-900 text-sm">{template.title}</h3>
                                            <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-blue-755 border border-blue-100">
                                                {template.badge}
                                            </span>
                                        </div>
                                        <p className="text-xs text-gray-500 leading-relaxed">{template.desc}</p>
                                        <p className="text-[10px] font-mono text-gray-400 select-all truncate bg-gray-50 px-2 py-1 rounded">{template.url}</p>
                                    </div>
                                    <Link 
                                        to={`/monitors?create=true&url=${encodeURIComponent(template.url)}&name=${encodeURIComponent(template.name)}`}
                                        className="w-full text-center py-2 border border-blue-600 text-blue-600 hover:bg-blue-50/50 rounded-lg text-xs font-bold transition-all block"
                                    >
                                        Monitor with 1-Click
                                    </Link>
                                </div>
                            ))}
                        </div>
                    </div>
                ) : (
                    <>
                        {/* 1. KEY PERFORMANCE INDICATORS ROW */}
                        {summary && (
                            <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-7 gap-3">
                                <div className="bg-white rounded-xl p-4 text-center border border-gray-250 shadow-sm">
                                    <div className="text-2xl font-black text-gray-900">{summary.totalMonitors}</div>
                                    <div className="text-[10px] uppercase font-bold text-gray-400 mt-1">Total Monitors</div>
                                </div>
                                <div className="bg-emerald-50/50 rounded-xl p-4 text-center border border-emerald-200 shadow-sm">
                                    <div className="text-2xl font-black text-emerald-800">{summary.upCount}</div>
                                    <div className="text-[10px] uppercase font-bold text-emerald-500 mt-1">Healthy</div>
                                </div>
                                <div className={`rounded-xl p-4 text-center border shadow-sm ${summary.downCount > 0 ? 'bg-red-50 text-red-900 border-red-200 animate-pulse' : 'bg-white border-gray-250'}`}>
                                    <div className={`text-2xl font-black ${summary.downCount > 0 ? 'text-red-700' : 'text-gray-900'}`}>{summary.downCount}</div>
                                    <div className="text-[10px] uppercase font-bold text-gray-400 mt-1">Down</div>
                                </div>
                                <div className="bg-amber-50/50 rounded-xl p-4 text-center border border-amber-250 shadow-sm">
                                    <div className="text-2xl font-black text-amber-800">{summary.maintenanceCount}</div>
                                    <div className="text-[10px] uppercase font-bold text-amber-500 mt-1">Maintenance</div>
                                </div>
                                <div className={`rounded-xl p-4 text-center border shadow-sm ${summary.openIncidents > 0 ? 'bg-orange-50 text-orange-900 border-orange-250' : 'bg-white border-gray-250'}`}>
                                    <div className={`text-2xl font-black ${summary.openIncidents > 0 ? 'text-orange-700' : 'text-gray-900'}`}>{summary.openIncidents}</div>
                                    <div className="text-[10px] uppercase font-bold text-gray-400 mt-1">Open Incidents</div>
                                </div>
                                <div className="bg-blue-50/50 rounded-xl p-4 text-center border border-blue-200 shadow-sm">
                                    <div className="text-2xl font-black text-blue-800">{summary.avgUptimePercent}%</div>
                                    <div className="text-[10px] uppercase font-bold text-blue-500 mt-1">Avg Uptime</div>
                                </div>
                                <div className="bg-purple-50/50 rounded-xl p-4 text-center border border-purple-200 shadow-sm">
                                    <div className="text-2xl font-black text-purple-800">{summary.avgResponseTimeLast24Hours}ms</div>
                                    <div className="text-[10px] uppercase font-bold text-purple-500 mt-1">Avg Latency</div>
                                </div>
                            </div>
                        )}

                        {/* Health Split Bar */}
                        <HealthDistribution summary={summary} />

                        {/* 2. MAIN LAYOUT (2 COLUMNS) */}
                        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

                            {/* LEFT COLUMN: Monitors + Telemetry Charts */}
                            <div className="lg:col-span-2 space-y-6">
                                
                                {/* Monitors List Subpanel */}
                                <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
                                    <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 mb-4">
                                        <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider">Metrics Explorer</h3>
                                        
                                        <form onSubmit={handleSearch} className="flex gap-2 w-full sm:w-auto">
                                            <div className="relative flex-1">
                                                <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                                                <input
                                                    type="text"
                                                    placeholder="Search monitors…"
                                                    value={searchInput}
                                                    onChange={e => setSearchInput(e.target.value)}
                                                    className="w-24 sm:w-48 pl-8 pr-2 py-1.5 border border-gray-200 rounded-lg text-xs focus:outline-none focus:ring-2 focus:ring-blue-500"
                                                />
                                            </div>
                                            <select
                                                value={statusFilter}
                                                onChange={e => { setStatusFilter(e.target.value); setPage(0); }}
                                                className="border border-gray-200 rounded-lg px-2 py-1.5 text-xs text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                            >
                                                <option value="">All States</option>
                                                <option value="UP">UP</option>
                                                <option value="DOWN">DOWN</option>
                                                <option value="MAINTENANCE">MAINTENANCE</option>
                                            </select>
                                        </form>
                                    </div>

                                    {loading ? (
                                        <div className="flex justify-center py-10">
                                            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" />
                                        </div>
                                    ) : monitors.length === 0 ? (
                                        <div className="text-center py-10 text-gray-400">
                                            <Globe className="w-12 h-12 mx-auto mb-3 opacity-40" />
                                            <p className="text-sm">No monitors match current filter.</p>
                                        </div>
                                    ) : (
                                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                                            {monitors.map(m => (
                                                <div
                                                    key={m.id}
                                                    onClick={() => setSelectedMonitorId(m.id)}
                                                    className={`flex items-center justify-between p-3 rounded-lg border cursor-pointer transition-all ${
                                                        selectedMonitorId === m.id 
                                                            ? 'bg-blue-50/50 border-blue-200 text-blue-900 font-medium' 
                                                            : 'border-gray-100 hover:border-gray-200 hover:bg-gray-50'
                                                    }`}
                                                >
                                                    <div className="truncate min-w-0 pr-2">
                                                        <span className="font-semibold text-xs text-gray-900 block truncate">{m.name}</span>
                                                        <span className="text-gray-450 text-[10px] font-mono block truncate mt-0.5">{m.url}</span>
                                                    </div>
                                                    <StatusBadge status={m.currentStatus} />
                                                </div>
                                            ))}
                                        </div>
                                    )}

                                    {/* Pagination */}
                                    {totalPages > 1 && (
                                        <div className="flex items-center justify-between mt-4 pt-3 border-t border-gray-150 text-xs">
                                            <span className="text-gray-400">{totalElements} total monitors</span>
                                            <div className="flex items-center gap-2">
                                                <button
                                                    onClick={() => setPage(p => Math.max(0, p - 1))}
                                                    disabled={page === 0}
                                                    className="p-1 rounded border border-gray-200 disabled:opacity-30 hover:bg-gray-100"
                                                >
                                                    <ChevronLeft className="w-3.5 h-3.5" />
                                                </button>
                                                <span className="text-gray-600">Page {page + 1} / {totalPages}</span>
                                                <button
                                                    onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                                                    disabled={page >= totalPages - 1}
                                                    className="p-1 rounded border border-gray-200 disabled:opacity-30 hover:bg-gray-100"
                                                >
                                                    <ChevronRight className="w-3.5 h-3.5" />
                                                </button>
                                            </div>
                                        </div>
                                    )}
                                </div>

                                {/* Telemetry Charts */}
                                {selectedMonitorId && stats && (
                                    <div className="space-y-4">
                                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                                            <MetricCard title="Uptime (30d)" value={stats.uptime?.toFixed(2)} unit="%" icon={Activity} colorClass="bg-emerald-500" />
                                            <MetricCard title="Average Latency" value={stats.avgLatency} unit="ms" icon={Clock} colorClass="bg-blue-500" />
                                            <MetricCard title="P95 Latency" value={stats.p95Latency} unit="ms" icon={Zap} colorClass="bg-purple-500" />
                                            <MetricCard title="Error Rate" value={stats.errorRate?.toFixed(2)} unit="%" icon={AlertTriangle} colorClass={stats.errorRate > 0 ? 'bg-red-500 animate-pulse' : 'bg-gray-450'} />
                                        </div>
                                        
                                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                            <LatencyChart data={latencyData} />
                                            <UptimeChart data={uptimeData} />
                                        </div>

                                        <IncidentTable monitorId={selectedMonitorId} />
                                    </div>
                                )}

                            </div>

                            {/* RIGHT COLUMN: Incidents, Alert deliveries, Down systems */}
                            <div className="space-y-6">

                                {/* DOWN SYSTEMS OR REQUIRES ATTENTION */}
                                <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5 space-y-3">
                                    <h3 className="text-sm font-bold text-gray-450 uppercase tracking-wider flex items-center gap-1.5">
                                        <AlertTriangle className="w-4 h-4 text-red-500 animate-pulse" />
                                        Requires Attention
                                    </h3>
                                    
                                    {summary && summary.downCount === 0 ? (
                                        <div className="bg-emerald-50 border border-emerald-250 p-4 rounded-xl text-center space-y-2">
                                            <ShieldCheck className="w-8 h-8 text-emerald-500 mx-auto" />
                                            <p className="text-xs font-bold text-emerald-800">All Systems Healthy</p>
                                            <p className="text-[10px] text-emerald-600">No monitors are reporting failure states.</p>
                                        </div>
                                    ) : (
                                        <div className="space-y-2">
                                            {downMonitors.length === 0 ? (
                                                <p className="text-xs text-gray-400 text-center py-3">Loading down monitors…</p>
                                            ) : downMonitors.map(m => (
                                                <div key={m.id} className="bg-red-50/50 border border-red-200 rounded-lg p-3 flex items-center justify-between gap-2 text-xs">
                                                    <div className="truncate">
                                                        <span className="font-bold text-gray-950 truncate block">{m.name}</span>
                                                        <span className="text-[10px] text-red-700 block truncate">{m.url}</span>
                                                    </div>
                                                    <span className="px-2 py-0.5 rounded-full text-[10px] font-black bg-red-150 text-red-800 border border-red-200">
                                                        DOWN
                                                    </span>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>

                                {/* RECENT INCIDENTS (Phase 4) */}
                                <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5 space-y-3">
                                    <h3 className="text-sm font-bold text-gray-450 uppercase tracking-wider flex items-center gap-2">
                                        <ShieldAlert className="w-4 h-4 text-orange-500" />
                                        Recent Incidents
                                    </h3>

                                    {recentIncidents.length === 0 ? (
                                        <p className="text-xs text-gray-400 text-center py-4">No incidents recorded.</p>
                                    ) : (
                                        <div className="space-y-2">
                                            {recentIncidents.map(inc => (
                                                <div key={inc.id} className="p-3 border border-gray-150 rounded-lg space-y-1.5 hover:bg-gray-50/50 transition-all text-xs">
                                                    <div className="flex justify-between items-center">
                                                        <span className="font-semibold text-gray-800 truncate pr-1">{inc.monitorName || 'Unknown Monitor'}</span>
                                                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold flex-shrink-0 ${
                                                            inc.status === 'OPEN' ? 'bg-red-100 text-red-800 border border-red-200' : 'bg-emerald-100 text-emerald-800 border-emerald-250'
                                                        }`}>
                                                            {inc.status === 'OPEN' ? 'Open' : 'Resolved'}
                                                        </span>
                                                    </div>
                                                    <div className="flex justify-between items-center text-xs mt-1">
                                                        <span className="text-[10px] text-gray-400">{formatDate(inc.startedAt)}</span>
                                                        <span className={`px-1.5 rounded text-[10px] font-bold ${getSeverityColor(inc.severity)}`}>
                                                            {inc.severity}
                                                        </span>
                                                    </div>
                                                    {inc.failureReason && (
                                                        <p className="text-[10px] text-gray-500 font-mono italic break-words">
                                                            Reason: {inc.failureReason}
                                                        </p>
                                                    )}
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>

                                {/* LATEST ALERT DELIVERIES (Phase 5) */}
                                <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5 space-y-3">
                                    <h3 className="text-sm font-bold text-gray-450 uppercase tracking-wider flex items-center gap-2">
                                        <Mail className="w-4 h-4 text-blue-500" />
                                        Latest Alerts
                                    </h3>

                                    {latestAlerts.length === 0 ? (
                                        <p className="text-xs text-gray-400 text-center py-4">No alert deliveries recorded.</p>
                                    ) : (
                                        <div className="space-y-2">
                                            {latestAlerts.map(alert => (
                                                <div key={alert.id} className="p-3 border border-gray-150 rounded-lg space-y-1 hover:bg-gray-50/50 transition-all text-xs">
                                                    <div className="flex justify-between items-center text-xs">
                                                        <span className="font-bold text-gray-900 truncate pr-2">
                                                            {labelFor(alert.alertType, ALERT_TYPE_LABELS)}
                                                        </span>
                                                        <span className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                                                            alert.status === 'SENT' ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-750'
                                                        }`}>
                                                            {alert.status === 'SENT' ? 'Delivered' : (alert.status || '—')}
                                                        </span>
                                                    </div>
                                                    <p className="text-[10px] text-gray-500 truncate">{alert.recipient}</p>
                                                    {alert.errorMessage && (
                                                        <p className="text-[9px] text-red-650 font-mono truncate">{alert.errorMessage}</p>
                                                    )}
                                                    <p className="text-[9px] text-gray-400 text-right mt-1">{formatDate(alert.sentAt)}</p>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>

                            </div>

                        </div>
                    </>
                )}
            </div>
        </div>
    );
};

export default Dashboard;
