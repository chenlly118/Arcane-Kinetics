"""Package one checked production JAR; never upload or publish."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
from release_check import ROOT, check


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--build-method', choices=['gradle', 'installed-runtime'], required=True)
    parser.add_argument('--output', type=Path, default=ROOT / 'build/release-candidate')
    args = parser.parse_args()
    result = check(args.jar)
    if not result['passed']:
        raise SystemExit(json.dumps(result, ensure_ascii=True))
    result['build_method'] = args.build_method
    result['candidate_only'] = not result['publication_approved']
    output = args.output / result['version']
    output.mkdir(parents=True, exist_ok=True)
    target = output / args.jar.name
    if target.resolve() != args.jar.resolve():
        shutil.copy2(args.jar, target)
    assert hashlib.sha256(target.read_bytes()).hexdigest() == result['production_sha256']
    checksum = result['production_sha256'] + '  ' + target.name + '\n'
    (output / 'SHA256SUMS.txt').write_text(checksum, encoding='utf-8')
    (output / (target.name + '.sha256')).write_text(checksum, encoding='utf-8')
    (output / 'validation.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    shutil.copy2(ROOT / 'release/notes' / (result['version'] + '.md'), output / 'RELEASE_NOTES.md')
    print(json.dumps({'version': result['version'], 'production_sha256': result['production_sha256'], 'build_method': args.build_method, 'candidate_only': result['candidate_only']}, indent=2))


if __name__ == '__main__':
    main()
