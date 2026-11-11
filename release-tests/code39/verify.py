"""Run official-signer upgrade checks only on a disposable Android emulator."""
import hashlib,json,os,pathlib,subprocess,sys,time
root=pathlib.Path.cwd(); app=sys.argv[1]; api=int(sys.argv[2])
c=json.loads((root/'release-tests/code39/request.json').read_text());c['previous_sha256']='bbedbf177e7e3aea68f9ddfce19f0ea12c5a4754c16d0f5e7221a5eaa088d50b'
os.environ['EXPECTED_SIGNING_SOURCE']=c['source_commit']
def run(*a,**kw):return subprocess.run(a,check=True,**kw)
def output(*a):return subprocess.check_output(a,text=True)
assert output('adb','shell','getprop','ro.kernel.qemu').strip()=='1'
assert int(output('adb','shell','getprop','ro.build.version.sdk'))==api
source=root/'source'
assert (source/'.git').exists(), 'Pinned source must be checked out through the existing authorized checkout action'
assert output('git','-C',str(source),'rev-parse','HEAD').strip()==c['source_commit']
signed=list((root/'release-tests/code39/signed').glob('*.apk'));assert len(signed)==1
apk=signed[0]; proof=json.loads((root/'release-tests/code39/signed/trial-result.json').read_text())
assert proof['apk_sha256']==hashlib.sha256(apk.read_bytes()).hexdigest()
assert proof['source_commit']==c['source_commit'] and proof['existing_key'] is True
tools=pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools/35.0.0'
cert=output(str(tools/'apksigner'),'verify','--verbose','--print-certs',str(apk))
assert c['certificate_sha256'] in cert
out=root/'verification';out.mkdir(exist_ok=True)
(out/'signature.txt').write_text(cert)
releases=source/'android-widget/releases';old=json.loads((releases/'manifest.json').read_text())
assert old['versionCode']==38 and old['sha256']==c['previous_sha256']
assert hashlib.sha256((releases/old['file']).read_bytes()).hexdigest()==c['previous_sha256']
previous={k:old[k] for k in ('applicationId','file','sha256','size','version','versionCode','certificateSha256')}
old.update(file='aruno-result-0.1.38.apk',version='0.1.38',versionCode=39,sha256=proof['apk_sha256'],size=apk.stat().st_size,previousRelease=previous)
(releases/old['file']).write_bytes(apk.read_bytes());(releases/'manifest.json').write_text(json.dumps(old))
import shutil
import xml.etree.ElementTree as ET
try:
 run('python3',str(root/'release-tests/code39/upgrade_result.py'),cwd=source)
finally:
 shutil.copytree(source/'android-widget/build/release-upgrade',out/'upgrade',dirs_exist_ok=True)
 with (out/'upgrade-last-screen.png').open('wb') as f:
  subprocess.run(['adb','exec-out','screencap','-p'],stdout=f,timeout=20)
home=ET.parse(out/'upgrade/target-home.xml')
buttons=[node.attrib.get('text') for node in home.iter('node')
         if node.attrib.get('class')=='android.widget.Button']
chrome='えらべるPayのポイント履歴をChromeで開く'
assert buttons.count(chrome)==1 and buttons[buttons.index(chrome)+1]=='CSVを取り込む', buttons
print('Chrome history button is immediately above CSV import')
# Capture the exact persisted revenue/settings after launch, then reboot without resetting data.
run('adb','shell','am','force-stop',c['package'])
prefs='/data/user/0/'+c['package']+'/shared_prefs/widget.xml'
before=output('adb','shell','cat',prefs)
run('adb','reboot');run('adb','wait-for-device')
run('timeout','180','bash','-c','until adb shell getprop sys.boot_completed | grep -q 1; do sleep 2; done')
# adbd restarts during root negotiation; verify readiness rather than assuming
# the transport survives the command. Keep exact preservation checks below.
deadline=time.monotonic()+90
while time.monotonic()<deadline:
 subprocess.run(['adb','wait-for-device'],timeout=15,check=False)
 subprocess.run(['adb','root'],timeout=15,check=False)
 time.sleep(2)
 ready=subprocess.run(['adb','shell','id','-u'],capture_output=True,text=True,timeout=15)
 if ready.returncode==0 and ready.stdout.strip()=='0':break
else:raise AssertionError('Root ADB did not recover after reboot within 90 seconds')
assert before==output('adb','shell','cat',prefs), 'Preferences changed across reboot'
(out/'reboot-preservation.json').write_text(json.dumps({'passed':True,'exact_widget_preferences_preserved':True}))
result={'passed':True,'app':app,'api':api,'package':c['package'],'version_code':c['version_code'],'source_commit':c['source_commit'],'apk_sha256':proof['apk_sha256'],'certificate_sha256':c['certificate_sha256'],'data_preservation':'PASS_DISPOSABLE_EMULATOR','physical_device_verified':False,'published':False}
(out/'result.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))

