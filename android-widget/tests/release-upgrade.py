"""Verify the previous official successor APK updates to the revised official APK.

Both successor binaries are v0.1.13/code14 with the same signing identity.
Never uninstall either app. The legacy v0.1.12 app remains untouched.
"""
import datetime
import hashlib
import json
import os
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

OLD_PACKAGE = 'com.aruno.foolquest.widget'
PACKAGE = OLD_PACKAGE + '.next'
OLD_CERTIFICATE = '18946d320bb61c58aa627abbf2b53ea521b273b218eaec57f7fbc662f375bdb1'
OLD_APK = pathlib.Path('android-widget/releases/fool-quest-widget-0.1.12.apk')
BASE_APK = pathlib.Path('android-widget/releases/fool-quest-widget-0.1.13-original.apk')
BASE_SHA256 = 'c896ee019bedaa983ef9362c06be83452d9111aaefe80247466e932f2eeb9f31'
BASE_SIZE = 59182
SUCCESSOR_CERTIFICATE = '8495f4b4b508ca5b24c2b7fd714b21963f5b1c0275659f434e3e0d9e239c6730'
NEW_APK = pathlib.Path('android-widget/releases/fool-quest-widget-0.1.13.apk')
OUT = pathlib.Path('android-widget/build/release-upgrade')
OUT.mkdir(parents=True, exist_ok=True)
(OUT / 'result.txt').unlink(missing_ok=True)


def adb(*args):
    return subprocess.run(['adb', *args], check=True, text=True,
                          capture_output=True).stdout


def uid(package):
    output = adb('shell', 'cmd', 'package', 'list', 'packages', '-U', package)
    match = re.search(r'^package:' + re.escape(package) + r' uid:(\d+)\s*$',
                      output, re.MULTILINE)
    assert match, (package, output)
    return match.group(1)


def prefs_path(package):
    return '/data/user/0/' + package + '/shared_prefs/widget.xml'


def prefs(package):
    root = ET.fromstring(adb('shell', 'cat', prefs_path(package)))
    return {e.attrib['name']: e.attrib.get('value', e.text or '') for e in root}


def check_preserved(package, expected, after_fetch=False):
    actual = prefs(package)
    for name, value in expected.items():
        if after_fetch and name == 'lastFailureAt':
            assert int(actual[name]) >= int(value)
        else:
            assert actual.get(name) == value, (package, name, actual.get(name), value)


def write_fixture(package):
    target = prefs_path(package)
    adb('shell', 'am', 'force-stop', package)
    adb('shell', 'mkdir', '-p', str(pathlib.PurePosixPath(target).parent))
    adb('push', str(fixture_path), target)
    owner = uid(package)
    adb('shell', 'chown', '-R', owner + ':' + owner,
        str(pathlib.PurePosixPath(target).parent))
    adb('shell', 'chmod', '660', target)
    adb('shell', 'restorecon', '-RF', '/data/user/0/' + package)
    check_preserved(package, expected)


def verify_apk(apk, package, version, code, certificate):
    sdk = os.environ.get('ANDROID_BUILD_TOOLS')
    if not sdk:
        sdk_root = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
        assert sdk_root, 'Set ANDROID_BUILD_TOOLS or ANDROID_HOME'
        sdk = str(pathlib.Path(sdk_root) / 'build-tools' / '35.0.0')
    signer = subprocess.run(['java', '-jar', str(pathlib.Path(sdk) / 'lib/apksigner.jar'),
                             'verify', '--verbose', '--print-certs', str(apk)],
                            check=True, text=True, capture_output=True).stdout
    digest = re.search(r'Signer #1 certificate SHA-256 digest: ([a-f0-9]+)', signer)
    assert digest and digest.group(1) == certificate, 'Unexpected signing certificate'
    badging = subprocess.run([str(pathlib.Path(sdk) / 'aapt'), 'dump', 'badging', str(apk)],
                             check=True, text=True, capture_output=True).stdout
    assert f"package: name='{package}' versionCode='{code}' versionName='{version}'" in badging
    (OUT / (apk.stem + '-signature.txt')).write_text(signer)


