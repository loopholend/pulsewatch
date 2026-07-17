import React, { useState, useEffect, useRef, useCallback } from 'react';

const RealtimeConnection = () => {
    const [status, setStatus] = useState('connecting'); // 'connecting' | 'connected' | 'disconnected'
    const eventSourceRef = useRef(null);
    const retryTimeoutRef = useRef(null);

    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

    const connect = useCallback(() => {
        const token = localStorage.getItem('token');
        if (!token) {
            setStatus('disconnected');
            return;
        }

        // Clean up any existing connection
        if (eventSourceRef.current) {
            eventSourceRef.current.close();
        }

        setStatus('connecting');

        try {
            const es = new EventSource(`${apiBaseUrl}/realtime/connect?token=${encodeURIComponent(token)}`);
            eventSourceRef.current = es;

            es.onopen = () => {
                setStatus('connected');
                if (retryTimeoutRef.current) {
                    clearTimeout(retryTimeoutRef.current);
                    retryTimeoutRef.current = null;
                }
            };

            es.onerror = () => {
                setStatus('disconnected');
                es.close();
                // Auto retry after 5 seconds
                if (!retryTimeoutRef.current) {
                    retryTimeoutRef.current = setTimeout(() => {
                        retryTimeoutRef.current = null;
                        connect();
                    }, 5000);
                }
            };

            // Custom event listeners
            es.addEventListener('CONNECTED', (e) => {
                console.log('SSE connection successfully verified:', e.data);
            });

            es.addEventListener('MONITOR_UPDATED', (e) => {
                try {
                    const data = JSON.parse(e.data);
                    window.dispatchEvent(new CustomEvent('pulsewatch-realtime', {
                        detail: { type: 'MONITOR_UPDATED', payload: data }
                    }));
                } catch (err) {
                    console.error('Failed to parse MONITOR_UPDATED payload', err);
                }
            });

            es.addEventListener('INCIDENT_UPDATED', (e) => {
                try {
                    const data = JSON.parse(e.data);
                    window.dispatchEvent(new CustomEvent('pulsewatch-realtime', {
                        detail: { type: 'INCIDENT_UPDATED', payload: data }
                    }));
                } catch (err) {
                    console.error('Failed to parse INCIDENT_UPDATED payload', err);
                }
            });

        } catch (err) {
            console.error('Error establishing SSE connection:', err);
            setStatus('disconnected');
        }
    }, [apiBaseUrl]);

    useEffect(() => {
        connect();

        return () => {
            if (eventSourceRef.current) {
                eventSourceRef.current.close();
            }
            if (retryTimeoutRef.current) {
                clearTimeout(retryTimeoutRef.current);
            }
        };
    }, [connect]);

    return (
        <div className="mt-4 p-2.5 rounded-lg bg-slate-800/40 border border-slate-800/80 flex items-center justify-between text-xs">
            <div className="flex items-center gap-2">
                <span className={`w-2.5 h-2.5 rounded-full ${
                    status === 'connected' ? 'bg-emerald-500 animate-pulse shadow-[0_0_8px_#10b981]' : 
                    status === 'connecting' ? 'bg-amber-500 animate-pulse shadow-[0_0_8px_#f59e0b]' : 
                    'bg-red-500 animate-pulse shadow-[0_0_8px_#ef4444]'
                }`} />
                <span className="font-semibold text-slate-350 tracking-wide">
                    {status === 'connected' ? 'Live Stream Connected' : 
                     status === 'connecting' ? 'Connecting Stream...' : 
                     'Offline (Disconnected)'}
                </span>
            </div>
            {status === 'disconnected' && (
                <button 
                    onClick={connect} 
                    className="px-2 py-0.5 rounded bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-750 text-[10px] font-bold transition-colors"
                >
                    Retry
                </button>
            )}
        </div>
    );
};

export default RealtimeConnection;
