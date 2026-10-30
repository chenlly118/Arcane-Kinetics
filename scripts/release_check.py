"""Check the curated source tree and release JAR without printing secret values."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SECRET_RULES = {
    'GitHub credential': re.compile(r'(?:github_pat_|gh[pousr]_)[A-Za-z0-9_]{20,}'),
    'Modrinth credential': re.compile(r'mrp_[A-Za-z0-9]{20,}'),
    'AWS credential': re.compile(r'AKIA[0-9A-Z]{16}'),
    'private key': re.compile(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'),
    'literal credential': re.compile(r'(?i)\b(?:modrinth_token|github_token|access_token|password)\s*[:=]\s*[\"\']?[A-Za-z0-9_.-]{24,}'),
}
PRIVATE_PARTS = {'.cache', '.g', '.gradle', 'backups', 'build', 'dist', 'run', 'logs', 'tmp', 'art', 'integrations', 'saves', 'world', '__pycache__'}


def check(jar=None, publish=False):
    project = json.loads((ROOT / 'release/project.json').read_text(encoding='utf-8'))
    approvals = json.loads((ROOT / 'release/approvals.json').read_text(encoding='utf-8'))
    version = re.search(r'^mod_version=(.+)$', (ROOT / 'gradle.properties').read_text(), re.M)[1].strip()
    errors = []
    def require(ok, message):
        if not ok:
            errors.append(message)
    require(project['version'] == version == approvals['version'], 'Version differs between metadata and approvals')
    require(project['tag'] == 'v' + version, 'Tag differs from version')
    require(project['mod_id'] == 'create_spellbook', 'Unexpected Mod ID')
    result = subprocess.run(['git', 'ls-files', '-z', '--cached', '--others', '--exclude-standard'], cwd=ROOT, capture_output=True, check=True)
    names = sorted(set(result.stdout.decode('utf-8').rstrip('\0').split('\0')) - {''})
    require(bool(names), 'No public source files found')
    for name in names:
        path = ROOT / name
        require(not (set(Path(name).parts) & PRIVATE_PARTS), f'Private path: {name}')
        require(path.suffix.lower() not in {'.key', '.pem', '.pfx', '.p12', '.jks', '.keystore', '.args', '.log', '.mca'}, f'Private file type: {name}')
        require(not Path(name).name.startswith('.env'), f'Environment file: {name}')
        require(path.exists(), f'Missing tracked file: {name}')
        if not path.is_file():
            continue
        require(path.stat().st_size < 2_000_000, f'Unexpected large public file: {name}')
        require(not (path.suffix == '.jar' and name != 'gradle/wrapper/gradle-wrapper.jar'), f'Unexpected bundled JAR: {name}')
        try:
            contents = path.read_text(encoding='utf-8')
        except UnicodeError:
            continue
        for rule, pattern in SECRET_RULES.items():
            require(not pattern.search(contents), f'{rule} detected in {name} (value omitted)')
        require(not re.search(r'[A-Za-z]:[\\/]+(?:Users|Desktop)[\\/]', contents), f'Personal absolute path in {name}')
    resources = ROOT / 'src/main/resources'
    metadata = (resources / 'META-INF/neoforge.mods.toml').read_text(encoding='utf-8')
    require('displayName="' + project['name'] + '"' in metadata, 'Mod display name differs from project metadata')
    if project.get('icon'):
        icon = ROOT / project['icon']
        require(icon.is_file(), 'Project icon is missing')
        require('logoFile="' + icon.name + '"' in metadata, 'Mod metadata does not reference the project icon')
        if icon.is_file():
            require(icon.read_bytes().startswith(b'\x89PNG\r\n\x1a\n') and icon.stat().st_size <= 256 * 1024, 'Project icon must be PNG, at most 256 KiB')
    for path in sorted(resources.rglob('*')):
        if path.is_file() and path.suffix in {'.json', '.mcmeta'}:
            try:
                json.loads(path.read_text(encoding='utf-8'))
            except (ValueError, UnicodeError):
                errors.append('Invalid JSON: ' + path.relative_to(ROOT).as_posix())
    en = json.loads((resources / 'assets/create_spellbook/lang/en_us.json').read_text(encoding='utf-8'))
    zh = json.loads((resources / 'assets/create_spellbook/lang/zh_cn.json').read_text(encoding='utf-8'))
    require(set(en) == set(zh), 'English/Chinese translation key mismatch')
    digest = None
    if jar:
        jar = Path(jar).resolve()
        require(jar.name == f'create-spellbook-1.21.1-neoforge-{version}.jar', 'Unexpected production JAR filename')
        with zipfile.ZipFile(jar) as archive:
            entries = archive.namelist()
            require(len(entries) == len(set(entries)), 'Duplicate JAR entries')
            require(archive.testzip() is None, 'JAR CRC failure')
            require(not any('/gametest/' in n or n.endswith(('.jar', '.args', '.log')) for n in entries), 'Test or dependency payload in production JAR')
            require(all(n.endswith('/') or n == 'META-INF/MANIFEST.MF' or n.startswith(('dev/createspellbook/', 'assets/', 'data/')) or n in {'logo.png', 'pack.mcmeta', 'create_spellbook.mixins.json', 'META-INF/neoforge.mods.toml', 'META-INF/LICENSE', 'META-INF/THIRD_PARTY_NOTICES.md'} for n in entries), 'Unexpected production namespace')
            for name in entries:
                if name.endswith('.class'):
                    require(int.from_bytes(archive.read(name)[6:8], 'big') == 65, f'Not Java 21 bytecode: {name}')
            for path in resources.rglob('*'):
                if path.is_file():
                    name = path.relative_to(resources).as_posix()
                    expected = path.read_bytes()
                    if name == 'META-INF/neoforge.mods.toml':
                        expected = expected.replace(b'${version}', version.encode())
                    require(name in entries and archive.read(name) == expected, f'JAR resource differs: {name}')
            for name in ['LICENSE', 'THIRD_PARTY_NOTICES.md']:
                require('META-INF/' + name in entries and archive.read('META-INF/' + name) == (ROOT / name).read_bytes(), f'JAR notice differs: {name}')
            mixins = json.loads(archive.read('create_spellbook.mixins.json'))
            for name in mixins['mixins']:
                require(mixins['package'].replace('.', '/') + '/' + name + '.class' in entries, 'Missing Mixin: ' + name)
        digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    approved = all(approvals.get(key) is True for key in ['license_approved', 'third_party_cleared', 'public_release_approved'])
    if publish:
        require(approved, 'Publication blocked: license, third-party and public release approvals must all be explicit')
    return {'passed': not errors, 'version': version, 'public_source_files': len(names), 'resource_files': sum(p.is_file() for p in resources.rglob('*')), 'production_sha256': digest, 'publication_approved': approved, 'errors': errors}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path)
    parser.add_argument('--publish', action='store_true')
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    result = check(args.jar, args.publish)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, ensure_ascii=True, indent=2))
    raise SystemExit(0 if result['passed'] else 1)


if __name__ == '__main__':
    main()
