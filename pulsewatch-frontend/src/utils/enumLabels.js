// src/utils/enumLabels.js
// Central mapping of backend enum values to human-friendly UI labels.
// Import and use these instead of rendering raw enum strings anywhere in the UI.

/** Alert rule trigger event types */
export const RULE_TYPE_LABELS = {
  INCIDENT_OPENED:  'When a service goes down',
  INCIDENT_RESOLVED: 'When a service recovers',
};

/** Alert history channel/type labels */
export const ALERT_TYPE_LABELS = {
  EMAIL:             'Email',
  WEBHOOK:           'Webhook',
  SLACK:             'Slack',
  INCIDENT_OPENED:   'Downtime Alert',
  INCIDENT_RESOLVED: 'Recovery Alert',
};

/** Assertion types shown in AssertionsPanel and dropdowns */
export const ASSERT_TYPE_LABELS = {
  STATUS_CODE:       'Response code equals',
  BODY_CONTAINS:     'Response body contains',
  BODY_NOT_CONTAINS: 'Response body does not contain',
};

/** Assertion operators -- used in verbose descriptions */
export const OPERATOR_LABELS = {
  EQ:           'equals',
  CONTAINS:     'contains',
  NOT_CONTAINS: 'does not contain',
};

/** Monitor status labels */
export const MONITOR_STATUS_LABELS = {
  UP:          'Operational',
  DOWN:        'Down',
  MAINTENANCE: 'In Maintenance',
  UNKNOWN:     'Unknown',
  PENDING:     'Pending first check',
};

/** Monitor type labels (human-readable, no backend enum jargon) */
export const MONITOR_TYPE_LABELS = {
  HTTP:     'Website / API (HTTP)',
  TCP:      'Port Check (TCP)',
  PING:     'Ping',
  DATABASE: 'Database',
  SSL:      'SSL Certificate',
};

/** Incident event type labels for the activity timeline */
export const INCIDENT_EVENT_LABELS = {
  INCIDENT_OPENED:       'Incident detected',
  SEVERITY_ESCALATED:    'Severity increased',
  INCIDENT_RESOLVED:     'Incident resolved',
  INCIDENT_ACKNOWLEDGED: 'Incident acknowledged',
  ACKNOWLEDGED:          'Incident acknowledged',
};

/** Incident status labels */
export const INCIDENT_STATUS_LABELS = {
  OPEN:         'Open',
  ACKNOWLEDGED: 'Acknowledged',
  RESOLVED:     'Resolved',
};

/** Incident severity labels */
export const SEVERITY_LABELS = {
  CRITICAL: 'Critical',
  HIGH:     'High',
  MEDIUM:   'Medium',
  LOW:      'Low',
};

/**
 * Human-readable interval display (seconds to friendly string).
 * e.g. 30 -> 'Every 30 seconds', 60 -> 'Every minute', 300 -> 'Every 5 minutes'
 */
export function formatInterval(seconds) {
  if (!seconds) return '--';
  if (seconds < 60) return `Every ${seconds} seconds`;
  const mins = Math.floor(seconds / 60);
  if (mins === 1) return 'Every minute';
  if (mins < 60) return `Every ${mins} minutes`;
  const hrs = Math.floor(mins / 60);
  if (hrs === 1) return 'Every hour';
  return `Every ${hrs} hours`;
}

/**
 * Resolve any enum value to a human-friendly label.
 * Falls back to a title-cased version of the raw value if no mapping found.
 * @param {string} value - Raw backend enum string
 * @param {object} map   - One of the mapping objects above
 * @returns {string}
 */
export function labelFor(value, map) {
  if (!value) return '--';
  return map[value] ?? value
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

/**
 * Format incident/downtime duration in seconds to a readable string.
 * e.g. 45 -> '45s', 125 -> '2m 5s', 3661 -> '1h 1m'
 */
export function formatDuration(seconds) {
  if (!seconds || seconds === 0) return '—';
  if (seconds < 60) return `${seconds}s`;
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  if (m < 60) return s > 0 ? `${m}m ${s}s` : `${m}m`;
  const h = Math.floor(m / 60);
  const rem = m % 60;
  return rem > 0 ? `${h}h ${rem}m` : `${h}h`;
}