assert all(apk.is_file() for apk in [OLD_APK, BASE_APK, NEW_APK]), 'Legacy, previous and revised official APKs are required'
assert hashlib.sha256(BASE_APK.read_bytes()).hexdigest() == BASE_SHA256
assert BASE_APK.stat().st_size == BASE_SIZE
assert BASE_APK.read_bytes() != NEW_APK.read_bytes(), 'Upgrade must test two distinct official binaries'
manifest = json.loads(pathlib.Path('android-widget/releases/manifest.json').read_text())
assert (manifest['applicationId'], manifest['version'], manifest['versionCode']) == (PACKAGE, '0.1.13', 14)
assert manifest['previousRelease']['file'] == BASE_APK.name
assert manifest['previousRelease']['sha256'] == BASE_SHA256
assert manifest['previousRelease']['size'] == BASE_SIZE
new_certificate = manifest['certificateSha256']
assert re.fullmatch(r'[a-f0-9]{64}', new_certificate)
assert new_certificate == SUCCESSOR_CERTIFICATE, 'Successor signing identity must remain unchanged'
assert hashlib.sha256(NEW_APK.read_bytes()).hexdigest() == manifest['sha256']
assert NEW_APK.stat().st_size == manifest['size']
verify_apk(OLD_APK, OLD_PACKAGE, '0.1.12', 13, OLD_CERTIFICATE)
verify_apk(BASE_APK, PACKAGE, '0.1.13', 14, new_certificate)
verify_apk(NEW_APK, PACKAGE, '0.1.13', 14, new_certificate)
assert adb('shell', 'getprop', 'ro.kernel.qemu').strip() == '1', 'Isolated emulator only'
assert not adb('shell', 'pm', 'list', 'packages', OLD_PACKAGE).strip(), 'Clean emulator required; do not uninstall'
adb('root')
adb('wait-for-device')
adb('shell', 'svc', 'wifi', 'disable')
adb('shell', 'svc', 'data', 'disable')
adb('shell', 'cmd', 'connectivity', 'airplane-mode', 'enable')
assert 'Success' in adb('install', str(OLD_APK))
old_uid = uid(OLD_PACKAGE)
month = datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=9))).strftime('%Y-%m')
now = int(time.time() * 1000)
entries = {
    'importMonth': ('string', month), 'couponMonth': ('string', month),
    'tiktok': ('long', '1150000'), 'coupon': ('long', '0'),
    'goalTotal': ('long', '1000000'), 'goalTiktok': ('long', '1000000'),
    'goalCoupon': ('long', '600000'), 'importAt': ('long', str(now)),
    'couponAt': ('long', str(now)), 'text': ('int', '-1'),
    'background': ('int', '-16777216'), 'colorTotal': ('int', '-195508'),
    'colorTiktok': ('int', '-44462'), 'colorCoupon': ('int', '-11870592'),
    'colorPaceAchieved': ('int', '-15654349'),
    'colorPaceBehind': ('int', '-12298906'),
    'colorPaceLow': ('int', '-8943463'),
    'colorSecondLap': ('int', '-5517841'),
    'paceColors': ('boolean', 'false'), 'opacity': ('int', '83'),
    'font': ('int', '14'), 'left': ('int', '9'), 'right': ('int', '10'),
    'top': ('int', '7'), 'bottom': ('int', '8'), 'gap': ('int', '6'),
    'gauge': ('int', '5'), 'lastFailureCode': ('string', 'OFFLINE'),
    'lastFailureAt': ('long', str(now - 60000)),
}
fixture = ET.Element('map')
for name, (kind, value) in entries.items():
    elem = ET.SubElement(fixture, kind, name=name)
    if kind == 'string':
        elem.text = value
    else:
        elem.set('value', value)
