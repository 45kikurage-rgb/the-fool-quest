"""Verify official v0.1.12/code13 -> v0.1.13/code14 on a clean emulator.

Requires both official APKs; never substitute a test-signed build or uninstall.
"""
import datetime
import pathlib
import re
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE = 'com.aruno.foolquest.widget'
OLD_APK = pathlib.Path('android-widget/releases/fool-quest-widget-0.1.12.apk')
NEW_APK = pathlib.Path('android-widget/releases/fool-quest-widget-0.1.13.apk')
PREFS = '/data/user/0/' + PACKAGE + '/shared_prefs/widget.xml'
OUT = pathlib.Path('android-widget/build/release-upgrade')
OUT.mkdir(parents=True, exist_ok=True)
# Remove stale success evidence before any preflight can fail.
(OUT / 'result.txt').unlink(missing_ok=True)

def adb(*args):
    result = subprocess.run(['adb', *args], check=True, text=True, capture_output=True)
    return result.stdout

def prefs():
    root = ET.fromstring(adb('shell', 'cat', PREFS))
    return {e.attrib['name']: e.attrib.get('value', e.text or '') for e in root}

def check_preserved(after_fetch=False):
    actual = prefs()
    for name, value in expected.items():
        if after_fetch and name == 'lastFailureAt':
            assert int(actual[name]) >= int(value)
            continue
        assert actual.get(name) == value, (name, actual.get(name), value)

assert OLD_APK.is_file() and NEW_APK.is_file(), 'Both official APKs are required'
assert adb('shell', 'getprop', 'ro.kernel.qemu').strip() == '1', 'Isolated emulator only'
assert not adb('shell', 'pm', 'list', 'packages', PACKAGE).strip(), 'Use a clean emulator; do not uninstall'
adb('root')
adb('wait-for-device')
adb('shell', 'svc', 'wifi', 'disable')
adb('shell', 'svc', 'data', 'disable')
adb('shell', 'cmd', 'connectivity', 'airplane-mode', 'enable')
assert 'Success' in adb('install', str(OLD_APK))
old_info = adb('shell', 'dumpsys', 'package', PACKAGE)
assert re.search(r'versionCode=13\b', old_info)
assert 'versionName=0.1.12' in old_info
uid = re.search(r'uid:(\d+)', adb('shell', 'cmd', 'package', 'list', 'packages', '-U', PACKAGE)).group(1)
month = datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=9))).strftime('%Y-%m')
now = int(time.time() * 1000)
fixture = ET.Element('map')
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
for name, (kind, value) in entries.items():
    elem = ET.SubElement(fixture, kind, name=name)
    if kind == 'string':
        elem.text = value
    else:
        elem.set('value', value)
expected = {name: value for name, (_, value) in entries.items()}
path = OUT / 'fixture.xml'
ET.ElementTree(fixture).write(path, encoding='utf-8', xml_declaration=True)
adb('shell', 'mkdir', '-p', str(pathlib.PurePosixPath(PREFS).parent))
adb('push', str(path), PREFS)
adb('shell', 'chown', '-R', uid + ':' + uid, '/data/user/0/' + PACKAGE + '/shared_prefs')
adb('shell', 'chmod', '660', PREFS)
adb('shell', 'restorecon', '-RF', '/data/user/0/' + PACKAGE)
adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.SettingsActivity')
time.sleep(3)
adb('shell', 'am', 'force-stop', PACKAGE)
check_preserved(after_fetch=True)
expected['lastFailureAt'] = prefs()['lastFailureAt']
before_update = prefs()
assert 'Success' in adb('install', '-r', str(NEW_APK))
assert prefs() == before_update, 'All stored preferences must survive installation exactly'
check_preserved()
info = adb('shell', 'dumpsys', 'package', PACKAGE)
assert re.search(r'versionCode=14\b', info)
assert 'versionName=0.1.13' in info
updated_uid = re.search(r'uid:(\d+)', adb('shell', 'cmd', 'package', 'list', 'packages', '-U', PACKAGE)).group(1)
assert updated_uid == uid
adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.SettingsActivity')
time.sleep(3)
adb('shell', 'uiautomator', 'dump', '/sdcard/upgrade-ui.xml')
adb('pull', '/sdcard/upgrade-ui.xml', str(OUT / 'upgrade-ui.xml'))
ui = (OUT / 'upgrade-ui.xml').read_text()
assert '収益ウィジェット 0.1.13' in ui
assert '115.00%' in ui
assert '1,150,000' in ui
assert 'content-desc="×2"' in ui, 'Updated native preview must display the second lap'
with (OUT / 'upgrade-preview.png').open('wb') as screenshot:
    subprocess.run(['adb', 'exec-out', 'screencap', '-p'], check=True, stdout=screenshot)
adb('shell', 'am', 'force-stop', PACKAGE)
check_preserved(after_fetch=True)
(OUT / 'result.txt').write_text('PASS official v0.1.12/code13 -> v0.1.13/code14 adb install -r; same UID; entire preference snapshot and all 29 saved settings/cache/history fields preserved across installation; 28 fixture values preserved after first launch, with lastFailureAt refreshed by the expected offline fetch; native cumulative 115.00% and second-lap ×2 preview verified.\n')
print((OUT / 'result.txt').read_text())
