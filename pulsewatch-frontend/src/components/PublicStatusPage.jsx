import React, { useState, useEffect } from 'react';
import { CheckCircle, XCircle, AlertTriangle, Clock, Globe, ShieldCheck, ShieldAlert, Calendar } from 'lucide-react';

/**
 * Public Status Page — no authentication required.
 * Reads the slug from the URL path: /status/:slug
 */
const PublicStatusPage = () => {
    const slug = window.location.pathname.split('/status/')[1] || '';
    const [page, setPage] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!slug) {
            setError('No status page slug provided.');
            setLoading(false);
            return;
        }

        const fetchPage = async () => {
            setLoading(true);
            try {
                // Use relative URL so request flows through nginx proxy in Docker/Azure
                const res = await fetch(`/public/status/${slug}`);
                if (!res.ok) throw new Error('Status page not found');
                const data = await res.json();
                setPage(data);
                setError(null);
            } catch (err) {
                setError(err.message || 'Failed to load status page');
            } finally {
                setLoading(false);
            }
        };

        fetchPage();
        const interval = setInterval(fetchPage, 30000); // refresh every 30s
        return () => clearInterval(interval);
    }, [slug]);

    const formatTime = (timeStr) => {
        if (!timeStr) return '—';
        return new Date(timeStr).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' });
    };

    const overallConfig = {
        UP: {
            color: 'from-emerald-500 to-teal-600',
            text: 'All Systems Operational',
            Icon: ShieldCheck,
            textColor: 'text-emerald-700',
            bgLight: 'bg-emerald-50 border-emerald-200'
        },
        DEGRADED: {
            color: 'from-amber-500 to-orange-600',
            text: 'Degraded Performance',
            Icon: AlertTriangle,
            textColor: 'text-amber-700',
            bgLight: 'bg-amber-50 border-amber-200'
        },
        DOWN: {
            color: 'from-red-500 to-rose-600',
            text: 'Major Outage',
            Icon: ShieldAlert,
            textColor: 'text-red-700',
            bgLight: 'bg-red-50 border-red-200'
        },
        UNKNOWN: {
            color: 'from-gray-500 to-slate-600',
            text: 'Status Unknown',
            Icon: Info,
            textColor: 'text-gray-700',
            bgLight: 'bg-gray-50 border-gray-200'
        }
    };

    const serviceStatusConfig = {
        UP: { icon: <CheckCircle className="w-5 h-5 text-emerald-500" />, label: 'Operational', color: 'text-emerald-700 bg-emerald-50 border-emerald-100' },
        DOWN: { icon: <XCircle className="w-5 h-5 text-red-500" />, label: 'Outage', color: 'text-red-700 bg-red-50 border-red-150' },
        MAINTENANCE: { icon: <Clock className="w-5 h-5 text-amber-500" />, label: 'Maintenance', color: 'text-amber-700 bg-amber-50 border-amber-200' },
        UNKNOWN: { icon: <AlertTriangle className="w-5 h-5 text-gray-400" />, label: 'Unknown', color: 'text-gray-600 bg-gray-50 border-gray-150' }
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center p-6 text-center">
                <XCircle className="w-16 h-16 text-red-500 mb-4 animate-bounce" />
                <h1 className="text-2xl font-bold text-gray-900">Status Page Not Found</h1>
                <p className="text-gray-500 mt-2 max-w-sm">{error}</p>
                <div className="mt-6 text-xs text-gray-400 font-mono">Slug: {slug}</div>
            </div>
        );
    }

    const overall = overallConfig[page.overallStatus] || overallConfig.UNKNOWN;
    const OverallIcon = overall.Icon;

    // Filter services under outage
    const downServices = page.services.filter(s => s.currentStatus === 'DOWN');
    // Filter services under maintenance
    const maintServices = page.services.filter(s => s.currentStatus === 'MAINTENANCE');

    return (
        <div className="min-h-screen bg-gray-50 flex flex-col justify-between font-sans">
            
            <div>
                {/* 1. Header Banner */}
                <div className={`bg-gradient-to-r ${overall.color} py-12 text-white shadow-sm`}>
                    <div className="max-w-3xl mx-auto px-6 flex items-center gap-5">
                        <div className="bg-white/10 p-3 rounded-2xl border border-white/10">
                            <OverallIcon className="w-10 h-10 text-white" />
                        </div>
                        <div>
                            <h1 className="text-3xl font-extrabold tracking-tight">{page.title}</h1>
                            <p className="text-white text-opacity-90 font-semibold text-lg mt-1">{overall.text}</p>
                        </div>
                    </div>
                </div>

                <div className="max-w-3xl mx-auto px-6 py-8 space-y-6">

                    {/* Description */}
                    {page.description && (
                        <p className="text-gray-650 text-sm leading-relaxed">{page.description}</p>
                    )}

                    {/* 2. Active Outages / Current Incidents */}
                    {page.hasActiveIncident && downServices.length > 0 && (
                        <div className="bg-red-50 border border-red-200 rounded-xl p-5 space-y-3">
                            <div className="flex items-center gap-2.5 text-red-800">
                                <ShieldAlert className="w-5 h-5 text-red-500 flex-shrink-0 animate-pulse" />
                                <h3 className="font-extrabold text-sm uppercase tracking-wider">Active Incident Detected</h3>
                            </div>
                            <p className="text-xs text-red-700">
                                We are currently experiencing service downtime affecting components listed below. Our operations team has been alerted and is investigating.
                            </p>
                            <div className="space-y-1.5 pt-1">
                                {downServices.map(ds => (
                                    <div key={ds.monitorId} className="flex justify-between text-xs text-red-950 font-bold bg-white/40 px-3 py-1.5 rounded-lg border border-red-150">
                                        <span>{ds.serviceName}</span>
                                        <span>Service Outage</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* 3. Active Maintenance Windows */}
                    {maintServices.length > 0 && (
                        <div className="bg-amber-50 border border-amber-250 rounded-xl p-5 space-y-3">
                            <div className="flex items-center gap-2.5 text-amber-800">
                                <Calendar className="w-5 h-5 text-amber-500 flex-shrink-0" />
                                <h3 className="font-extrabold text-sm uppercase tracking-wider">Scheduled Maintenance Active</h3>
                            </div>
                            <p className="text-xs text-amber-700">
                                The following services are undergoing scheduled maintenance. Services will resume normal operations shortly.
                            </p>
                            <div className="space-y-1.5 pt-1">
                                {maintServices.map(ms => (
                                    <div key={ms.monitorId} className="flex justify-between text-xs text-amber-950 font-bold bg-white/40 px-3 py-1.5 rounded-lg border border-amber-100">
                                        <span>{ms.serviceName}</span>
                                        <span>Maintenance Mode</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* 4. Service Components List */}
                    <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
                        <div className="px-6 py-4 border-b border-gray-200 bg-gray-50/50 flex justify-between items-center">
                            <h2 className="text-xs font-bold text-gray-400 uppercase tracking-wider">Service Components</h2>
                            <span className="inline-flex items-center gap-1 text-[10px] font-bold text-gray-450 uppercase">
                                <Globe className="w-3 h-3 text-blue-500" /> Public Telemetry
                            </span>
                        </div>

                        {page.services.length === 0 ? (
                            <div className="p-12 text-center text-gray-400 space-y-2">
                                <Globe className="w-12 h-12 text-gray-300 mx-auto" />
                                <p className="font-semibold text-gray-700">No services configured</p>
                                <p className="text-xs">No active monitors have been published to this status page.</p>
                            </div>
                        ) : (
                            <ul className="divide-y divide-gray-150">
                                {page.services.map((service, idx) => {
                                    const config = serviceStatusConfig[service.currentStatus] || serviceStatusConfig.UNKNOWN;
                                    return (
                                        <li key={idx} className="flex items-center justify-between px-6 py-4 hover:bg-gray-50/50 transition-colors">
                                            <div className="flex items-center gap-3">
                                                {config.icon}
                                                <div>
                                                    <span className="font-bold text-gray-900 text-sm block">{service.serviceName}</span>
                                                    <span className="text-[10px] text-gray-400 font-mono">
                                                        {service.lastChecked
                                                            ? `Checked ${new Date(service.lastChecked).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
                                                            : 'Not yet checked'}
                                                    </span>
                                                </div>
                                            </div>
                                            <div className="flex items-center gap-3 text-xs">
                                                {service.avgResponseTimeMs != null && (
                                                    <span className="text-gray-500 font-mono text-[10px] hidden sm:inline">
                                                        {service.avgResponseTimeMs}ms avg
                                                    </span>
                                                )}
                                                {service.uptimePercent != null && (
                                                    <span className="text-gray-500 font-mono text-[10px] hidden sm:inline">
                                                        {service.uptimePercent.toFixed(2)}% uptime
                                                    </span>
                                                )}
                                                <span className={`px-2 py-0.5 border rounded-full text-[10px] font-bold ${config.color}`}>
                                                    {config.label}
                                                </span>
                                            </div>
                                        </li>
                                    );
                                })}
                            </ul>
                        )}
                    </div>

                    {/* 5. Uptime History & Incident Log */}
                    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 space-y-4">
                        <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider">Incident History (Past 7 Days)</h3>
                        
                        {!page.hasActiveIncident && downServices.length === 0 ? (
                            <div className="flex items-center gap-3 p-4 bg-emerald-50/50 border border-emerald-150 rounded-xl text-emerald-800">
                                <CheckCircle className="w-5 h-5 text-emerald-500 flex-shrink-0" />
                                <span className="text-xs font-semibold">No operational outages reported in the last 7 days. Systems are fully stable.</span>
                            </div>
                        ) : (
                            <div className="space-y-3">
                                <div className="border-l-2 border-red-500 pl-4 space-y-1">
                                    <span className="text-xs font-bold text-gray-700">{new Date().toLocaleDateString([], { dateStyle: 'medium' })}</span>
                                    <p className="text-xs text-gray-500">Service outage detected on component(s): {downServices.map(s => s.serviceName).join(', ')}</p>
                                </div>
                            </div>
                        )}
                    </div>

                </div>
            </div>

            {/* Footer */}
            <footer className="border-t border-gray-200 py-6 text-center text-xs text-gray-450 bg-white">
                <div className="max-w-3xl mx-auto px-6 flex flex-col sm:flex-row justify-between items-center gap-2">
                    <span>Powered by <span className="font-bold text-blue-600">PulseWatch Observability Platform</span></span>
                    <span>Last updated: {formatTime(page.lastUpdated)}</span>
                </div>
            </footer>

        </div>
    );
};

export default PublicStatusPage;
