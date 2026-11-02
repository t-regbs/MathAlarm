#!/usr/bin/env python3
"""Check the approved native renderer boundaries in production source and framework exports."""
import argparse
import pathlib
import re


RENDERER_IMPORT = re.compile(
    r'^\s*import\s+(?:androidx\.compose(?:\.|$)|org\.jetbrains\.compose(?:\.|$)|'
    r'com\.mohamedrejeb\.calf(?:\.|$)|cafe\.adriel\.lyricist(?:\.|$)|'
    r'androidx\.navigation(?:3)?(?:\.|$))', re.MULTILINE)
COMMON_NATIVE_IMPORT = re.compile(
    r'^\s*import\s+(?:platform\.(?:UIKit|SwiftUI)(?:\.|$)|android\.(?:content|view|widget)(?:\.|$))',
    re.MULTILINE)
GRADLE_RENDERER = re.compile(
    r'\blibs\.(?:compose(?:\.|\b)|calf(?:\.|\b)|lyricist(?:\.|\b)|'
    r'navigation3(?:\.|\b)|plugins\.compose(?:Multiplatform|\.|\b)|'
    r'(?:runtime|foundation|material3|ui|haze|compottie)(?:\.|\b)|components\.resources|'
    r'androidx\.(?:compose|activity\.compose)(?:\.|\b)|'
    r'jetbrains\.(?:navigation3|lifecycle\.viewmodel\.navigation3)(?:\.|\b)|'
    r'koin\.compose(?:\.|\b))|'
    r'\bswiftExport\s*\{|org\.jetbrains\.compose')
IOS_COMPOSE = re.compile(r'\b(?:MainViewControllerKt|ComposeUIViewController|ComposeView)\b')
HEADER_RENDERER = re.compile(r'\b\w*(?:ComposeUIViewController|MainViewController|Calf|CALF|ComposeRuntime|ComposeUi)\w*')
HEADER_INFRASTRUCTURE = re.compile(
    r'\bApp(?:Koin\w*|Usecases|AlarmEntity|\w*(?:Repository|DataSource|Database|Dao|'
    r'ProgressStore|ReviewEligibilityStore)\w*)\b')
RESOLVED_RENDERER = re.compile(
    r'(?:org\.jetbrains\.compose(?:\.[\w.]+)?|androidx\.compose(?:\.[\w.]+)?|'
    r'com\.mohamedrejeb\.calf|cafe\.adriel\.lyricist):|'
    r'io\.insert-koin:koin-compose|(?:org\.jetbrains\.androidx|androidx)\.[\w.]+:[\w-]*compose')


def violations(root: pathlib.Path, framework: pathlib.Path | None = None,
               dependencies: pathlib.Path | None = None) -> list[str]:
    failures = []
    for module in ('core', 'shared'):
        sources = root / module / 'src'
        for source in sorted(sources.rglob('*.kt')):
            # Native adapters may use SDK services; common code cannot use a native UI SDK.
            text = source.read_text()
            for pattern in (RENDERER_IMPORT, COMMON_NATIVE_IMPORT if 'commonMain' in source.parts else None):
                if pattern is None:
                    continue
                for match in pattern.finditer(text):
                    line = text[:match.start()].count('\n') + 1
                    failures.append(f'{source.relative_to(root)}:{line}: renderer/native UI import')
        for resources in sources.rglob('composeResources'):
            if any(item.is_file() for item in resources.rglob('*')):
                failures.append(f'{resources.relative_to(root)}: shared renderer resources')
        build = root / module / 'build.gradle.kts'
        if build.exists():
            # Ignore explanatory comments, which may name the deleted compatibility DSL.
            code = '\n'.join(line.split('//', 1)[0] for line in build.read_text().splitlines())
            if GRADLE_RENDERER.search(code):
                failures.append(f'{build.relative_to(root)}: renderer dependency/plugin or Swift export DSL')
    for source in sorted((root / 'iosApp' / 'iosApp').rglob('*.swift')):
        code = '\n'.join(line.split('//', 1)[0] for line in source.read_text().splitlines())
        if IOS_COMPOSE.search(code):
            failures.append(f'{source.relative_to(root)}: compatibility Compose entry point')
    if framework is not None:
        header = framework / 'Headers' / 'app.h'
        if not header.is_file():
            failures.append(f'{header}: generated app framework header missing')
        else:
            text = header.read_text()
            if HEADER_RENDERER.search(text):
                failures.append(f'{header}: renderer exported from production framework')
            exported_infrastructure = sorted(set(HEADER_INFRASTRUCTURE.findall(text)))
            if exported_infrastructure:
                failures.append(f'{header}: infrastructure exported: {", ".join(exported_infrastructure)}')
            for api in ('IosApplication', 'SharedFeatures', 'AlarmListViewModel', 'AlarmSettingsViewModel'):
                if api not in text:
                    failures.append(f'{header}: required native API {api} missing')
    if dependencies is not None:
        for number, line in enumerate(dependencies.read_text().splitlines(), 1):
            if RESOLVED_RENDERER.search(line):
                failures.append(f'{dependencies}:{number}: resolved shared renderer dependency: {line.strip()}')
    return failures


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=pathlib.Path, default=pathlib.Path(__file__).resolve().parents[1])
    parser.add_argument('--framework', type=pathlib.Path, help='Freshly built app.framework (checks app.h)')
    parser.add_argument('--dependencies', type=pathlib.Path, help='Gradle shared dependency report for a production configuration')
    args = parser.parse_args()
    failures = violations(args.root.resolve(), args.framework, args.dependencies)
    if failures:
        raise SystemExit('\n'.join(failures))
    print('Native UI boundaries passed: shared/core renderer-free; iOS uses native presentation' +
          ('; production framework exports checked' if args.framework else '') +
          ('; resolved shared dependencies checked' if args.dependencies else ''))


if __name__ == '__main__':
    main()
