"""Read back Modrinth metadata and compare its file hashes to the uploaded JAR."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import urllib.request
from urllib.parse import quote


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project', required=True)
    parser.add_argument('--version', required=True)
    parser.add_argument('--file', type=Path, required=True)
    parser.add_argument('--pin-dependencies', action='store_true', help='Pin reviewed dependency files after MC-Publish creates project-level dependencies')
    args = parser.parse_args()
    headers = {'User-Agent': 'ArcaneKinetics-release-verification/0.3.0'}
    if os.environ.get('MODRINTH_TOKEN'):
        headers['Authorization'] = os.environ['MODRINTH_TOKEN']
    url = 'https://api.modrinth.com/v2/project/' + quote(args.project, safe='') + '/version/' + quote(args.version, safe='')
    with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=60) as response:
        version = json.load(response)
    data = args.file.read_bytes()
    matching = [f for f in version['files'] if f['filename'] == args.file.name and f['hashes']['sha512'] == hashlib.sha512(data).hexdigest() and f['hashes']['sha1'] == hashlib.sha1(data).hexdigest()]
    assert len(matching) == 1, 'Modrinth file hash mismatch'
    assert version['game_versions'] == ['1.21.1'] and version['loaders'] == ['neoforge'], 'Modrinth target mismatch'
    assert version['version_type'] == 'beta', 'Modrinth release type mismatch'
    project = json.loads((Path(__file__).resolve().parents[1] / 'release/project.json').read_text(encoding='utf-8'))
    actual = {(d['project_id'], d.get('version_id'), d['dependency_type']) for d in version['dependencies']}
    expected = {(d['project_id'], d['version_id'], d['type']) for d in project['dependencies']}
    if args.pin_dependencies and actual != expected:
        assert os.environ.get('MODRINTH_TOKEN'), 'Missing Modrinth token'
        # MC-Publish v3.3 resolves projects, but does not preserve version pins.
        dependencies = [{'project_id': d['project_id'], 'version_id': d['version_id'],
                         'dependency_type': d['type']} for d in project['dependencies']]
        request = urllib.request.Request('https://api.modrinth.com/v2/version/' + quote(version['id'], safe=''),
            data=json.dumps({'dependencies': dependencies}).encode('utf-8'),
            headers=dict(headers, **{'Content-Type': 'application/json'}), method='PATCH')
        with urllib.request.urlopen(request, timeout=60) as response:
            response.read()
        with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=60) as response:
            version = json.load(response)
        actual = {(d['project_id'], d.get('version_id'), d['dependency_type']) for d in version['dependencies']}
    assert expected <= actual, 'Modrinth dependency versions or requirement types differ from reviewed metadata'
    print('PASS: Modrinth hashes and beta metadata match the GitHub release JAR.')


if __name__ == '__main__':
    main()
