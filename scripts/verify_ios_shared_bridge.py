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
    'production challenge placeholder preserves durable ordered deliveries',
    'native editor subpages and staged sound survive presentation replacement',
    'native validation retry duplicate save result acknowledgement and list undo',
    'typed validation result without persistence',
    'native suspend cancellation leaves later authoritative result retained',
    'native Flow cancellation leaves owner and state alive',
    'mounted StateViewModel cleanup leaves durable queue unchanged',
    'production session-end and window cleanup preserve unresolved delivery',
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
    command = ['xcrun', 'simctl', 'launch', '--console', '--terminate-running-process', args.udid,
               'com.timilehinaregbesola.mathalarm', '--verify-shared-bridge']
    process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    selector = selectors.DefaultSelector()
    selector.register(process.stdout, selectors.EVENT_READ)
    passed = False
    captured = b''
    deadline = time.monotonic() + 60
    try:
        with args.output.open('wb') as log:
            while time.monotonic() < deadline and process.poll() is None:
                for key, _ in selector.select(timeout=1):
                    chunk = key.fileobj.read1(65536)
                    if not chunk:
                        continue
                    log.write(chunk)
                    log.flush()
                    captured += chunk
                    if b'SHARED_BRIDGE_VERIFICATION_PASSED' in captured:
                        passed = True
                        break
                if passed:
                    break
    finally:
        selector.close()
        subprocess.run(['xcrun', 'simctl', 'terminate', args.udid,
                        'com.timilehinaregbesola.mathalarm'], check=False, capture_output=True)
        if process.poll() is None:
            process.terminate()
        process.wait(timeout=5)
    if not passed:
        raise SystemExit(f'Production bridge verification failed; inspect {args.output}')
    missing = missing_checks(captured)
    if missing:
        raise SystemExit(f'Production bridge checks missing {missing}; inspect {args.output}')
    print(f'Production bridge verification passed ({len(REQUIRED_CHECKS)} check groups); log: {args.output}')


if __name__ == '__main__':
    main()
