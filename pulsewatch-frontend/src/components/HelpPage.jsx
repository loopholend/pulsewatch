import React from 'react';
import { HelpCircle, Activity, Bell, Shield, Globe } from 'lucide-react';

const HelpPage = () => {
    return (
        <div className="min-h-screen bg-gray-50 p-6 md:p-8">
            <div className="max-w-4xl mx-auto space-y-8">
                {/* Header */}
                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 flex items-center gap-3">
                    <HelpCircle className="w-8 h-8 text-blue-600" />
                    <div>
                        <h1 className="text-2xl font-bold text-gray-900">Help & Documentation</h1>
                        <p className="text-gray-500 mt-1">Learn how PulseWatch pings, monitors, and alerts you about service downtime.</p>
                    </div>
                </div>

                {/* Content Cards */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 space-y-3">
                        <Activity className="w-6 h-6 text-blue-600" />
                        <h2 className="text-lg font-semibold text-gray-900">Uptime & Latency Monitoring</h2>
                        <p className="text-sm text-gray-600 leading-relaxed">
                            PulseWatch operates a background scheduler that issues periodic HTTP or TCP pings to your endpoints. 
                            You can customize intervals down to 30 seconds and set tight timeouts to detect latency spikes before they affect your users.
                        </p>
                    </div>

                    <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 space-y-3">
                        <Shield className="w-6 h-6 text-emerald-600" />
                        <h2 className="text-lg font-semibold text-gray-900">Response Assertions</h2>
                        <p className="text-sm text-gray-600 leading-relaxed">
                            Beyond standard HTTP status checks, you can add custom assertions. Check if response bodies contain expected words, 
                            or assert that error messages are absent. All active assertions must evaluate to true for a monitor to be marked <strong>UP</strong>.
                        </p>
                    </div>

                    <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 space-y-3">
                        <Bell className="w-6 h-6 text-red-600" />
                        <h2 className="text-lg font-semibold text-gray-900">Incidents & Cooldown Alerts</h2>
                        <p className="text-sm text-gray-600 leading-relaxed">
                            When a monitor exceeds your failure threshold, an Incident is declared. PulseWatch sends a notification email immediately 
                            and enters a cooldown state. You won't be spammed with redundant emails until the cooldown timer expires.
                        </p>
                    </div>

                    <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 space-y-3">
                        <Globe className="w-6 h-6 text-purple-600" />
                        <h2 className="text-lg font-semibold text-gray-900">Public Status Pages</h2>
                        <p className="text-sm text-gray-600 leading-relaxed">
                            Publish service health to your stakeholders. Status pages are publicly accessible without authentication. 
                            Group monitors together, customize slugs, and show real-time service availability metrics.
                        </p>
                    </div>
                </div>

                {/* Workflow Guide */}
                <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100">
                    <h2 className="text-lg font-semibold text-gray-900 mb-4">Quick Start Workflow</h2>
                    <ol className="relative border-l border-gray-200 ml-3 space-y-6">
                        <li className="mb-4 ml-6">
                            <span className="absolute flex items-center justify-center w-6 h-6 bg-blue-100 rounded-full -left-3 ring-8 ring-white">
                                <span className="text-xs font-semibold text-blue-600">1</span>
                            </span>
                            <h3 className="font-semibold text-gray-900 text-sm">Create a Monitor</h3>
                            <p className="text-xs text-gray-500 mt-1">Head over to the Monitors panel and click "New Monitor". Set your URL, interval, and expected status code.</p>
                        </li>
                        <li className="mb-4 ml-6">
                            <span className="absolute flex items-center justify-center w-6 h-6 bg-blue-100 rounded-full -left-3 ring-8 ring-white">
                                <span className="text-xs font-semibold text-blue-600">2</span>
                            </span>
                            <h3 className="font-semibold text-gray-900 text-sm">Add Alert Rules</h3>
                            <p className="text-xs text-gray-500 mt-1">Go to Alert Rules and configure who should be emailed when a monitor fails (Downtime Detected) or recovers (Service Recovered).</p>
                        </li>
                        <li className="ml-6">
                            <span className="absolute flex items-center justify-center w-6 h-6 bg-blue-100 rounded-full -left-3 ring-8 ring-white">
                                <span className="text-xs font-semibold text-blue-600">3</span>
                            </span>
                            <h3 className="font-semibold text-gray-900 text-sm">Setup Status Page</h3>
                            <p className="text-xs text-gray-500 mt-1">Publish system status by creating a custom status page slug and mapping your active monitors to it.</p>
                        </li>
                    </ol>
                </div>
            </div>
        </div>
    );
};

export default HelpPage;
