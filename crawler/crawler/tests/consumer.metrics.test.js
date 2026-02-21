const test = require('node:test');
const assert = require('node:assert/strict');

const { metrics, asPrometheusMetrics } = require('../consumer');

test('asPrometheusMetrics expõe contadores do consumer', () => {
  metrics.received = 9;
  metrics.imported = 7;
  metrics.retried = 1;
  metrics.sentToDlq = 1;
  metrics.duplicates = 2;
  metrics.invalid = 3;
  metrics.processingErrors = 4;
  metrics.receivedBySource = { 'acme-lever::lever': 8 };
  metrics.importedBySource = { 'acme-lever::lever': 6 };
  metrics.retriedBySource = { 'acme-lever::lever': 2 };
  metrics.dlqBySource = { 'acme-lever::lever': 1 };
  metrics.health = 'healthy';

  const content = asPrometheusMetrics();

  assert.match(content, /hermes_consumer_received_total 9/);
  assert.match(content, /hermes_consumer_imported_total 7/);
  assert.match(content, /hermes_consumer_retried_total 1/);
  assert.match(content, /hermes_consumer_dlq_total 1/);
  assert.match(content, /hermes_consumer_duplicates_total 2/);
  assert.match(content, /hermes_consumer_invalid_total 3/);
  assert.match(content, /hermes_consumer_processing_errors_total 4/);
  assert.match(content, /hermes_consumer_up 1/);
  assert.match(content, /hermes_consumer_received_by_source_total\{source="acme-lever",source_type="lever"\} 8/);
  assert.match(content, /hermes_consumer_imported_by_source_total\{source="acme-lever",source_type="lever"\} 6/);
  assert.match(content, /hermes_consumer_retried_by_source_total\{source="acme-lever",source_type="lever"\} 2/);
  assert.match(content, /hermes_consumer_dlq_by_source_total\{source="acme-lever",source_type="lever"\} 1/);
});
