#!/usr/bin/env python3
"""Run the production app's explicit Debug bridge checks on a disposable simulator."""
import argparse
import pathlib
import selectors
import subprocess
import time


REQUIRED_CHECKS = (
    'UI-free bootstrap, versioned codec and identity-only missing-occurrence result',
    'typed edits and Swift Observation',
    'production editor owner survives compact-expanded-compact layout',
    'production detail and nested destination replacement retain drafts',
    'failed readiness preserves durable ordered deliveries',
    'native editor subpages and staged sound survive presentation replacement',
    'native validation retry duplicate save result acknowledgement and list undo',
    'typed validation result without persistence',
    'native suspend cancellation leaves later authoritative result retained',
    'native Flow cancellation leaves owner and state alive',
    'mounted StateViewModel cleanup leaves durable queue unchanged',
    'production session-end and window cleanup preserve unresolved delivery',
    'native permission denial keeps unsaved draft without retry; grant saves exact draft and occurrences; late response cannot save replacement',
    'native first Save requests authorization once; Settings grant resumes exact draft once; denial, failed open and replacement never save; delivery defers resumption',
    'native permission tutorial bundles nine silent loops; audio lease rejects competing owners and stale release; delivery stops guide',
    'native maths preview validation progress cancellation and retained route',
    'native answer focus keyboard placeholder and UITextField insertion update shared raw answer',
    'native preview accepted completion returns to identical unsaved draft and nested route',
    'real accepted completion returns to identical retained draft nested route staged sound and permission guard',
    'native readiness before exact acknowledgement and ordered delivery interruption',
    'acknowledged unresolved restoration preserves exact challenge progress',
    'native accepted completion snooze retry duplicates and result acknowledgement',
    'process restart after acknowledgement restores exact durable progress',
    'native settings all themes/sorts use persisted keys while two drafts, nested route, staged sound and permission guard retain owners',
    'native announcement IDs/page survive interruption without acknowledgement; explicit browse/Got it persist and latest batch reopens',
    'production settings share presents in the tapped active scene with iPad anchor, cancellation/error callbacks; unavailable feedback handler reports failure using injected mailto opener',
    'fresh process restores theme/sort and announcement acknowledgement while latest batch remains reopenable',
)


def missing_checks(captured: bytes) -> list[str]:
    lines = captured.decode('utf-8', errors='replace').splitlines()
    return [check for check in REQUIRED_CHECKS if f'BRIDGE PASS {check}' not in lines]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--udid', required=True, help='Disposable simulator UDID (never a physical device)')
    parser.add_argument('--app', required=True, type=pathlib.Path, help='Built Debug MathAlarm.app')
    parser.add_argument('--output', type=pathlib.Path, default=pathlib.Path('build/ios-shared-bridge.log'))
    args = parser.parse_args()
    subprocess.run(['xcrun', 'simctl', 'install', args.udid, str(args.app)], check=True)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    captured = b''
    with args.output.open('wb') as log:
        for phase in (['--verify-m6-settings-fresh'], ['--verify-m5-restoration']):
            command = ['xcrun', 'simctl', 'launch', '--console', '--terminate-running-process', args.udid,
                       'com.timilehinaregbesola.mathalarm', '--verify-shared-bridge', *phase]
            process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            selector = selectors.DefaultSelector()
            selector.register(process.stdout, selectors.EVENT_READ)
            passed = False
            phase_output = b''
            deadline = time.monotonic() + 160
            try:
                while time.monotonic() < deadline and process.poll() is None:
                    for key, _ in selector.select(timeout=1):
                        chunk = key.fileobj.read1(65536)
                        if not chunk:
                            continue
                        log.write(chunk)
                        log.flush()
                        captured += chunk
                        phase_output += chunk
                        if b'SHARED_BRIDGE_VERIFICATION_PASSED' in phase_output:
                            passed = True
                            break
                    if passed:
                        break
            finally:
                selector.close()
                # A separate application process must read persisted progress, never the
                # coordinator's retained in-memory session from the first launch.
                subprocess.run(['xcrun', 'simctl', 'terminate', args.udid,
                                'com.timilehinaregbesola.mathalarm'], check=False, capture_output=True)
                if process.poll() is None:
                    process.terminate()
                process.wait(timeout=5)
            if not passed:
                raise SystemExit(f'Production bridge verification failed in {phase}; inspect {args.output}')
    missing = missing_checks(captured)
    if missing:
        raise SystemExit(f'Production bridge checks missing {missing}; inspect {args.output}')
    print(f'Production bridge verification passed ({len(REQUIRED_CHECKS)} check groups); log: {args.output}')


if __name__ == '__main__':
    main()