expected = {name: value for name, (_, value) in entries.items()}
fixture_path = OUT / 'fixture.xml'
ET.ElementTree(fixture).write(fixture_path, encoding='utf-8', xml_declaration=True)
write_fixture(OLD_PACKAGE)
old_snapshot = prefs(OLD_PACKAGE)
# Install the actual prior published successor before testing its update.
assert 'Success' in adb('install', str(BASE_APK))
new_uid = uid(PACKAGE)
assert new_uid != old_uid, 'Separate applications must have independent UIDs'
assert uid(OLD_PACKAGE) == old_uid
assert prefs(OLD_PACKAGE) == old_snapshot, 'Old settings/cache/history must remain untouched'
old_info = adb('shell', 'dumpsys', 'package', OLD_PACKAGE)
assert re.search(r'versionCode=13\b', old_info) and 'versionName=0.1.12' in old_info
info = adb('shell', 'dumpsys', 'package', PACKAGE)
assert re.search(r'versionCode=14\b', info) and 'versionName=0.1.13' in info
# Seed the previous official successor, then update without uninstalling it.
write_fixture(PACKAGE)
before_upgrade = prefs(PACKAGE)
assert 'Success' in adb('install', '-r', str(NEW_APK))
assert uid(PACKAGE) == new_uid
assert prefs(PACKAGE) == before_upgrade, 'Successor preferences must survive the revised APK upgrade exactly'
check_preserved(PACKAGE, expected)
adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.SettingsActivity')
time.sleep(3)
adb('shell', 'uiautomator', 'dump', '/sdcard/upgrade-ui.xml')
adb('pull', '/sdcard/upgrade-ui.xml', str(OUT / 'upgrade-ui.xml'))
ui = (OUT / 'upgrade-ui.xml').read_text()
assert '収益ウィジェット 0.1.13' in ui
assert '115.00%' in ui and '1,150,000' in ui
# Android exposes the preview's aggregate accessibility description rather than
# nested lap-label ImageViews. Exact lap rendering is checked by native instrumentation.
nodes = list(ET.fromstring(ui).iter('node'))
assert any('Total ¥1,150,000/¥1,000,000 115.00%' in node.attrib.get('content-desc', '')
           for node in nodes), 'Successor native preview must expose cumulative revenue'
preview_nodes = [node for node in nodes
                 if 'Total ¥1,150,000/¥1,000,000 115.00%' in node.attrib.get('content-desc', '')]
(OUT / 'preview-bounds.json').write_text(json.dumps(
    [node.attrib for node in preview_nodes], ensure_ascii=False, indent=2))
(OUT / 'activity-top.txt').write_text(adb('shell', 'dumpsys', 'activity', 'top'))
with (OUT / 'upgrade-preview.png').open('wb') as screenshot:
    subprocess.run(['adb', 'exec-out', 'screencap', '-p'], check=True, stdout=screenshot)
adb('shell', 'am', 'force-stop', PACKAGE)
check_preserved(PACKAGE, expected, after_fetch=True)
assert uid(OLD_PACKAGE) == old_uid and prefs(OLD_PACKAGE) == old_snapshot
(OUT / 'result.txt').write_text(
    'PASS previous official v0.1.13/code14 successor updated to revised v0.1.13/code14 '
    'using adb install -r with unchanged official certificate; two distinct APKs verified; '
    'successor UID and all 29 fixture fields retained exactly after update; '
    '28 fixture values retained after launch with offline lastFailureAt refreshed; '
    'legacy v0.1.12/code13 UID and all 29 fixture fields remain untouched; '
    'native preview cumulative revenue 115.00% verified and screenshot recorded; '
    'exact revised gauge rendering is separately checked by native instrumentation CI. '
    'No uninstall and no automatic legacy-to-successor migration. '
    'Physical-device checks remain unverified.\n')
print((OUT / 'result.txt').read_text())
