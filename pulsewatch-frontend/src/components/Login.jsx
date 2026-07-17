import React, { useState } from 'react';
import { api } from '../api/axiosConfig';
import { Lock, Mail, Activity, AlertCircle, ArrowRight, CheckCircle2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const Login = ({ onLoginSuccess }) => {
    const navigate = useNavigate();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [loading, setLoading] = useState(false);
    const [isRegistering, setIsRegistering] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');
        setLoading(true);
        try {
            if (isRegistering) {
                await api.post('/auth/register', { email, password, role: 'USER' });
                // Auto-switch to login mode and show a proper success message
                setIsRegistering(false);
                setSuccess('Account created! You can now sign in.');
                setPassword('');
            } else {
                const res = await api.post('/auth/login', { email, password });
                const token = res.data.token || res.data.accessToken;
                if (token) {
                    localStorage.setItem('userEmail', email);
                    onLoginSuccess(token); // AuthContext stores token + updates state
                    navigate('/');
                } else {
                    setError('Invalid token received from server.');
                }
            }
        } catch (err) {
            setError(err.response?.data?.message || 'Authentication failed. Please check your credentials.');
        } finally {
            setLoading(false);
        }
    };

    const handleToggleMode = () => {
        setIsRegistering(!isRegistering);
        setError('');
        setSuccess('');
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 text-white flex flex-col justify-center py-12 sm:px-6 lg:px-8">
            <div className="sm:mx-auto sm:w-full sm:max-w-md text-center space-y-4">
                <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-500/10 border border-blue-500/20 text-blue-400 text-xs font-semibold uppercase tracking-wider mx-auto">
                    <Activity className="w-4 h-4 animate-pulse" />
                    PulseWatch Observability
                </div>
                <h2 className="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
                    {isRegistering ? 'Create your developer account' : 'Sign in to PulseWatch'}
                </h2>
                <p className="text-sm text-slate-400">
                    {isRegistering
                        ? 'Get started with real-time API uptime & telemetry monitoring.'
                        : 'Access your monitoring dashboards and alert channels.'}
                </p>
            </div>

            <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
                <div className="bg-white/5 backdrop-blur-md py-8 px-4 shadow-xl sm:rounded-2xl sm:px-10 border border-white/10">
                    <form className="space-y-5" onSubmit={handleSubmit}>
                        {/* Error banner */}
                        {error && (
                            <div className="p-3 text-xs rounded-lg flex items-center gap-2 border bg-red-500/10 text-red-400 border-red-500/20">
                                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                                {error}
                            </div>
                        )}

                        {/* Success banner */}
                        {success && (
                            <div className="p-3 text-xs rounded-lg flex items-center gap-2 border bg-emerald-500/10 text-emerald-400 border-emerald-500/20">
                                <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
                                {success}
                            </div>
                        )}

                        <div>
                            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                                Email Address
                            </label>
                            <div className="relative rounded-lg shadow-sm">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Mail className="h-4 w-4 text-slate-500" />
                                </div>
                                <input
                                    type="email"
                                    required
                                    value={email}
                                    onChange={e => setEmail(e.target.value)}
                                    className="block w-full pl-10 pr-3 py-2.5 text-sm bg-slate-950/40 border border-white/15 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all"
                                    placeholder="developer@company.com"
                                />
                            </div>
                        </div>

                        <div>
                            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1.5">
                                Password
                            </label>
                            <div className="relative rounded-lg shadow-sm">
                                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                    <Lock className="h-4 w-4 text-slate-500" />
                                </div>
                                <input
                                    type="password"
                                    required
                                    value={password}
                                    onChange={e => setPassword(e.target.value)}
                                    className="block w-full pl-10 pr-3 py-2.5 text-sm bg-slate-950/40 border border-white/15 rounded-lg text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all"
                                    placeholder="••••••••"
                                />
                            </div>
                        </div>

                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full flex justify-center items-center gap-2 py-2.5 px-4 border border-transparent rounded-lg shadow-lg text-sm font-semibold text-white bg-blue-600 hover:bg-blue-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 transition-all"
                        >
                            {loading ? 'Processing...' : isRegistering ? 'Create Account' : 'Sign In'}
                            {!loading && <ArrowRight className="w-4 h-4" />}
                        </button>
                    </form>

                    <div className="mt-6 text-center border-t border-white/10 pt-5">
                        <button
                            onClick={handleToggleMode}
                            className="text-sm text-blue-400 hover:text-blue-300 font-medium transition-all"
                        >
                            {isRegistering
                                ? 'Already have an account? Sign in'
                                : "Don't have an account? Register for free"}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Login;
