const test = require('node:test');
const assert = require('node:assert/strict');

const { metrics, asPrometheusMetrics } = require('../producer');

test('asPrometheusMetrics expõe formato Prometheus com labels por source', () => {
  metrics.runs = 2;
  metrics.published = 10;
  metrics.dropped = 1;
  metrics.sourceFailures = 3;
  metrics.sourceFailuresByName = { 'acme"source': 2 };
  metrics.sourceRunsByName = { 'acme"source': 5 };
  metrics.sourceSuccessByName = { 'acme"source': 4 };
  metrics.sourceJobsCollectedByName = { 'acme"source': 11 };
  metrics.sourceUpByName = { 'acme"source': true };
  metrics.health = 'degraded';

  const content = asPrometheusMetrics();

  assert.match(content, /# TYPE hermes_producer_runs_total counter/);
  assert.match(content, /hermes_producer_runs_total 2/);
  assert.match(content, /hermes_producer_jobs_published_total 10/);
  assert.match(content, /hermes_producer_jobs_dropped_total 1/);
  assert.match(content, /hermes_producer_source_failures_total 3/);
  assert.match(content, /hermes_producer_up 0/);
  assert.match(content, /hermes_producer_source_failures_by_source_total\{source="acme\\"source",source_type="unknown"\} 2/);
  assert.match(content, /hermes_producer_source_runs_total\{source="acme\\"source",source_type="unknown"\} 5/);
  assert.match(content, /hermes_producer_source_success_total\{source="acme\\"source",source_type="unknown"\} 4/);
  assert.match(content, /hermes_producer_source_jobs_collected_total\{source="acme\\"source",source_type="unknown"\} 11/);
  assert.match(content, /hermes_producer_source_up\{source="acme\\"source",source_type="unknown"\} 1/);
});
