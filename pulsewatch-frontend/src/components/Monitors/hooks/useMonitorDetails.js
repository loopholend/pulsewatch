// src/components/Monitors/hooks/useMonitorDetails.js
// Fetches and manages state for a single monitor's detail panel:
// overview, analytics stats, latency/uptime chart data, and history.

import { useState, useEffect, useCallback } from 'react';
import { api } from '../../../api/axiosConfig';

/**
 * @param {string|null} monitorId  UUID of the selected monitor
 */
export function useMonitorDetails(monitorId) {
    const [overview, setOverview]         = useState(null);
    const [stats, setStats]               = useState(null);
    const [latencyData, setLatencyData]   = useState([]);
    const [uptimeData, setUptimeData]     = useState([]);
    const [history, setHistory]           = useState([]);
    const [loading, setLoading]           = useState(false);
    const [error, setError]               = useState('');
    const [refreshTrigger, setRefreshTrigger] = useState(0);

    const refresh = useCallback(() => {
        setRefreshTrigger(t => t + 1);
    }, []);

    useEffect(() => {
        if (!monitorId) {
            setOverview(null);
            setStats(null);
            setLatencyData([]);
            setUptimeData([]);
            setHistory([]);
            return;
        }

        let cancelled = false;
        setLoading(true);
        setError('');

        const fetchAll = async () => {
            try {
                const [overviewRes, statsRes, latencyRes, uptimeRes, historyRes] = await Promise.all([
                    api.get(`/monitors/${monitorId}/overview`),
                    api.get(`/analytics/${monitorId}`),
                    api.get(`/analytics/${monitorId}/latency`),
                    api.get(`/analytics/${monitorId}/uptime`),
                    api.get(`/monitors/${monitorId}/history?window=24h`),
                ]);
                if (!cancelled) {
                    setOverview(overviewRes.data);
                    setStats(statsRes.data);
                    setLatencyData(latencyRes.data || []);
                    setUptimeData(uptimeRes.data || []);
                    setHistory(historyRes.data || []);
                }
            } catch (err) {
                if (!cancelled) {
                    setError(err.response?.data?.message || 'Failed to load monitor details.');
                }
            } finally {
                if (!cancelled) setLoading(false);
            }
        };

        fetchAll();
        return () => { cancelled = true; };
    }, [monitorId, refreshTrigger]);

    // SSE: refresh when this specific monitor is updated
    useEffect(() => {
        if (!monitorId) return;
        const handler = (e) => {
            const { type, payload } = e.detail || {};
            if ((type === 'MONITOR_UPDATED' || type === 'INCIDENT_UPDATED') &&
                (payload?.id === monitorId || payload?.monitorId === monitorId)) {
                refresh();
            }
        };
        window.addEventListener('pulsewatch-realtime', handler);
        return () => window.removeEventListener('pulsewatch-realtime', handler);
    }, [monitorId, refresh]);

    return { overview, stats, latencyData, uptimeData, history, loading, error, refresh };
}
