"""Resolve a project ID/Slug and synchronize the author-approved Modrinth page."""
from pathlib import Path
import argparse
import json
import os
import urllib.error
import urllib.request
from urllib.parse import quote

ROOT = Path(__file__).resolve().parents[1]


def request(path, patch=None):
    token = os.environ.get('MODRINTH_TOKEN')
    if not token:
        raise SystemExit('Missing MODRINTH_TOKEN Actions Secret.')
    headers = {'User-Agent': 'ArcaneEngineering-release/0.3.0', 'Authorization': token}
    data = None if patch is None else json.dumps(patch).encode('utf-8')
    if data is not None:
        headers['Content-Type'] = 'application/json'
    req = urllib.request.Request('https://api.modrinth.com/v2/' + path, data=data,
                                 headers=headers, method='GET' if patch is None else 'PATCH')
    try:
        with urllib.request.urlopen(req, timeout=60) as response:
            body = response.read()
            return json.loads(body) if body else None
    except urllib.error.HTTPError as error:
        if error.code in (401, 403, 404):
            raise SystemExit('Modrinth HTTP %s: the Secret needs access to this project, Read projects, Write projects and Create/Write versions permissions. Update the Secret in GitHub; never print its value.' % error.code) from None
        message = error.read().decode('utf-8', errors='replace').replace(token, '[redacted]')
        raise SystemExit('Modrinth HTTP %s: %s' % (error.code, message)) from None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project', required=True, help='Project ID or Slug')
    parser.add_argument('--sync', action='store_true')
    parser.add_argument('--submit', action='store_true')
    args = parser.parse_args()
    config = json.loads((ROOT / 'release/project.json').read_text(encoding='utf-8'))
    approvals = json.loads((ROOT / 'release/approvals.json').read_text(encoding='utf-8'))
    assert approvals['version'] == config['version'] and all(approvals.get(k) is True for k in
        ('license_approved', 'third_party_cleared', 'public_release_approved')), 'Publication is not approved'
    project = request('project/' + quote(args.project, safe=''))
    assert project['slug'] == config['modrinth_slug'], 'Unexpected Modrinth project Slug'
    assert not config.get('modrinth_project_id') or project['id'] == config['modrinth_project_id'], 'Unexpected Modrinth project ID'
    assert project['project_type'] == 'mod', 'Expected a Mod project'
    path = 'project/' + project['id']
    if args.sync:
        repository = 'https://github.com/' + config['github_repository']
        desired = dict(title=config['name'], description=config['description'],
            body=(ROOT / 'release/modrinth-description.md').read_text(encoding='utf-8'),
            categories=config['categories'], license_id='MIT', source_url=repository,
            issues_url=repository + '/issues', client_side=config['client_side'], server_side=config['server_side'])
        current = dict(project, license_id=project['license']['id'])
        patch = {k: v for k, v in desired.items() if current.get(k) != v}
        if patch:
            request(path, patch)
        project = request(path)
        assert project['license']['id'] == 'MIT', 'Modrinth MIT license was not saved'
    if args.submit and project['status'] == 'draft':
        assert project['versions'], 'Cannot submit a project without a version'
        request(path, {'status': 'processing', 'requested_status': 'approved'})
        project = request(path)
    if args.submit:
        assert project['status'] in ('processing', 'approved'), 'Project needs owner/moderator attention: ' + project['status']
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a', encoding='utf-8') as output:
            output.write('modrinth_id=' + project['id'] + '\n')
    print(json.dumps({'id': project['id'], 'slug': project['slug'], 'status': project['status'],
                      'license': project['license']['id']}))


if __name__ == '__main__':
    main()
