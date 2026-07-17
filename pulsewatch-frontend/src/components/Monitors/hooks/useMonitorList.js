// src/components/Monitors/hooks/useMonitorList.js
// Manages the paginated monitor list state (filtering, pagination, SSE refresh).

import { useState, useCallback, useEffect } from 'react';
import { api } from '../../../api/axiosConfig';

/**
 * Encapsulates all state and data-fetching for the monitor list panel.
 *
 * @param {object} opts
 * @param {string} opts.initialMonitorId  - Pre-select a monitor by URL param
 */
export function useMonitorList({ initialMonitorId } = {}) {
    const [page, setPage]                     = useState(0);
    const [search, setSearch]                 = useState('');
    const [statusFilter, setStatusFilter]     = useState('');
    const [monitorPage, setMonitorPage]       = useState(null);
    const [selectedMonitorId, setSelectedMonitorId] = useState(initialMonitorId || null);
    const [loading, setLoading]               = useState(true);
    const [error, setError]                   = useState('');

    const fetchMonitors = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            const params = new URLSearchParams({ page, size: 15, sortBy: 'createdAt', sortDir: 'desc' });
            if (search)       params.append('search', search);
            if (statusFilter) params.append('status', statusFilter);

            const res = await api.get(`/monitors?${params}`);
            setMonitorPage(res.data);

            const content = res.data.content || [];
            if (!selectedMonitorId && content.length > 0) {
                setSelectedMonitorId(content[0].id);
            }
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to load monitors.');
        } finally {
            setLoading(false);
        }
    }, [page, search, statusFilter]);

    useEffect(() => { fetchMonitors(); }, [fetchMonitors]);

    // SSE: re-fetch when a realtime event arrives
    useEffect(() => {
        const handler = (e) => {
            if (e.detail?.type === 'MONITOR_UPDATED') fetchMonitors();
        };
        window.addEventListener('pulsewatch-realtime', handler);
        return () => window.removeEventListener('pulsewatch-realtime', handler);
    }, [fetchMonitors]);

    const monitors = monitorPage?.content || [];
    const totalPages = monitorPage?.totalPages || 0;
    const totalElements = monitorPage?.totalElements || 0;

    return {
        monitors,
        totalPages,
        totalElements,
        page,
        setPage,
        search,
        setSearch,
        statusFilter,
        setStatusFilter,
        selectedMonitorId,
        setSelectedMonitorId,
        loading,
        error,
        refetch: fetchMonitors,
    };
}
