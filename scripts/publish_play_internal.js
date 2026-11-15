#!/usr/bin/env node
// Compatibility entry point. This command always targets Internal testing.
// Production releases use play_release.js prepare + explicit commit instead.
const { spawnSync } = require('node:child_process');
const state = process.env.SUBTY_PLAY_STATUS_FILE || 'artifacts/play-internal-edit.json';
for (const action of ['prepare', 'commit']) {
  const result = spawnSync(process.execPath, [require.resolve('./play_release.js'), action, state], {
    stdio: 'inherit',
    env: { ...process.env, SUBTY_RELEASE_TRACK: 'internal' },
  });
  if (result.status !== 0) process.exit(result.status || 1);
}
