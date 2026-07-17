import React, { useEffect, useState, useCallback, useRef } from 'react';
import { useParams, useNavigate, useLocation, useSearchParams } from 'react-router-dom';
import AssertionsPanel from './AssertionsPanel';
import MaintenancePanel from './MaintenancePanel';
import ConfirmDialog from './ui/ConfirmDialog';
import MonitorWizard from './MonitorWizard';
import { api } from '../api/axiosConfig';
import { 
  Plus, Edit2, Trash2, Search, Activity, Play, Square, 
  Info, ShieldAlert, X, Clock, Zap, AlertTriangle, Globe,
  CheckCircle2, XCircle, Settings, Calendar, ShieldCheck, RefreshCw,
  ChevronDown, ChevronUp, AlertCircle, Copy, Check
} from 'lucide-react';
import { MONITOR_TYPE_LABELS, formatInterval } from '../utils/enumLabels';

const MonitorTypes = ['HTTP', 'TCP', 'PING', 'DATABASE'];
const HttpMethods = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HEAD', 'OPTIONS'];

const analyzeUrl = (url) => {
    if (!url) return null;
    
    const hasProtocol = /^https?:\/\//i.test(url);
    const isHttps = /^https:\/\//i.test(url);
    
    let suggestedName = '';
    try {
        let cleanUrl = url;
        if (!hasProtocol) {
            cleanUrl = 'http://' + url;
        }
        const parsed = new URL(cleanUrl);
        let host = parsed.hostname;
        if (host === 'localhost' || host === '127.0.0.1') {
            suggestedName = 'Local Development';
        } else {
            host = host.replace(/^www\./i, '');
            const parts = host.split('.');
            if (parts.length > 0) {
                suggestedName = parts[0].charAt(0).toUpperCase() + parts[0].slice(1);
                if (parts.length > 2 && parts[0].toLowerCase() === 'api') {
                    suggestedName = parts[1].charAt(0).toUpperCase() + parts[1].slice(1) + ' API';
                }
            }
        }
    } catch (e) {
        const match = url.match(/^(?:https?:\/\/)?([^/?#:]+)/i);
        if (match && match[1]) {
            let host = match[1].replace(/^www\./i, '');
            const parts = host.split('.');
            suggestedName = parts[0].charAt(0).toUpperCase() + parts[0].slice(1);
        }
    }

    return {
        hasProtocol,
        isHttps,
        suggestedName: suggestedName || 'Target Host'
    };
};

// Standard HTTP response messages
const getHttpStatusMessage = (code) => {
    if (!code) return '—';
    const map = {
        200: 'OK',
        201: 'Created',
        202: 'Accepted',
        204: 'No Content',
        301: 'Moved Permanently',
        302: 'Found',
        400: 'Bad Request',
        401: 'Unauthorized',
        403: 'Forbidden',
        404: 'Not Found',
        405: 'Method Not Allowed',
        500: 'Internal Server Error',
        502: 'Bad Gateway',
        550: 'Permission Denied',
        503: 'Service Unavailable',
        504: 'Gateway Timeout'
    };
    return map[code] || `HTTP status ${code}`;
};

// ── Real-time runtime status helper ──────────────────────────────────────────
const RuntimeStateBadge = ({ active, lastCheckedAt, intervalSeconds }) => {
    const [now, setNow] = useState(Date.now());

    useEffect(() => {
        const timer = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(timer);
    }, []);

    if (!active) {
        return (
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-gray-100 text-gray-700 border border-gray-200">
                <span className="w-1.5 h-1.5 bg-gray-400 rounded-full"></span>
                Paused
            </span>
        );
    }

    if (!lastCheckedAt) {
        return (
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200">
                <span className="w-1.5 h-1.5 bg-blue-500 rounded-full animate-ping"></span>
                Running • Pending check
            </span>
        );
    }

    const lastCheckedMs = new Date(lastCheckedAt).getTime();
    const elapsedSecs = Math.max(0, Math.floor((now - lastCheckedMs) / 1000));
    const nextCheckMs = lastCheckedMs + (intervalSeconds * 1000);
    const remainingSecs = Math.max(0, Math.floor((nextCheckMs - now) / 1000));

    const formatRemaining = (s) => {
        if (s < 60) return `${s}s`;
        const m = Math.floor(s / 60);
        const sec = s % 60;
        return `${m}m ${sec}s`;
    };

    const formatElapsed = (s) => {
        if (s < 60) return `${s}s ago`;
        const m = Math.floor(s / 60);
        const sec = s % 60;
        return `${m}m ${sec}s ago`;
    };

    return (
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-55 text-emerald-700 border border-emerald-250">
            <span className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse"></span>
            Running • Next check in {formatRemaining(remainingSecs)} • Checked {formatElapsed(elapsedSecs)}
        </span>
    );
};

// ── Real-time Active Incident Banner component ──────────────────────────────
const ActiveIncidentBanner = ({ incidentStartedAt, severity }) => {
    const [now, setNow] = useState(Date.now());

    useEffect(() => {
        const timer = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(timer);
    }, []);

    const startedMs = new Date(incidentStartedAt).getTime();
    const durationSec = Math.max(0, Math.floor((now - startedMs) / 1000));

    const formatDuration = (s) => {
        if (s < 60) return `${s}s`;
        const mins = Math.floor(s / 60);
        if (mins < 60) return `${mins}m ${s % 60}s`;
        const hrs = Math.floor(mins / 60);
        return `${hrs}h ${mins % 60}m`;
    };

    return (
        <div className="bg-gradient-to-r from-red-500 to-rose-600 border border-red-650 rounded-xl p-4 text-white flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 shadow-md">
            <div className="flex items-center gap-3">
                <ShieldAlert className="w-6 h-6 text-white flex-shrink-0" />
                <div>
                    <h4 className="font-extrabold text-sm uppercase tracking-wide">Open Incident</h4>
                    <p className="text-xs text-red-100 mt-0.5">
                        Severity: <span className="font-bold underline">{severity}</span> • Started at {new Date(incidentStartedAt).toLocaleTimeString()}
                    </p>
                </div>
            </div>
            <div className="text-right text-xs font-bold bg-white/10 px-3 py-1 rounded-lg border border-white/10">
                Duration: {formatDuration(durationSec)}
            </div>
        </div>
    );
};

// Skeleton loaders
const DetailSkeleton = () => (
    <div className="bg-white rounded-xl border border-gray-200 p-6 space-y-6 animate-pulse">
        <div className="flex justify-between items-center border-b border-gray-150 pb-4">
            <div className="space-y-2">
                <div className="h-6 w-48 bg-gray-200 rounded"></div>
                <div className="h-4 w-72 bg-gray-200 rounded"></div>
            </div>
            <div className="h-8 w-24 bg-gray-200 rounded"></div>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            {[...Array(9)].map((_, i) => (
                <div key={i} className="h-20 bg-gray-100 rounded-xl"></div>
            ))}
        </div>
    </div>
);

const SidebarSkeleton = () => (
    <div className="space-y-2 animate-pulse">
        {[...Array(4)].map((_, i) => (
            <div key={i} className="h-16 bg-gray-200 rounded-lg"></div>
        ))}
    </div>
);

const MonitorsPage = () => {
  const { monitorId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();

  const [monitors, setMonitors] = useState([]);
  const [selectedMonitor, setSelectedMonitor] = useState(null); 
  const [overview, setOverview] = useState(null); 
  const [recentChecks, setRecentChecks] = useState([]); 
  
  // Search & pagination (preserved from URL params!)
  const [page, setPage] = useState(Number(searchParams.get('page')) || 0);
  const [totalPages, setTotalPages] = useState(0);
  
  const [loading, setLoading] = useState(false);
  const [detailsLoading, setDetailsLoading] = useState(false);
  
  // Local forms state
  const [search, setSearch] = useState(searchParams.get('search') || '');
  const [statusFilter, setStatusFilter] = useState(searchParams.get('status') || '');
  const [configExpanded, setConfigExpanded] = useState(false);

  // Wizard state (for CREATE flow)
  const [showWizard, setShowWizard] = useState(false);
  const [wizardInitialUrl, setWizardInitialUrl] = useState('');
  const [wizardInitialName, setWizardInitialName] = useState('');

  // Form modal states (for EDIT flow only)
  const [showModal, setShowModal] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [form, setForm] = useState({
    id: null,
    name: '',
    url: '',
    monitorType: 'HTTP',
    method: 'GET',
    intervalSeconds: 60,
    timeoutMs: 5000,
    expectedStatus: 200,
  });
  const [formError, setFormError] = useState('');
  const [formLoading, setFormLoading] = useState(false);
  const [modalAdvancedExpanded, setModalAdvancedExpanded] = useState(false);

  // Toast Notification State (Success / Error toast notifications)
  const [toast, setToast] = useState({ message: '', type: 'success' });
  const pollTimerRef = useRef(null);

  // Copy Health Badge state
  const [copiedSnippet, setCopiedSnippet] = useState(null);
  const copyBadgeSnippet = (text, type) => {
    navigator.clipboard.writeText(text);
    setCopiedSnippet(type);
    setTimeout(() => setCopiedSnippet(null), 2000);
  };

  // Confirm dialog state
  const [confirmDialog, setConfirmDialog] = useState({ open: false, monitorId: null });

  // Success Toast trigger helper
  const triggerToast = (msg, type = 'success') => {
    setToast({ message: msg, type });
    setTimeout(() => {
      setToast({ message: '', type: 'success' });
    }, 4000);
  };

  // URL query parameter listener for onboarding — opens wizard with pre-filled URL
  useEffect(() => {
    const triggerCreate = searchParams.get('create') === 'true';
    if (triggerCreate) {
      const urlParam  = searchParams.get('url')  || '';
      const nameParam = searchParams.get('name') || '';
      setWizardInitialUrl(urlParam);
      setWizardInitialName(nameParam);
      setSearchParams({});
      setShowWizard(true);
    }
  }, [searchParams, setSearchParams]);

  const handleUrlChange = (e) => {
    const val = e.target.value;
    setForm(prev => {
      const next = { ...prev, url: val };
      const analysis = analyzeUrl(val);
      if (analysis) {
        if (!prev.name || prev.name === analyzeUrl(prev.url)?.suggestedName) {
          next.name = analysis.suggestedName;
        }
        // Always use HTTP as the monitor type — HTTPS is detected from the URL protocol,
        // not a separate monitor type (the backend only supports HTTP, TCP, PING, DATABASE).
        next.monitorType = 'HTTP';
      }
      return next;
    });
  };

  const getValidationWarnings = () => {
    const warnings = [];
    if (form.url) {
      if (!/^https?:\/\//i.test(form.url)) {
        warnings.push('URL missing protocol prefix (http:// or https://).');
      }
      try {
        new URL(form.url);
      } catch (e) {
        warnings.push('URL format appears invalid.');
      }
    }
    if (form.intervalSeconds < 30) {
      warnings.push('Interval is extremely low (under 30s). This might cause rate-limiting issues on the target host.');
    }
    if (form.timeoutMs > 30000) {
      warnings.push('Timeout is extremely high (over 30s). This could lock scheduler threads unnecessarily.');
    }
    if (form.intervalSeconds * 1000 < form.timeoutMs) {
      warnings.push('Check interval is less than timeout. Timeout should always be smaller than the interval.');
    }
    return warnings;
  };

  // 1. Fetch monitors list (preserved filters)
  const fetchMonitors = useCallback(async (targetId = null) => {
    setLoading(true);
    try {
      const params = new URLSearchParams({
        page,
        size: 10,
        sortBy: 'createdAt',
        sortDir: 'desc'
      });
      if (search) params.append('search', search);
      if (statusFilter) params.append('status', statusFilter);

      const r = await api.get(`/monitors?${params}`);
      const content = r.data?.content || [];
      setMonitors(content);
      setTotalPages(r.data?.totalPages || 0);
      
      if (content.length > 0) {
        // If there's an active monitorId in the URL path, navigate or keep it
        if (monitorId) {
          const match = content.some(m => m.id === monitorId);
          if (!match && !targetId) {
             // not in filter result, but it could exist.
          }
        } else {
          // If no monitorId in URL, auto-select the first in the current listing
          navigate(`/monitors/${content[0].id}${location.search}`, { replace: true });
        }
      } else {
        if (monitorId) {
          // keep selected if we are refreshing
        }
      }
    } catch (e) {
      triggerToast(e.response?.data?.message || 'Failed to load monitors.', 'error');
    } finally {
      setLoading(false);
    }
  }, [page, search, statusFilter, monitorId, navigate, location.search]);

  // 2. Fetch specific monitor overview and history
  const fetchMonitorDetails = useCallback(async (id) => {
    if (!id) return;
    setDetailsLoading(true);
    try {
      const [overviewRes, historyRes, monitorRes] = await Promise.all([
        api.get(`/monitors/${id}/overview`),
        api.get(`/monitors/${id}/history?window=24h`),
        api.get(`/monitors/${id}`)
      ]);
      setOverview(overviewRes.data);
      setRecentChecks(historyRes.data || []);
      setSelectedMonitor(monitorRes.data);
    } catch (e) {
      console.error('Failed to load monitor details', e);
    } finally {
      setDetailsLoading(false);
    }
  }, []);

  // Sync state filters to searchParams in URL
  const updateUrlParams = useCallback(() => {
    const nextParams = {};
    if (search) nextParams.search = search;
    if (statusFilter) nextParams.status = statusFilter;
    if (page > 0) nextParams.page = page;
    setSearchParams(nextParams);
  }, [search, statusFilter, page, setSearchParams]);

  // Load listing when page or status filter changes
  useEffect(() => {
    fetchMonitors();
    updateUrlParams();
  }, [page, statusFilter]);

  // Load details when monitorId route changes
  useEffect(() => {
    if (monitorId) {
      fetchMonitorDetails(monitorId);
    } else {
      setOverview(null);
      setRecentChecks([]);
      setSelectedMonitor(null);
    }
  }, [monitorId, fetchMonitorDetails]);

  // Real-time update listener
  useEffect(() => {
    const handleRealtimeEvent = (e) => {
      const { type, payload } = e.detail;
      if (type === 'MONITOR_UPDATED') {
        fetchMonitors();
        if (monitorId && payload && payload.id === monitorId) {
          fetchMonitorDetails(monitorId);
        }
      }
    };

    window.addEventListener('pulsewatch-realtime', handleRealtimeEvent);
    return () => {
      window.removeEventListener('pulsewatch-realtime', handleRealtimeEvent);
    };
  }, [fetchMonitors, fetchMonitorDetails, monitorId]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    fetchMonitors();
    updateUrlParams();
  };

  // Open wizard for creating a new monitor
  const handleOpenCreate = () => {
    setWizardInitialUrl('');
    setWizardInitialName('');
    setShowWizard(true);
  };

  // Called when wizard succeeds — navigate to the new monitor
  const handleWizardSuccess = (newMonitorId) => {
    setShowWizard(false);
    fetchMonitors();
    if (newMonitorId) {
      navigate(`/monitors/${newMonitorId}`, { replace: false });
    }
  };

  const handleOpenEdit = (m) => {
    setIsEditing(true);
    setForm({
      id: m.id,
      name: m.name,
      url: m.url,
      monitorType: m.monitorType,
      method: m.method || 'GET',
      intervalSeconds: m.intervalSeconds,
      timeoutMs: m.timeoutMs,
      expectedStatus: m.expectedStatus || 200,
    });
    setFormError('');
    setModalAdvancedExpanded(true); // Open advanced settings by default if editing an existing monitor
    setShowModal(true);
  };

  const handleFormSubmit = async (e) => {
    e.preventDefault();
    setFormError('');
    setFormLoading(true);

    if (form.timeoutMs > form.intervalSeconds * 1000) {
      setFormError('Timeout cannot exceed the check interval.');
      setFormLoading(false);
      return;
    }

    try {
      if (isEditing) {
        await api.put(`/monitors/${form.id}`, form);
        triggerToast('Monitor updated successfully', 'success');
      } else {
        await api.post('/monitors', form);
        triggerToast('New monitor created successfully', 'success');
      }
      setShowModal(false);
      fetchMonitors();
    } catch (err) {
      setFormError(err.response?.data?.message || 'Failed to save monitor.');
      triggerToast('Validation error: check configurations', 'error');
    } finally {
      setFormLoading(false);
    }
  };

  const handleDeleteRequest = (id) => {
    setConfirmDialog({ open: true, monitorId: id });
  };

  const handleDeleteConfirm = async () => {
    const { monitorId: id } = confirmDialog;
    setConfirmDialog({ open: false, monitorId: null });
    try {
      await api.delete(`/monitors/${id}`);
      triggerToast('Monitor deleted successfully', 'success');
      navigate(`/monitors${location.search}`);
      fetchMonitors();
    } catch (e) {
      triggerToast(e.response?.data?.message || 'Failed to delete monitor.', 'error');
    }
  };

  const handleDelete = (id) => handleDeleteRequest(id);

  const handleToggleActive = async (id) => {
    try {
      await api.patch(`/monitors/${id}/toggle`);
      const updatedState = !selectedMonitor?.active;
      triggerToast(updatedState ? 'Monitor resumed successfully' : 'Monitor paused successfully', 'success');
      fetchMonitors();
      if (monitorId === id) {
        fetchMonitorDetails(id);
      }
    } catch (e) {
      triggerToast('Failed to toggle monitor state.', 'error');
    }
  };

  const getDerivedStatus = () => {
    if (!overview || overview.totalChecks === 0 || !overview.lastCheckedAt) {
      return 'PENDING';
    }
    if (overview.currentStatus === 'MAINTENANCE') {
      return 'MAINTENANCE';
    }
    if (recentChecks.length > 0) {
      return recentChecks[0].success ? 'UP' : 'DOWN';
    }
    return 'UNKNOWN';
  };

  const getStatusBadge = (status) => {
    if (status === 'PENDING') {
      return (
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-gray-100 text-gray-700 border border-gray-250 animate-pulse">
          <Clock className="w-3.5 h-3.5 text-gray-500" />
          Pending first check...
        </span>
      );
    }

    switch (status) {
      case 'UP':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-55 text-emerald-700 border border-emerald-250">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
            UP
          </span>
        );
      case 'DOWN':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-red-50 text-red-700 border border-red-200">
            <XCircle className="w-3.5 h-3.5 text-red-500 animate-pulse" />
            DOWN
          </span>
        );
      case 'MAINTENANCE':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-250">
            <Calendar className="w-3.5 h-3.5 text-amber-500" />
            MAINTENANCE
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-gray-50 text-gray-600 border border-gray-250">
            <Info className="w-3.5 h-3.5 text-gray-400" />
            UNKNOWN
          </span>
        );
    }
  };

  const getStatusCodeBadge = (code, success) => {
    if (!code) {
        return (
            <span className="px-2 py-0.5 rounded text-[10px] font-black bg-red-50 text-red-700 border border-red-155 animate-pulse">
                ERR
            </span>
        );
    }
    if (success) {
        return (
            <span className="px-2 py-0.5 rounded text-[10px] font-black bg-emerald-50 text-emerald-800 border border-emerald-100">
                {code}
            </span>
        );
    }
    return (
        <span className="px-2 py-0.5 rounded text-[10px] font-black bg-red-50 text-red-700 border border-red-155">
            {code}
        </span>
    );
  };

  const formatTime = (dateStr) => {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' });
  };

  const derivedStatus = getDerivedStatus();

  return (
    <div className="min-h-screen bg-gray-50 p-6 md:p-8">
      {/* Toast Notification (Failures + Actions) */}
      {toast.message && (
        <div className={`fixed bottom-5 right-5 z-50 px-4 py-3 rounded-lg shadow-xl flex items-center gap-2 text-sm border animate-slide-in text-white ${
            toast.type === 'success' ? 'bg-slate-900 border-white/10' : 'bg-red-600 border-red-750'
        }`}>
          {toast.type === 'success' ? <CheckCircle2 className="w-4 h-4 text-emerald-500" /> : <AlertCircle className="w-4 h-4 text-white" />}
          {toast.message}
        </div>
      )}

      <div className="max-w-7xl mx-auto space-y-6">
        
        {/* Top Header Row */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center bg-white p-6 rounded-xl shadow-sm border border-gray-205 gap-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-3">
              <Activity className="w-8 h-8 text-blue-600 animate-pulse" />
              Monitors
            </h1>
            <p className="text-gray-500 mt-0.5 text-sm">Real-time health status, check latency metrics, assertions and alerts configuration.</p>
          </div>
          <button
            onClick={handleOpenCreate}
            className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-medium px-4 py-2.5 rounded-lg transition-colors shadow-sm text-sm"
          >
            <Plus className="w-4 h-4" />
            Create Monitor
          </button>
        </div>

        <div className="flex flex-col lg:flex-row gap-6">
          
          {/* LEFT SIDEBAR: Monitors List */}
          <div className="w-full lg:w-80 flex-shrink-0 space-y-4">
            <div className="bg-white p-4 rounded-xl border border-gray-200 shadow-sm space-y-3">
              {/* Search form */}
              <form onSubmit={handleSearchSubmit} className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  type="text"
                  placeholder="Search monitors..."
                  value={search}
                  onChange={e => setSearch(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </form>

              {/* Status filter */}
              <select
                value={statusFilter}
                onChange={e => { setStatusFilter(e.target.value); setPage(0); }}
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">All Statuses</option>
                <option value="UP">UP</option>
                <option value="DOWN">DOWN</option>
                <option value="MAINTENANCE">MAINTENANCE</option>
                <option value="UNKNOWN">UNKNOWN</option>
              </select>
            </div>

            {/* Monitor List Panel */}
            <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-4 space-y-3">
              <h2 className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Monitors</h2>
              
              {loading && monitors.length === 0 ? (
                <SidebarSkeleton />
              ) : monitors.length === 0 ? (
                <div className="text-center py-6 text-gray-455 text-sm">
                  No monitors configured.
                </div>
              ) : (
                <div className="space-y-1">
                  {monitors.map(m => (
                    <div
                      key={m.id}
                      className={`w-full group flex items-center justify-between p-3 rounded-lg text-sm transition-all border ${
                        monitorId === m.id 
                          ? 'bg-blue-50/50 border-blue-200 text-blue-900 font-medium' 
                          : 'border-transparent text-gray-700 hover:bg-gray-50'
                      }`}
                    >
                      <button
                        onClick={() => navigate(`/monitors/${m.id}${location.search}`)}
                        className="flex-1 text-left truncate min-w-0"
                      >
                        <div className="flex items-center justify-between">
                          <span className="truncate pr-2 font-medium">{m.name}</span>
                          <span className={`px-1.5 py-0.5 rounded text-[10px] border font-bold flex-shrink-0 ${
                            m.currentStatus === 'UP' ? 'bg-emerald-100 text-emerald-800 border-emerald-200' :
                            m.currentStatus === 'DOWN' ? 'bg-red-100 text-red-800 border-red-200' :
                            m.currentStatus === 'MAINTENANCE' ? 'bg-amber-100 text-amber-800 border-amber-200' :
                            'bg-gray-100 text-gray-650 border-gray-200'
                          }`}>
                            {m.currentStatus}
                          </span>
                        </div>
                        <p className="text-xs text-gray-450 truncate mt-0.5">{m.url}</p>
                      </button>
                    </div>
                  ))}
                </div>
              )}

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="flex justify-between items-center mt-4 pt-3 border-t border-gray-100 text-xs">
                  <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
                    className="p-1 border border-gray-200 rounded hover:bg-gray-50 disabled:opacity-30">← Prev</button>
                  <span className="text-gray-400">Page {page + 1} / {totalPages}</span>
                  <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
                    className="p-1 border border-gray-200 rounded hover:bg-gray-50 disabled:opacity-30">Next →</button>
                </div>
              )}
            </div>
          </div>

          {/* RIGHT WORKSPACE: Health, Metrics, Assertions, Windows, Configuration */}
          <div className="flex-1 space-y-6">
            {monitors.length === 0 ? (
              // Empty State
              <div className="flex flex-col items-center justify-center bg-white rounded-xl border border-gray-200 shadow-sm p-12 text-center space-y-4">
                <Globe className="w-16 h-16 text-blue-500/20" />
                <h3 className="text-lg font-bold text-gray-900">No monitors configured.</h3>
                <p className="text-sm text-gray-500 max-w-sm">Create your first monitor to begin monitoring an API or web service in real-time.</p>
                <button
                  onClick={handleOpenCreate}
                  className="px-5 py-2.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-medium shadow-sm transition-all text-sm"
                >
                  Create Monitor
                </button>
              </div>
            ) : detailsLoading && !overview ? (
              <DetailSkeleton />
            ) : monitorId && overview ? (
              <>
                {/* 1. HEALTH OVERVIEW WORKSPACE (PRIORITY 1) */}
                <div className="bg-white rounded-xl border border-gray-250 shadow-sm overflow-hidden">
                  <div className="p-6 border-b border-gray-200 flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-gray-50/50">
                    <div className="space-y-1 flex-1 min-w-0">
                      <h2 className="text-xl font-bold text-gray-900 flex flex-wrap items-center gap-2">
                        {overview.name}
                        <RuntimeStateBadge 
                            active={overview.active} 
                            lastCheckedAt={overview.lastCheckedAt} 
                            intervalSeconds={overview.intervalSeconds} 
                        />
                      </h2>
                      <p className="text-xs font-mono text-gray-500 break-all select-all">{overview.url}</p>
                    </div>

                    {/* Monitor Actions — always visible in the header */}
                    <div className="flex items-center gap-2 flex-shrink-0">
                      {getStatusBadge(derivedStatus)}

                      {/* Pause / Resume */}
                      <button
                        onClick={() => handleToggleActive(monitorId)}
                        className={`px-3 py-1.5 rounded-lg text-xs font-semibold border flex items-center gap-1.5 transition-colors ${
                          selectedMonitor?.active 
                            ? 'bg-amber-50 text-amber-700 border-amber-200 hover:bg-amber-100' 
                            : 'bg-emerald-50 text-emerald-700 border-emerald-200 hover:bg-emerald-100'
                        }`}
                        title={selectedMonitor?.active ? 'Pause monitoring' : 'Resume monitoring'}
                      >
                        {selectedMonitor?.active ? <Square className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5" />}
                        {selectedMonitor?.active ? 'Pause' : 'Resume'}
                      </button>

                      {/* Edit */}
                      <button
                        onClick={() => handleOpenEdit(selectedMonitor)}
                        className="px-3 py-1.5 rounded-lg text-xs font-semibold border border-gray-200 text-gray-700 hover:bg-gray-50 flex items-center gap-1.5 transition-colors"
                        title="Edit monitor settings"
                      >
                        <Edit2 className="w-3.5 h-3.5" /> Edit
                      </button>

                      {/* Delete */}
                      <button
                        onClick={() => handleDeleteRequest(monitorId)}
                        className="px-3 py-1.5 rounded-lg text-xs font-semibold border border-red-200 text-red-600 hover:bg-red-50 flex items-center gap-1.5 transition-colors"
                        title="Delete monitor"
                      >
                        <Trash2 className="w-3.5 h-3.5" /> Delete
                      </button>

                      {/* Refresh */}
                      <button
                        onClick={() => fetchMonitorDetails(monitorId)}
                        className="p-2 border border-gray-200 rounded-lg hover:bg-gray-100 text-gray-500 hover:text-gray-700 transition-colors"
                        title="Refresh metrics"
                      >
                        <RefreshCw className={`w-4 h-4 ${detailsLoading ? 'animate-spin' : ''}`} />
                      </button>
                    </div>
                  </div>

                  <div className="p-6 space-y-6">
                    {overview.totalChecks === 0 ? (
                      <div className="p-6 bg-blue-50/50 border border-blue-200 rounded-xl text-center space-y-3">
                        <Clock className="w-8 h-8 text-blue-600 mx-auto animate-pulse" />
                        <h4 className="font-semibold text-blue-900">Pending Initial Execution</h4>
                        <p className="text-xs text-blue-700 max-w-md mx-auto">
                          Waiting for the scheduler to perform the first health check.
                        </p>
                      </div>
                    ) : (
                      <>
                        {/* Active Incident Alert Banner */}
                        {overview.currentIncidentStatus && (
                          <ActiveIncidentBanner 
                            incidentStartedAt={overview.currentIncidentStartedAt} 
                            severity={overview.currentIncidentSeverity} 
                          />
                        )}

                        {/* KPI Cards Grid (Telemetries + Successful metrics!) */}
                        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                          
                          <div className="p-4 bg-gray-50 rounded-xl border border-gray-200">
                            <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Uptime</p>
                            <p className="text-2xl font-bold text-gray-900 mt-1">{overview.uptime}%</p>
                          </div>

                          <div className="p-4 bg-gray-50 rounded-xl border border-gray-200">
                            <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Avg Latency</p>
                            <p className="text-2xl font-bold text-gray-900 mt-1">
                              {overview.avgLatency} <span className="text-xs font-normal text-gray-500">ms</span>
                            </p>
                          </div>

                          <div className="p-4 bg-gray-50 rounded-xl border border-gray-200">
                            <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider">P95 Latency</p>
                            <p className="text-2xl font-bold text-gray-900 mt-1">
                              {overview.p95Latency} <span className="text-xs font-normal text-gray-500">ms</span>
                            </p>
                          </div>

                          <div className="p-4 bg-gray-50 rounded-xl border border-gray-200">
                            <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Last Latency</p>
                            <p className="text-2xl font-bold text-gray-900 mt-1">
                              {overview.lastLatencyMs ? `${overview.lastLatencyMs} ms` : '—'}
                            </p>
                          </div>

                          {/* Last Successful Check Latency */}
                          <div className="p-4 bg-gray-50 rounded-xl border border-gray-200">
                            <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Last Successful Latency</p>
                            <p className="text-2xl font-bold text-emerald-700 mt-1">
                              {overview.lastSuccessfulLatencyMs ? `${overview.lastSuccessfulLatencyMs} ms` : '—'}
                            </p>
                          </div>

                          {/* Next Scheduled Execution */}
                          <div className="p-4 bg-gray-50 rounded-xl border border-gray-200 col-span-1">
                            <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Next Execution</p>
                            <p className="text-sm font-semibold text-gray-800 mt-2 font-mono">{formatTime(overview.nextCheckAt)}</p>
                          </div>

                          {/* Last Successful Execution Timestamp */}
                          <div className="p-4 bg-gray-50 rounded-xl border border-gray-200 col-span-1 md:col-span-2">
                            <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Last Successful Check</p>
                            <p className="text-sm font-semibold text-emerald-800 mt-2 font-mono">{formatDate(overview.lastSuccessfulCheckAt)}</p>
                          </div>

                          {/* Current Response Code */}
                          <div className="p-4 bg-gray-50 rounded-xl border border-gray-200">
                            <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Response Code</p>
                            <div className="mt-1 flex items-center gap-1.5">
                              {getStatusCodeBadge(overview.lastStatusCode, derivedStatus === 'UP')}
                            </div>
                          </div>

                          {/* Current Response Message */}
                          <div className="p-4 bg-gray-50 rounded-xl border border-gray-200">
                            <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Response Status</p>
                            <p className="text-sm font-semibold text-gray-900 mt-2">
                              {overview.lastStatusCode ? getHttpStatusMessage(overview.lastStatusCode) : (overview.lastFailureReason || 'Timeout')}
                            </p>
                          </div>

                          {/* Consecutive Failures */}
                          <div className="p-4 bg-gray-50 rounded-xl border border-gray-200">
                            <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Consecutive Failures</p>
                            <p className={`text-2xl font-bold mt-1 ${overview.consecutiveFailures > 0 ? 'text-red-600 animate-pulse' : 'text-gray-900'}`}>
                              {overview.consecutiveFailures}
                            </p>
                          </div>

                          {/* Total Checks */}
                          <div className="p-4 bg-gray-50 rounded-xl border border-gray-200">
                            <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Total Checks</p>
                            <p className="text-2xl font-bold text-gray-900 mt-1">{overview.totalChecks}</p>
                          </div>

                        </div>
                      </>
                    )}
                  </div>
                </div>

                {/* 2. RECENT ACTIVITY LOGS */}
                <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6 space-y-4">
                  <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                    <Clock className="w-5 h-5 text-blue-600" />
                    Recent Checks
                  </h3>

                  {recentChecks.length === 0 ? (
                    <div className="text-center py-8 text-gray-400 text-sm">
                      Waiting for the first scheduled check...
                    </div>
                  ) : (
                    <div className="overflow-x-auto">
                      <table className="w-full text-left text-sm text-gray-700">
                        <thead className="text-xs font-semibold text-gray-450 uppercase tracking-wider border-b border-gray-200 bg-gray-50/50">
                          <tr>
                            <th className="py-2.5 px-3">Time</th>
                            <th className="py-2.5 px-3">Result</th>
                            <th className="py-2.5 px-3">HTTP Status</th>
                            <th className="py-2.5 px-3">Latency</th>
                            <th className="py-2.5 px-3">Failure Reason</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-150">
                          {recentChecks.slice(0, 10).map((check, idx) => (
                            <tr 
                              key={check.id || idx} 
                              className={`hover:bg-gray-50/50 transition-colors ${
                                idx === 0 ? 'bg-blue-50/20 border-l-4 border-l-blue-500 font-semibold' : ''
                              }`}
                            >
                              <td className="py-2.5 px-3 font-mono">{formatTime(check.checkedAt)}</td>
                              <td className="py-2.5 px-3">{getStatusCodeBadge(check.statusCode, check.success)}</td>
                              <td className="py-2.5 px-3 font-mono text-xs">
                                {check.statusCode ? check.statusCode : '—'}
                              </td>
                              <td className="py-2.5 px-3">{check.responseTimeMs ? `${check.responseTimeMs} ms` : '—'}</td>
                              <td className="py-2.5 px-3 text-xs text-gray-500 font-mono">
                                {check.success ? 'OK' : (check.failureReason || 'Timeout')}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>

                {/* 3. ASSERTIONS LIST */}
                <AssertionsPanel monitorId={monitorId} />

                {/* 4. DOWNTIME WINDOWS LIST */}
                <MaintenancePanel monitorId={monitorId} />

                {/* 5. PUBLIC STATUS BADGES */}
                <div className="bg-white rounded-xl border border-gray-250 shadow-sm p-6 space-y-4">
                  <div>
                    <h3 className="text-base font-bold text-gray-800 flex items-center gap-2">
                      <Globe className="w-4 h-4 text-emerald-500" />
                      Status Badges
                    </h3>
                    <p className="text-xs text-gray-500 mt-1">
                      Embed a live health status badge for this monitor in your GitHub README, documentation, or dashboard.
                    </p>
                  </div>

                  <div className="flex flex-col md:flex-row items-start md:items-center gap-4 bg-gray-50/50 p-4 rounded-xl border border-gray-150">
                    <div className="flex-shrink-0 bg-white px-3 py-2.5 rounded-lg border border-gray-200 shadow-sm flex flex-col items-center justify-center">
                      <p className="text-[9px] text-gray-400 font-bold uppercase tracking-wider mb-2">Live Badge</p>
                      <img
                        src={`${api.defaults.baseURL.replace('/api', '')}/public/badge/${selectedMonitor?.id}`}
                        alt="Monitor Health Badge"
                        className="h-5"
                      />
                    </div>
                    <div className="flex-1 w-full space-y-3">
                      <div>
                        <div className="flex justify-between items-center mb-1">
                          <label className="text-[10px] text-gray-400 font-semibold uppercase tracking-wider">Markdown</label>
                          <button
                            onClick={() => copyBadgeSnippet(`![PulseWatch Status](${api.defaults.baseURL.replace('/api', '')}/public/badge/${selectedMonitor?.id})`, 'markdown')}
                            className="text-[10px] text-blue-600 font-bold hover:underline flex items-center gap-1"
                          >
                            {copiedSnippet === 'markdown' ? <><Check className="w-3 h-3 text-emerald-500" /> Copied</> : <><Copy className="w-3 h-3" /> Copy</>}
                          </button>
                        </div>
                        <input
                          type="text" readOnly
                          className="w-full border border-gray-200 rounded-lg px-2.5 py-1.5 text-xs font-mono bg-white select-all text-gray-600 outline-none focus:ring-1 focus:ring-blue-500"
                          value={`![PulseWatch Status](${api.defaults.baseURL.replace('/api', '')}/public/badge/${selectedMonitor?.id})`}
                        />
                      </div>
                      <div>
                        <div className="flex justify-between items-center mb-1">
                          <label className="text-[10px] text-gray-400 font-semibold uppercase tracking-wider">HTML</label>
                          <button
                            onClick={() => copyBadgeSnippet(`<a href="${window.location.origin}/monitors/${selectedMonitor?.id}"><img src="${api.defaults.baseURL.replace('/api', '')}/public/badge/${selectedMonitor?.id}" alt="PulseWatch Status" /></a>`, 'html')}
                            className="text-[10px] text-blue-600 font-bold hover:underline flex items-center gap-1"
                          >
                            {copiedSnippet === 'html' ? <><Check className="w-3 h-3 text-emerald-500" /> Copied</> : <><Copy className="w-3 h-3" /> Copy</>}
                          </button>
                        </div>
                        <input
                          type="text" readOnly
                          className="w-full border border-gray-200 rounded-lg px-2.5 py-1.5 text-xs font-mono bg-white select-all text-gray-600 outline-none focus:ring-1 focus:ring-blue-500"
                          value={`<a href="${window.location.origin}/monitors/${selectedMonitor?.id}"><img src="${api.defaults.baseURL.replace('/api', '')}/public/badge/${selectedMonitor?.id}" alt="PulseWatch Status" /></a>`}
                        />
                      </div>
                    </div>
                  </div>
                </div>

                {/* 6. CONFIGURATION PANEL - EXPANDABLE (COLLAPSED BY DEFAULT) */}
                <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6 space-y-4">
                  <button
                    onClick={() => setConfigExpanded(!configExpanded)}
                    className="w-full flex justify-between items-center text-left focus:outline-none"
                  >
                    <h3 className="text-base font-bold text-gray-800 flex items-center gap-2">
                      <Settings className="w-4 h-4 text-gray-500" />
                      Monitor Configuration
                    </h3>
                    <div className="flex items-center gap-1.5 text-xs text-blue-600 font-bold hover:underline">
                      <span>{configExpanded ? 'Hide Configuration' : 'View Configuration'}</span>
                      {configExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                    </div>
                  </button>

                  {configExpanded && (
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm text-gray-600 bg-gray-50/50 p-4 rounded-xl border border-gray-100 animate-fade-in">
                      <div>
                        <p className="text-xs text-gray-400 font-medium">Monitor Type</p>
                        <p className="font-semibold text-gray-900 mt-0.5">{MONITOR_TYPE_LABELS[selectedMonitor?.monitorType] ?? selectedMonitor?.monitorType}</p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-400 font-medium">HTTP Method</p>
                        <p className="font-semibold text-gray-900 mt-0.5">{selectedMonitor?.method || 'GET'}</p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-400 font-medium">Check Interval</p>
                        <p className="font-semibold text-gray-900 mt-0.5">{formatInterval(selectedMonitor?.intervalSeconds)}</p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-400 font-medium">Request Timeout</p>
                        <p className="font-semibold text-gray-900 mt-0.5">{selectedMonitor?.timeoutMs} ms</p>
                      </div>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <div className="flex h-96 flex-col items-center justify-center bg-white rounded-xl border border-gray-200 shadow-sm p-6 text-center text-gray-450 space-y-2">
                <Info className="w-12 h-12 text-gray-350" />
                <p className="font-medium text-gray-700">No monitor selected</p>
                <p className="text-sm">Select an existing check monitor on the left side panel to review its health overview.</p>
              </div>
            )}
          </div>
        </div>

      </div>

      {/* CREATE & EDIT MODAL OVERLAY */}
      {showModal && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full shadow-2xl border border-gray-100 overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-100 flex justify-between items-center">
              <h2 className="text-lg font-bold text-gray-900">
                {isEditing ? 'Edit Monitor Settings' : 'Create New Monitor'}
              </h2>
              <button 
                onClick={() => setShowModal(false)}
                className="p-1 hover:bg-gray-100 rounded-full text-gray-400 hover:text-gray-955"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleFormSubmit} className="p-6 space-y-4">
              {formError && (
                <div className="p-3 bg-red-50 border border-red-150 text-red-800 text-sm rounded-lg flex items-center gap-2">
                  <ShieldAlert className="w-4 h-4" />
                  {formError}
                </div>
              )}

              <div>
                <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1">Target Endpoint URL</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. https://github.com or https://jsonplaceholder.typicode.com/posts/1"
                  className="w-full border border-gray-200 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                  value={form.url}
                  onChange={handleUrlChange}
                />
                <p className="text-[10px] text-gray-400 mt-1">
                  A good health check endpoint responds quickly, does not require authentication, and reports internal subsystem health.
                </p>
              </div>

              {/* URL telemetry analysis card */}
              {form.url && (
                <div className="bg-slate-50 border border-slate-150 p-4 rounded-xl space-y-2 animate-fade-in">
                  <h4 className="text-[10px] font-bold text-slate-450 uppercase tracking-wider">URL telemetry analysis</h4>
                  <div className="space-y-1 text-xs">
                    {(() => {
                      const analysis = analyzeUrl(form.url);
                      if (!analysis) return null;
                      return (
                        <>
                          <div className="flex items-center gap-1.5 text-slate-700">
                            {analysis.hasProtocol ? (
                              <span className="text-emerald-700 font-semibold flex items-center gap-1">
                                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
                                {analysis.isHttps ? 'HTTPS detected' : 'HTTP detected (unencrypted)'}
                              </span>
                            ) : (
                              <span className="text-amber-700 font-semibold flex items-center gap-1">
                                <AlertTriangle className="w-3.5 h-3.5 text-amber-500" />
                                Missing protocol prefix
                              </span>
                            )}
                          </div>
                          
                          <div className="flex items-center justify-between gap-1.5 text-slate-600">
                            <span>Suggested Name: <strong className="text-slate-800">{analysis.suggestedName}</strong></span>
                            {analysis.suggestedName && form.name !== analysis.suggestedName && (
                              <button 
                                type="button"
                                onClick={() => setForm(f => ({ ...f, name: analysis.suggestedName }))}
                                className="text-[10px] text-blue-600 font-bold hover:underline"
                              >
                                Use Suggestion
                              </button>
                            )}
                          </div>
                        </>
                      );
                    })()}
                  </div>
                </div>
              )}

              <div>
                <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1">Friendly Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Core API Service"
                  className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                  value={form.name}
                  onChange={e => setForm({ ...form, name: e.target.value })}
                />
              </div>

              {/* Collapsible Advanced Settings (Collapsed by default) */}
              <div className="pt-2">
                <button
                  type="button"
                  onClick={() => setModalAdvancedExpanded(!modalAdvancedExpanded)}
                  className="w-full text-center py-2 bg-gray-50 hover:bg-gray-100 rounded-lg text-xs font-semibold text-gray-700 border border-gray-200 flex items-center justify-center gap-1.5 transition-colors"
                >
                  {modalAdvancedExpanded ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                  <span>{modalAdvancedExpanded ? 'Hide Advanced Settings' : 'Show Advanced Settings'}</span>
                </button>
              </div>

              {modalAdvancedExpanded && (
                <div className="space-y-4 pt-3 border-t border-gray-150 animate-fade-in">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1">Monitor Type</label>
                      <select
                        className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                        value={form.monitorType}
                        onChange={e => setForm({ ...form, monitorType: e.target.value })}
                      >
                        {MonitorTypes.map(t => (
                          <option key={t} value={t} disabled={t !== 'HTTP'}>
                            {MONITOR_TYPE_LABELS[t] ?? t}{t !== 'HTTP' ? ' (Coming soon)' : ''}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1">HTTP Method</label>
                      <select
                        className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                        value={form.method}
                        onChange={e => setForm({ ...form, method: e.target.value })}
                        disabled={form.monitorType !== 'HTTP'}
                      >
                        {HttpMethods.map(m => <option key={m} value={m}>{m}</option>)}
                      </select>
                      <p className="text-[10px] text-gray-400 mt-1">GET is recommended for health endpoints.</p>
                    </div>
                  </div>

                  <div className="grid grid-cols-3 gap-3">
                    <div>
                      <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1" title="Interval in seconds">Interval (s)</label>
                      <input
                        type="number"
                        min="30"
                        className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                        value={form.intervalSeconds}
                        onChange={e => setForm({ ...form, intervalSeconds: Number(e.target.value) })}
                      />
                      <p className="text-[9px] text-gray-400 mt-1">How often PulseWatch checks the endpoint.</p>
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1" title="Timeout in milliseconds">Timeout (ms)</label>
                      <input
                        type="number"
                        min="1000"
                        className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                        value={form.timeoutMs}
                        onChange={e => setForm({ ...form, timeoutMs: Number(e.target.value) })}
                      />
                      <p className="text-[9px] text-gray-400 mt-1">Max wait time before consider failed.</p>
                    </div>
                    <div>
                      <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider mb-1" title="Expected Status Code">Expected Code</label>
                      <input
                        type="number"
                        className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                        value={form.expectedStatus}
                        onChange={e => setForm({ ...form, expectedStatus: Number(e.target.value) })}
                        disabled={form.monitorType !== 'HTTP'}
                      />
                      <p className="text-[9px] text-gray-400 mt-1">The HTTP status code considered healthy.</p>
                    </div>
                  </div>
                </div>
              )}

              {/* Dynamic live validation warnings */}
              {getValidationWarnings().map((w, idx) => (
                <div key={idx} className="p-2.5 bg-amber-50 border border-amber-200 text-amber-900 rounded-lg text-xs flex items-center gap-2 animate-fade-in">
                  <AlertTriangle className="w-4 h-4 flex-shrink-0 text-amber-600" />
                  <span>{w}</span>
                </div>
              ))}

              <div className="flex gap-3 pt-4 border-t border-gray-100 justify-end">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 border border-gray-200 rounded-lg text-sm text-gray-700 hover:bg-gray-50 font-medium"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={formLoading}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700 font-medium disabled:opacity-50 flex items-center gap-2"
                >
                  {formLoading ? 'Saving...' : isEditing ? 'Save Changes' : 'Create Monitor'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Monitor Wizard (CREATE flow) */}
      {showWizard && (
        <MonitorWizard
          initialUrl={wizardInitialUrl}
          initialName={wizardInitialName}
          onClose={() => setShowWizard(false)}
          onSuccess={handleWizardSuccess}
        />
      )}

      {/* Confirm delete dialog */}
      <ConfirmDialog
        isOpen={confirmDialog.open}
        title="Delete Monitor"
        message="This monitor and all its check history, assertions, and alert rules will be permanently deleted. This action cannot be undone."
        confirmText="Delete Monitor"
        onConfirm={handleDeleteConfirm}
        onCancel={() => setConfirmDialog({ open: false, monitorId: null })}
      />
    </div>
  );
};

export default MonitorsPage;
