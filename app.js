/* =========================================================
   ASSETS MANAGEMENT - SCRIPT INDEX
   ---------------------------------------------------------
   State / local storage
   Rendering / tab UI
   Asset & debt calculations
   Charts / current-month delta
   Server sync (GET / PUT)
   Event handlers / startup

   Logic is unchanged in this Step 1 cleanup.
   ========================================================= */

(()=>{
const K='asset-tracker-v4';
const OLD_KEYS=['asset-tracker-v3','asset-tracker-v2','asset-tracker-v1'];
const $=id=>document.getElementById(id);
const fmt=n=>'¥'+Math.round(Number(n)||0).toLocaleString('ja-JP');
const today=()=>{const d=new Date();return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`};
const ymNow=()=>today().slice(0,7);
const monthLabel=ym=>`${Number((ym||ymNow()).slice(5,7))}月返済金額`;
function uid(){return Math.random().toString(36).slice(2)+Date.now().toString(36)}
function num(v){return Number(String(v??'').replace(/[^\d.-]/g,''))||0}
function calculateAmount(value){
 const raw=String(value??'').trim();
 if(!raw)return {ok:true,value:0};
 const expr=raw
  .replace(/[０-９]/g,ch=>String.fromCharCode(ch.charCodeAt(0)-0xFEE0))
  .replace(/[．。]/g,'.')
  .replace(/[，,￥¥\s]/g,'')
  .replace(/[×✕✖＊xXｘＸ]/g,'*')
  .replace(/[÷／]/g,'/')
  .replace(/＋/g,'+')
  .replace(/[−ー－]/g,'-')
  .replace(/（/g,'(')
  .replace(/）/g,')');
 if(!/^[0-9.+\-*/()]+$/.test(expr))return {ok:false};
 let pos=0;
 const parseExpression=()=>{
  let result=parseTerm();
  while(expr[pos]==='+'||expr[pos]==='-'){
   const op=expr[pos++],right=parseTerm();
   result=op==='+'?result+right:result-right;
  }
  return result;
 };
 const parseTerm=()=>{
  let result=parseFactor();
  while(expr[pos]==='*'||expr[pos]==='/'){
   const op=expr[pos++],right=parseFactor();
   if(op==='/'&&right===0)throw new Error('division by zero');
   result=op==='*'?result*right:result/right;
  }
  return result;
 };
 const parseFactor=()=>{
  if(expr[pos]==='+'){pos++;return parseFactor()}
  if(expr[pos]==='-'){pos++;return -parseFactor()}
  if(expr[pos]==='('){
   pos++;const result=parseExpression();
   if(expr[pos]!==')')throw new Error('missing parenthesis');
   pos++;return result;
  }
  const match=expr.slice(pos).match(/^(?:\d+(?:\.\d*)?|\.\d+)/);
  if(!match)throw new Error('number expected');
  pos+=match[0].length;
  return Number(match[0]);
 };
 try{
  const result=parseExpression();
  if(pos!==expr.length||!Number.isFinite(result))return {ok:false};
  return {ok:true,value:Math.round(result)};
 }catch(e){return {ok:false}}
}
function clone(v){return JSON.parse(JSON.stringify(v))}
function esc(s){return String(s??'').replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m]))}
function toast(t){const f=$('flash');f.textContent=t;f.classList.add('show');clearTimeout(toast._t);toast._t=setTimeout(()=>f.classList.remove('show'),1600)}
const DEFAULT_ASSET_NAMES=['住信SBIネット銀行','横浜銀行','PayPay','PayPayサブ','えらぺい'];
const SEED_LENDERS=[{"id":"seed-01","name":"両親","account":"横浜銀行 厚木支店 1565579 ヨコタ マユミ","totalRepayment":5050000,"monthlyPayment":450000,"remaining":3700000,"finishMonth":"2027-05","repayMonth":"2026-08","paidMonths":["2026-06","2026-07","2026-08"],"schedule":{"2026-06":500000,"2026-07":400000,"2026-08":450000,"2026-09":450000,"2026-10":450000,"2026-11":450000,"2026-12":450000,"2027-01":450000,"2027-02":450000,"2027-03":450000,"2027-04":450000,"2027-05":100000}},{"id":"seed-02","name":"楽天カード(株)（楽天銀行分）","account":"三井住友銀行 すずらん支店 普通 9468564 ラクテンカード(カ","totalRepayment":2287407,"monthlyPayment":20910,"remaining":711057,"finishMonth":"2029-05","repayMonth":"2026-08","paidMonths":["2026-06","2026-07"],"schedule":{"2026-06":1555440,"2026-07":20910,"2026-08":20910,"2026-09":20910,"2026-10":20910,"2026-11":20910,"2026-12":20910,"2027-01":20910,"2027-02":20910,"2027-03":20910,"2027-04":20910,"2027-05":20910,"2027-06":20910,"2027-07":20910,"2027-08":20910,"2027-09":20910,"2027-10":20910,"2027-11":20910,"2027-12":20910,"2028-01":20910,"2028-02":20910,"2028-03":20910,"2028-04":20910,"2028-05":20910,"2028-06":20910,"2028-07":20910,"2028-08":20910,"2028-09":20910,"2028-10":20910,"2028-11":20910,"2028-12":20910,"2029-01":20910,"2029-02":20910,"2029-03":20910,"2029-04":20910,"2029-05":21027}},{"id":"seed-03","name":"合同会社紫雲","account":"北海道銀行 本店営業部 普通 3387021 ベンミズクラブ ウゴウホククリッジ ムショ アズカリグチ","totalRepayment":1526277,"monthlyPayment":13950,"remaining":474457,"finishMonth":"2029-05","repayMonth":"2026-08","paidMonths":["2026-06","2026-07"],"schedule":{"2026-06":1037870,"2026-07":13950,"2026-08":13950,"2026-09":13950,"2026-10":13950,"2026-11":13950,"2026-12":13950,"2027-01":13950,"2027-02":13950,"2027-03":13950,"2027-04":13950,"2027-05":13950,"2027-06":13950,"2027-07":13950,"2027-08":13950,"2027-09":13950,"2027-10":13950,"2027-11":13950,"2027-12":13950,"2028-01":13950,"2028-02":13950,"2028-03":13950,"2028-04":13950,"2028-05":13950,"2028-06":13950,"2028-07":13950,"2028-08":13950,"2028-09":13950,"2028-10":13950,"2028-11":13950,"2028-12":13950,"2029-01":13950,"2029-02":13950,"2029-03":13950,"2029-04":13950,"2029-05":14107}},{"id":"seed-04","name":"SMBCコンシューマーファイナンス(株)","account":"三井住友銀行 あじさい支店 普通 6332838 SMBCコンシューマーファイナンス(カ","totalRepayment":1354167,"monthlyPayment":12380,"remaining":420947,"finishMonth":"2029-05","repayMonth":"2026-08","paidMonths":["2026-06","2026-07"],"schedule":{"2026-06":920840,"2026-07":12380,"2026-08":12380,"2026-09":12380,"2026-10":12380,"2026-11":12380,"2026-12":12380,"2027-01":12380,"2027-02":12380,"2027-03":12380,"2027-04":12380,"2027-05":12380,"2027-06":12380,"2027-07":12380,"2027-08":12380,"2027-09":12380,"2027-10":12380,"2027-11":12380,"2027-12":12380,"2028-01":12380,"2028-02":12380,"2028-03":12380,"2028-04":12380,"2028-05":12380,"2028-06":12380,"2028-07":12380,"2028-08":12380,"2028-09":12380,"2028-10":12380,"2028-11":12380,"2028-12":12380,"2029-01":12380,"2029-02":12380,"2029-03":12380,"2029-04":12380,"2029-05":12407}},{"id":"seed-05","name":"楽天カード(株)（プロパー分）","account":"楽天銀行 楽天第一支店 普通 6390498 ラクテンカード(カ","totalRepayment":1335501,"monthlyPayment":12210,"remaining":415151,"finishMonth":"2029-05","repayMonth":"2026-08","paidMonths":["2026-06","2026-07"],"schedule":{"2026-06":908140,"2026-07":12210,"2026-08":12210,"2026-09":12210,"2026-10":12210,"2026-11":12210,"2026-12":12210,"2027-01":12210,"2027-02":12210,"2027-03":12210,"2027-04":12210,"2027-05":12210,"2027-06":12210,"2027-07":12210,"2027-08":12210,"2027-09":12210,"2027-10":12210,"2027-11":12210,"2027-12":12210,"2028-01":12210,"2028-02":12210,"2028-03":12210,"2028-04":12210,"2028-05":12210,"2028-06":12210,"2028-07":12210,"2028-08":12210,"2028-09":12210,"2028-10":12210,"2028-11":12210,"2028-12":12210,"2029-01":12210,"2029-02":12210,"2029-03":12210,"2029-04":12210,"2029-05":12221}},{"id":"seed-06","name":"アビリオ債権回収(株)","account":"三井住友銀行 はまゆう支店 普通 9453877 アビリオサイケンカイシュウ(カ","totalRepayment":1219268,"monthlyPayment":11140,"remaining":379018,"finishMonth":"2029-05","repayMonth":"2026-08","paidMonths":["2026-06","2026-07"],"schedule":{"2026-06":829110,"2026-07":11140,"2026-08":11140,"2026-09":11140,"2026-10":11140,"2026-11":11140,"2026-12":11140,"2027-01":11140,"2027-02":11140,"2027-03":11140,"2027-04":11140,"2027-05":11140,"2027-06":11140,"2027-07":11140,"2027-08":11140,"2027-09":11140,"2027-10":11140,"2027-11":11140,"2027-12":11140,"2028-01":11140,"2028-02":11140,"2028-03":11140,"2028-04":11140,"2028-05":11140,"2028-06":11140,"2028-07":11140,"2028-08":11140,"2028-09":11140,"2028-10":11140,"2028-11":11140,"2028-12":11140,"2029-01":11140,"2029-02":11140,"2029-03":11140,"2029-04":11140,"2029-05":11398}},{"id":"seed-07","name":"セゾン債権回収(株)","account":"GMOあおぞらネット銀行 法人第二営業部 普通 3787613 セゾンサイケンカイシュウ(カ)CSカイトリグチ","totalRepayment":1136707,"monthlyPayment":10390,"remaining":353357,"finishMonth":"2029-05","repayMonth":"2026-08","paidMonths":["2026-06","2026-07"],"schedule":{"2026-06":772960,"2026-07":10390,"2026-08":10390,"2026-09":10390,"2026-10":10390,"2026-11":10390,"2026-12":10390,"2027-01":10390,"2027-02":10390,"2027-03":10390,"2027-04":10390,"2027-05":10390,"2027-06":10390,"2027-07":10390,"2027-08":10390,"2027-09":10390,"2027-10":10390,"2027-11":10390,"2027-12":10390,"2028-01":10390,"2028-02":10390,"2028-03":10390,"2028-04":10390,"2028-05":10390,"2028-06":10390,"2028-07":10390,"2028-08":10390,"2028-09":10390,"2028-10":10390,"2028-11":10390,"2028-12":10390,"2029-01":10390,"2029-02":10390,"2029-03":10390,"2029-04":10390,"2029-05":10487}},{"id":"seed-08","name":"(株)ジャックス","account":"みずほ銀行 ジャックス支店 普通 8373778 カ)ジャックス","totalRepayment":630488,"monthlyPayment":5760,"remaining":195988,"finishMonth":"2029-05","repayMonth":"2026-08","paidMonths":["2026-06","2026-07"],"schedule":{"2026-06":428740,"2026-07":5760,"2026-08":5760,"2026-09":5760,"2026-10":5760,"2026-11":5760,"2026-12":5760,"2027-01":5760,"2027-02":5760,"2027-03":5760,"2027-04":5760,"2027-05":5760,"2027-06":5760,"2027-07":5760,"2027-08":5760,"2027-09":5760,"2027-10":5760,"2027-11":5760,"2027-12":5760,"2028-01":5760,"2028-02":5760,"2028-03":5760,"2028-04":5760,"2028-05":5760,"2028-06":5760,"2028-07":5760,"2028-08":5760,"2028-09":5760,"2028-10":5760,"2028-11":5760,"2028-12":5760,"2029-01":5760,"2029-02":5760,"2029-03":5760,"2029-04":5760,"2029-05":5908}},{"id":"seed-09","name":"藤沢市職員福利厚生会","account":"スルガ銀行 藤沢支店 普通 3688830 フジサワシショクインフクリコウセイカイ","totalRepayment":261107,"monthlyPayment":2380,"remaining":81167,"finishMonth":"2029-05","repayMonth":"2026-08","paidMonths":["2026-06","2026-07"],"schedule":{"2026-06":177560,"2026-07":2380,"2026-08":2380,"2026-09":2380,"2026-10":2380,"2026-11":2380,"2026-12":2380,"2027-01":2380,"2027-02":2380,"2027-03":2380,"2027-04":2380,"2027-05":2380,"2027-06":2380,"2027-07":2380,"2027-08":2380,"2027-09":2380,"2027-10":2380,"2027-11":2380,"2027-12":2380,"2028-01":2380,"2028-02":2380,"2028-03":2380,"2028-04":2380,"2028-05":2380,"2028-06":2380,"2028-07":2380,"2028-08":2380,"2028-09":2380,"2028-10":2380,"2028-11":2380,"2028-12":2380,"2029-01":2380,"2029-02":2380,"2029-03":2380,"2029-04":2380,"2029-05":2627}},{"id":"seed-10","name":"(株)アプラス","account":"三菱UFJ銀行 関西中央支店 普通 1268516 カ)アプラス","totalRepayment":172916,"monthlyPayment":1580,"remaining":53746,"finishMonth":"2029-05","repayMonth":"2026-08","paidMonths":["2026-06","2026-07"],"schedule":{"2026-06":117590,"2026-07":1580,"2026-08":1580,"2026-09":1580,"2026-10":1580,"2026-11":1580,"2026-12":1580,"2027-01":1580,"2027-02":1580,"2027-03":1580,"2027-04":1580,"2027-05":1580,"2027-06":1580,"2027-07":1580,"2027-08":1580,"2027-09":1580,"2027-10":1580,"2027-11":1580,"2027-12":1580,"2028-01":1580,"2028-02":1580,"2028-03":1580,"2028-04":1580,"2028-05":1580,"2028-06":1580,"2028-07":1580,"2028-08":1580,"2028-09":1580,"2028-10":1580,"2028-11":1580,"2028-12":1580,"2029-01":1580,"2029-02":1580,"2029-03":1580,"2029-04":1580,"2029-05":1606}},{"id":"seed-11","name":"(株)エポスカード","account":"三菱UFJ銀行 めいけつ支店 普通 5119009 カ)エポスカード","totalRepayment":160907,"monthlyPayment":1470,"remaining":50017,"finishMonth":"2029-05","repayMonth":"2026-08","paidMonths":["2026-06","2026-07"],"schedule":{"2026-06":109420,"2026-07":1470,"2026-08":1470,"2026-09":1470,"2026-10":1470,"2026-11":1470,"2026-12":1470,"2027-01":1470,"2027-02":1470,"2027-03":1470,"2027-04":1470,"2027-05":1470,"2027-06":1470,"2027-07":1470,"2027-08":1470,"2027-09":1470,"2027-10":1470,"2027-11":1470,"2027-12":1470,"2028-01":1470,"2028-02":1470,"2028-03":1470,"2028-04":1470,"2028-05":1470,"2028-06":1470,"2028-07":1470,"2028-08":1470,"2028-09":1470,"2028-10":1470,"2028-11":1470,"2028-12":1470,"2029-01":1470,"2029-02":1470,"2029-03":1470,"2029-04":1470,"2029-05":1507}},{"id":"seed-12","name":"イオンフィナンシャルサービス(株)","account":"三井住友銀行 関東第三支店 普通 8200073 イオンフィナンシャルサービス(カ","totalRepayment":72947,"monthlyPayment":0,"remaining":0,"finishMonth":"2026-06","repayMonth":"2026-08","paidMonths":["2026-06","2026-07"],"schedule":{"2026-06":72947,"2026-07":0,"2026-08":0,"2026-09":0,"2026-10":0,"2026-11":0,"2026-12":0,"2027-01":0,"2027-02":0,"2027-03":0,"2027-04":0,"2027-05":0,"2027-06":0,"2027-07":0,"2027-08":0,"2027-09":0,"2027-10":0,"2027-11":0,"2027-12":0,"2028-01":0,"2028-02":0,"2028-03":0,"2028-04":0,"2028-05":0,"2028-06":0,"2028-07":0,"2028-08":0,"2028-09":0,"2028-10":0,"2028-11":0,"2028-12":0,"2029-01":0,"2029-02":0,"2029-03":0,"2029-04":0,"2029-05":0}},{"id":"seed-13","name":"(株)メルペイ","account":"GMOあおぞらネット銀行 ワシ支店 普通 1816688 カ)メルペイ ヨコタヒロカズ","totalRepayment":60966,"monthlyPayment":0,"remaining":0,"finishMonth":"2026-06","repayMonth":"2026-08","paidMonths":["2026-06","2026-07"],"schedule":{"2026-06":60966,"2026-07":0,"2026-08":0,"2026-09":0,"2026-10":0,"2026-11":0,"2026-12":0,"2027-01":0,"2027-02":0,"2027-03":0,"2027-04":0,"2027-05":0,"2027-06":0,"2027-07":0,"2027-08":0,"2027-09":0,"2027-10":0,"2027-11":0,"2027-12":0,"2028-01":0,"2028-02":0,"2028-03":0,"2028-04":0,"2028-05":0,"2028-06":0,"2028-07":0,"2028-08":0,"2028-09":0,"2028-10":0,"2028-11":0,"2028-12":0,"2029-01":0,"2029-02":0,"2029-03":0,"2029-04":0,"2029-05":0}}];
const MAX_LENDERS=13;
const TRANSFER_ALIASES={
 'seed-02':['ラクテンカード'],
 'seed-03':['ミズナラ'],
 'seed-04':['エスエムビーシーコンシユーマーフア','エスエムビーシーコンシューマーファ','SMBCコンシューマ'],
 'seed-05':['ラクテンカード'],
 'seed-06':['アビリオサイケン'],
 'seed-07':['セゾンサイケン'],
 'seed-08':['ジヤツクス','ジャックス'],
 'seed-09':['フジサワシシヨクインフクリコウセイ','フジサワシショクインフクリコウセイ'],
 'seed-10':['アプラス'],
 'seed-11':['エポスカード'],
 'seed-12':['イオンフィナンシャル'],
 'seed-13':['メルペイ']
};
let transferCheck={rows:[],results:[],month:ymNow(),fileName:''};

function freshState(){
 return {
  assets:DEFAULT_ASSET_NAMES.map(name=>({id:uid(),name,value:0})),
  debts:[],
  lenders:clone(SEED_LENDERS),
  records:[],
  activeTab:'home'
 };
}
function loadRaw(){
 try{
  const cur=JSON.parse(localStorage.getItem(K));
  if(cur)return cur;
  for(const key of OLD_KEYS){
   const old=JSON.parse(localStorage.getItem(key));
   if(old)return old;
  }
 }catch(e){}
 return null;
}
function normalize(raw){
 const s=raw||freshState();
 s.assets=Array.isArray(s.assets)?s.assets:[];
 for(const name of DEFAULT_ASSET_NAMES){
  if(!s.assets.some(x=>x.name===name))s.assets.push({id:uid(),name,value:0});
 }
 s.debts=Array.isArray(s.debts)?s.debts:[];
 s.records=Array.isArray(s.records)?s.records:[];
 s.activeTab=s.activeTab||'home';
 const current=Array.isArray(s.lenders)?s.lenders:[];
 const hasReal=current.some(x=>String(x?.name||'').trim());
 if(!hasReal){
  s.lenders=clone(SEED_LENDERS);
 }else{
  // 既存データを保ちつつ、標準13件（両親を含む）が必ず残るように統合する。
  const used=new Set();
  const merged=[];
  for(const seed of SEED_LENDERS){
   const found=current.find((x,idx)=>!used.has(idx) && (x?.id===seed.id || String(x?.name||'').trim()===seed.name));
   if(found){
    const idx=current.indexOf(found);used.add(idx);
    // ユーザー側で更新した残額・返済済み月などを優先する。
    merged.push({...clone(seed),...found,id:seed.id,name:found.name||seed.name});
   }else{
    merged.push(clone(seed));
   }
  }
  // 標準13件以外の手動データは空きがある場合のみ保持。
  for(let i=0;i<current.length && merged.length<MAX_LENDERS;i++){
   if(!used.has(i) && String(current[i]?.name||'').trim())merged.push(current[i]);
  }
  s.lenders=merged.slice(0,MAX_LENDERS);
 }
 s.lenders.forEach(x=>{
  x.id=x.id||uid();
  x.name=x.name||'';
  x.account=x.account||'';
  x.totalRepayment=num(x.totalRepayment);
  x.monthlyPayment=num(x.monthlyPayment);
  x.remaining=num(x.remaining);
  x.finishMonth=x.finishMonth||'';
  x.repayMonth=x.repayMonth||ymNow();
  x.paidMonths=Array.isArray(x.paidMonths)?x.paidMonths:[];
  x.schedule=x.schedule&&typeof x.schedule==='object'?x.schedule:{};
 });
 return s;
}
let state=normalize(loadRaw());
function persist(){localStorage.setItem(K,JSON.stringify(state))}

// 月が変わった最初の起動時に、各借入先の返済対象月を自動で当月へ進める。
// 同じ月の中ではユーザーが過去月を手動選択して確認できるよう、その選択を維持する。
function syncCalendarMonth(force=false){
 const current=ymNow();
 const changed=force || state.lastAutoMonth!==current;
 if(!changed)return false;
 state.lastAutoMonth=current;
 for(const x of state.lenders){
  x.repayMonth=current;
 }
 transferCheck.month=current;
 const vm=$('verifyMonth');
 if(vm)vm.value=current;
 persist();
 return true;
}
syncCalendarMonth();
persist();

function totals(){const a=state.assets.reduce((s,x)=>s+num(x.value),0),d=state.debts.reduce((s,x)=>s+num(x.value),0);return{a,d,n:a-d}}
function rowHtml(x,formula=false){
 const amountAttrs=formula?'inputmode="text" autocomplete="off" enterkeyhint="done" placeholder="0 または 3000+500"':'inputmode="numeric" placeholder="0"';
 return `<div class="row" data-id="${x.id}"><input class="name" value="${esc(x.name)}"><input class="amt money" ${amountAttrs} value="${x.value||''}"><button class="small danger del" type="button">削除</button></div>`;
}
function renderInputs(){
 $('assetRows').innerHTML=state.assets.map(x=>rowHtml(x,true)).join('');
 $('debtRows').innerHTML=state.debts.map(x=>rowHtml(x)).join('');
 bindRows('assetRows',state.assets);bindRows('debtRows',state.debts);
 updateTotals();
}
function bindRows(id,arr){
 $(id).querySelectorAll('.row').forEach(r=>{
  const x=arr.find(v=>v.id===r.dataset.id);
  r.querySelector('.name').addEventListener('input',e=>{x.name=e.target.value;persist()});
  const amount=r.querySelector('.amt');
  if(id==='assetRows'){
   amount.addEventListener('input',e=>{
    // 演算子を含む間は式を崩さず、入力確定時にまとめて計算する。
    if(/[+＋×✕✖*xXｘＸ÷/／()（）−ー－]/.test(e.target.value)||String(e.target.value).slice(1).includes('-'))return;
    x.value=num(e.target.value);persist();updateTotals();
   });
   amount.addEventListener('blur',()=>{
    const calculated=calculateAmount(amount.value);
    if(!calculated.ok){
     amount.value=x.value||'';
     toast('計算式を確認してください');
     return;
    }
    x.value=calculated.value;
    amount.value=calculated.value?String(calculated.value):'';
    persist();updateTotals();
   });
   amount.addEventListener('keydown',e=>{
    if(e.key==='Enter'){e.preventDefault();amount.blur()}
   });
  }else{
   amount.addEventListener('input',e=>{x.value=num(e.target.value);persist();updateTotals()});
  }
  r.querySelector('.del').onclick=()=>{arr.splice(arr.indexOf(x),1);persist();renderInputs()};
 });
}
function updateTotals(){
 const t=totals();
 $('assetTotal').textContent=$('assetSub').textContent=fmt(t.a);
 $('debtTotal').textContent=$('debtSub').textContent=fmt(t.d);
 $('netAsset').textContent=fmt(t.n);
 $('netAsset').className='big '+(t.n>=0?'pos':'neg');
 const recs=[...state.records].sort((a,b)=>a.date.localeCompare(b.date));
 const prevRec=recs.length?recs[recs.length-1]:null;
 const prev=prevRec?Number(prevRec.net)||0:0;
 const ch=t.n-prev;
 $('changeText').textContent=`前回比 ${ch>=0?'+':''}${fmt(ch)}`;
 $('changeText').className='hero-sub '+(ch>=0?'pos':'neg');
 const ach=t.a-(prevRec?Number(prevRec.asset)||0:0);
 const act=$('assetChangeText');if(act){act.textContent=`前回比 ${ach>=0?'+':''}${fmt(ach)}`;act.className='hero-sub '+(ach>=0?'pos':'neg')}
 if(state.activeTab==='home')requestAnimationFrame(drawHomeTrendChart);
}
function paymentFor(x,ym){
 if(x.schedule&&Object.prototype.hasOwnProperty.call(x.schedule,ym))return num(x.schedule[ym]);
 return num(x.monthlyPayment);
}
function lenderHtml(x,i){
 const ym=x.repayMonth||ymNow();
 const paid=(x.paidMonths||[]).includes(ym);
 const payment=paymentFor(x,ym);
 return `<div class="lender" data-id="${x.id}">
 <div class="lender-title editable-title" role="button" tabindex="0" title="タップして名称を編集">
   <span class="lender-no">No.${String(i+1).padStart(2,'0')}</span>
   <span class="lender-name-title">${esc(x.name||'未登録')}</span>
   <span class="muted" style="margin-left:auto;font-size:10px;flex:0 0 auto">名称編集</span>
 </div>
 <div class="lender-grid">
  <div class="field field-account"><label>返済口座</label><input class="account" value="${esc(x.account)}" placeholder="返済先口座"></div>
  <div class="field"><label>返済総額</label><input class="totalRepayment money" inputmode="numeric" value="${x.totalRepayment?Math.round(x.totalRepayment).toLocaleString('ja-JP'):''}" placeholder="0"></div>
  <div class="field"><label>返済残額</label><input class="remaining money" inputmode="numeric" value="${x.remaining?Math.round(x.remaining).toLocaleString('ja-JP'):''}" placeholder="0"></div>
  <div class="field field-payment"><label>${monthLabel(ym)}</label><div class="repay-line"><input class="repay money" inputmode="numeric" value="${payment?Math.round(payment).toLocaleString('ja-JP'):''}" placeholder="0"><button class="small copy" type="button">コピー</button></div></div>
  <div class="field"><label>返済完了月</label><input class="finish" type="month" value="${esc(x.finishMonth||'')}"></div>
  <div class="field"><label>対象月</label><input class="repayMonth" type="month" value="${ym}"></div>
 </div>
 <div class="lender-actions">
  <button class="small ${paid?'success':'warn'} pay" type="button" ${paid?'disabled':''}>${paid?`${Number(ym.slice(5,7))}月 返済済み`:`${Number(ym.slice(5,7))}月に返済した`}</button>
 </div>
 <div class="lender-compact-bottom">
  <button class="small danger remove" type="button">内容クリア</button>
  <div class="statusline"><span>${paid?'この月の返済は減算済み':'返済後にボタンを押すと残額から減算'}</span><strong>残額 ${fmt(x.remaining)}</strong></div>
 </div>
 </div>`;
}
function renderLenders(){
 const registered=state.lenders.filter(x=>String(x.name||'').trim()).length;
 $('lenderCount').textContent=`${registered}/${MAX_LENDERS}`;
 $('addLender').disabled=state.lenders.length>=MAX_LENDERS;
 $('addLender').title=state.lenders.length>=MAX_LENDERS?'13件まで登録済みです':'';
 const sortedLenders=[...state.lenders].sort((a,b)=>{
  const payA=paymentFor(a,a.repayMonth||ymNow());
  const payB=paymentFor(b,b.repayMonth||ymNow());
  if(payB!==payA)return payB-payA;
  // 同額なら返済残額の多い順、その次に名称順。
  const remDiff=num(b.remaining)-num(a.remaining);
  if(remDiff!==0)return remDiff;
  return String(a.name||'').localeCompare(String(b.name||''),'ja');
 });
 $('lenderRows').innerHTML=sortedLenders.length?sortedLenders.map((x,i)=>lenderHtml(x,i)).join(''):'<div class="empty">借入先がまだ登録されていません</div>';
 $('lenderGrandTotal').textContent=fmt(state.lenders.reduce((s,x)=>s+num(x.totalRepayment),0));
 $('lenderRemainTotal').textContent=fmt(state.lenders.reduce((s,x)=>s+num(x.remaining),0));
 $('lenderRows').querySelectorAll('.lender').forEach(card=>bindLender(card));
}
function bindLender(card){
 const id=card.dataset.id,x=state.lenders.find(v=>v.id===id);if(!x)return;
 const title=card.querySelector('.editable-title');
 const editName=()=>{const v=prompt('借入先名称を編集',x.name||'');if(v===null)return;x.name=v.trim();persist();renderLenders()};
 title.addEventListener('click',editName);
 title.addEventListener('keydown',e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();editName()}});
 card.querySelectorAll('.money').forEach(el=>el.addEventListener('blur',()=>{const v=num(el.value);el.value=v?Math.round(v).toLocaleString('ja-JP'):''}));
 const set=(sel,key,number=false)=>card.querySelector(sel).addEventListener('input',e=>{
  x[key]=number?num(e.target.value):e.target.value;
  persist();
  if(key==='remaining'){
   $('lenderRemainTotal').textContent=fmt(state.lenders.reduce((s,v)=>s+num(v.remaining),0));
   card.querySelector('.statusline strong').textContent='残額 '+fmt(x.remaining);
  }
  if(key==='totalRepayment')$('lenderGrandTotal').textContent=fmt(state.lenders.reduce((s,v)=>s+num(v.totalRepayment),0));
 });
 set('.account','account');set('.totalRepayment','totalRepayment',true);set('.remaining','remaining',true);set('.finish','finishMonth');
 card.querySelector('.repay').addEventListener('input',e=>{
  const ym=x.repayMonth||ymNow();
  const val=num(e.target.value);
  x.schedule=x.schedule||{};
  x.schedule[ym]=val;
  x.monthlyPayment=val;
  persist();
 });
 card.querySelector('.copy').onclick=()=>copyText(String(paymentFor(x,x.repayMonth||ymNow())));
 card.querySelector('.repayMonth').onchange=e=>{x.repayMonth=e.target.value||ymNow();persist();renderLenders()};
 card.querySelector('.pay').onclick=()=>applyPayment(x);
 card.querySelector('.remove').onclick=()=>{if(confirm(`${x.name||'この借入先'}の内容をクリアしますか？`)){x.name='';x.account='';x.totalRepayment=0;x.monthlyPayment=0;x.remaining=0;x.finishMonth='';x.repayMonth=ymNow();x.paidMonths=[];x.schedule={};persist();renderLenders()}};
}
async function copyText(text){
 try{await navigator.clipboard.writeText(text);toast('返済金額をコピーしました')}
 catch(e){const ta=document.createElement('textarea');ta.value=text;document.body.appendChild(ta);ta.select();document.execCommand('copy');ta.remove();toast('返済金額をコピーしました')}
}
function applyPayment(x){
 const ym=x.repayMonth||ymNow();
 x.paidMonths=Array.isArray(x.paidMonths)?x.paidMonths:[];
 if(x.paidMonths.includes(ym)){toast('この月は返済済みです');return}
 const p=paymentFor(x,ym);
 if(p<=0){alert('この月の返済金額を入力してください');return}
 if(!confirm(`${monthLabel(ym)} ${fmt(p)} を返済残額から減算しますか？`))return;
 x.remaining=Math.max(0,num(x.remaining)-p);
 x.paidMonths.push(ym);
 if(x.remaining===0&&!x.finishMonth)x.finishMonth=ym;
 persist();renderLenders();toast('返済残額を更新しました');
}
function syncDebts(){
 const lenderIds=new Set(state.lenders.map(x=>x.id));
 state.debts=state.debts.filter(d=>d.source!=='lender'||lenderIds.has(d.lenderId));
 for(const l of state.lenders){
  if(!String(l.name||'').trim())continue;
  let d=state.debts.find(x=>x.source==='lender'&&x.lenderId===l.id);
  if(!d){d={id:uid(),name:l.name,value:num(l.remaining),source:'lender',lenderId:l.id};state.debts.push(d)}
  else{d.name=l.name;d.value=num(l.remaining)}
 }
 persist();renderInputs();toast('借入タブへ反映しました');
}
function normalizeTransferText(s){
 return String(s||'').normalize('NFKC').toUpperCase().replace(/[\s　＊*・\.\-ー_()（）株式会社合同会社]/g,'').replace(/シユ/g,'シュ').replace(/ジヤ/g,'ジャ').replace(/フア/g,'ファ').replace(/カイシユウ/g,'カイシュウ');
}
function aliasesFor(l){
 const a=TRANSFER_ALIASES[l.id]||[];
 if(a.length)return a.map(normalizeTransferText);
 const n=normalizeTransferText(l.name);
 return n?[n]:[];
}
function decodeCsvBuffer(buf){
 const bytes=new Uint8Array(buf);
 // UTF-8 BOM or valid-looking UTF-8 first; SMBC CSV is commonly Shift_JIS/CP932.
 if(bytes[0]===0xEF&&bytes[1]===0xBB&&bytes[2]===0xBF)return new TextDecoder('utf-8').decode(bytes);
 try{
  const utf=new TextDecoder('utf-8',{fatal:true}).decode(bytes);
  if(utf.includes('日付')||utf.includes('出金金額'))return utf;
 }catch(e){}
 try{return new TextDecoder('shift_jis').decode(bytes)}catch(e){return new TextDecoder('utf-8').decode(bytes)}
}
function csvDateToYm(v){
 const m=String(v||'').match(/(\d{4})[\/-](\d{1,2})/);return m?`${m[1]}-${String(m[2]).padStart(2,'0')}`:'';
}
function parseTransferCsv(text){
 const rows=parseCsv(text.replace(/^\uFEFF/,''));if(!rows.length)throw new Error('CSVが空です');
 const head=rows.shift().map(x=>String(x).trim());
 const idxDate=head.findIndex(x=>x==='日付');
 const idxContent=head.findIndex(x=>x==='内容');
 const idxOut=head.findIndex(x=>x.includes('出金金額'));
 if(idxDate<0||idxContent<0||idxOut<0)throw new Error('SMBC取引明細CSVの列を確認できません');
 return rows.map((r,i)=>({
  id:i,date:String(r[idxDate]||'').trim(),ym:csvDateToYm(r[idxDate]),content:String(r[idxContent]||'').trim(),amount:num(r[idxOut]),used:false
 })).filter(x=>x.content&&x.content!=='振込手数料'&&x.amount>0);
}
function verifyTransfers(){
 const ym=$('verifyMonth').value||transferCheck.month||ymNow();
 transferCheck.month=ym;
 const tx=transferCheck.rows.filter(r=>r.ym===ym).map(r=>({...r,used:false,norm:normalizeTransferText(r.content)}));
 const lenders=state.lenders.filter(l=>l.id!=='seed-01'&&String(l.name||'').trim());
 const results=lenders.map(l=>({l,expected:paymentFor(l,ym),status:'pending',tx:null,actual:0,note:''}));
 // 1) recipient + amount exact matches first.
 for(const res of results.filter(r=>r.expected>0)){
  const aliases=aliasesFor(res.l);
  const hit=tx.find(t=>!t.used&&t.amount===res.expected&&aliases.some(a=>a&&t.norm.includes(a)));
  if(hit){hit.used=true;res.status='ok';res.tx=hit;res.actual=hit.amount;}
 }
 // 2) remaining: recipient found but amount differs.
 for(const res of results.filter(r=>r.expected>0&&r.status==='pending')){
  const aliases=aliasesFor(res.l);
  const hit=tx.find(t=>!t.used&&aliases.some(a=>a&&t.norm.includes(a)));
  if(hit){hit.used=true;res.status='amount';res.tx=hit;res.actual=hit.amount;res.note='振込先名は一致していますが金額が違います';}
 }
 // 3) remaining: same amount exists under another recipient = possible destination mismatch.
 for(const res of results.filter(r=>r.expected>0&&r.status==='pending')){
  const hit=tx.find(t=>!t.used&&t.amount===res.expected);
  if(hit){hit.used=true;res.status='destination';res.tx=hit;res.actual=hit.amount;res.note='金額は一致しますが振込先名が一致しません';}
  else{res.status='missing';res.note='該当する振込を確認できません';}
 }
 // 4) zero-payment lenders require no transfer.
 for(const res of results.filter(r=>r.expected<=0)){res.status='none';res.note='この月は返済予定額が0円です';}
 transferCheck.results=results;
 renderTransferCheck();
}
function renderTransferCheck(){
 const ym=transferCheck.month||$('verifyMonth').value||ymNow();
 $('transferMonthLabel').textContent=transferCheck.fileName?`${Number(ym.slice(5,7))}月・${transferCheck.fileName}`:'未読込';
 const req=transferCheck.results.filter(r=>r.expected>0);
 const ok=req.filter(r=>r.status==='ok').length;
 const ng=req.length-ok;
 $('verifyRequired').textContent=req.length;
 $('verifyOk').textContent=ok;
 $('verifyNg').textContent=ng;
 if(!transferCheck.results.length){$('verifyStatus').className='empty';$('verifyStatus').textContent='CSVを選択すると照合結果を表示します';$('verifyRows').innerHTML='';$('applyVerifiedPayments').disabled=true;return}
 const allOk=req.length>0&&ng===0;
 $('verifyStatus').className=allOk?'empty success':'empty';
 $('verifyStatus').innerHTML=allOk?`✅ ${ok}/${req.length}件、振込先名・金額とも一致しました`:`${ok}/${req.length}件一致・${ng}件は確認が必要です`;
 const order={ok:0,amount:1,destination:2,missing:3,none:4};
 const sorted=[...transferCheck.results].sort((a,b)=>(order[a.status]-order[b.status])||(b.expected-a.expected));
 const badge={ok:['badge-ok','一致'],amount:['badge-warn','金額違い'],destination:['badge-bad','振込先違い'],missing:['badge-bad','未確認'],none:['badge-none','支払なし']};
 $('verifyRows').innerHTML=sorted.map(r=>{
  const [cls,txt]=badge[r.status];
  const paid=(r.l.paidMonths||[]).includes(ym);
  const txDetail=r.tx?`CSV：${esc(r.tx.content)} ／ ${fmt(r.tx.amount)}`:'CSV：該当なし';
  const paidText=paid?' ／ この月はHTML上で返済済み':'';
  return `<div class="verify-item"><div class="verify-head"><div class="verify-name">${esc(r.l.name)}</div><span class="${cls}">${txt}</span></div><div class="verify-detail">予定：${fmt(r.expected)}${paidText}<br>${txDetail}${r.note?`<br>${esc(r.note)}`:''}</div></div>`;
 }).join('');
 $('applyVerifiedPayments').disabled=!allOk||req.every(r=>(r.l.paidMonths||[]).includes(ym));
}
async function loadTransferCsv(file){
 try{
  const buf=await file.arrayBuffer();
  const text=decodeCsvBuffer(buf);
  const rows=parseTransferCsv(text);
  if(!rows.length)throw new Error('振込データが見つかりません');
  transferCheck.rows=rows;transferCheck.fileName=file.name;
  const months={};for(const r of rows){if(r.ym)months[r.ym]=(months[r.ym]||0)+1}
  const detected=Object.entries(months).sort((a,b)=>b[1]-a[1])[0]?.[0];
  if(detected){transferCheck.month=detected;$('verifyMonth').value=detected}
  verifyTransfers();toast('振込CSVを読み込みました');
 }catch(e){alert('CSVを読み込めませんでした：'+(e.message||e));}
}
function applyVerifiedPayments(){
 const ym=transferCheck.month||$('verifyMonth').value||ymNow();
 const req=transferCheck.results.filter(r=>r.expected>0);
 if(!req.length||req.some(r=>r.status!=='ok')){alert('全件一致してから一括反映してください');return}
 const targets=req.filter(r=>!(r.l.paidMonths||[]).includes(ym));
 if(!targets.length){toast('すでに返済反映済みです');return}
 const total=targets.reduce((s,r)=>s+r.expected,0);
 if(!confirm(`${Number(ym.slice(5,7))}月分 ${targets.length}件・合計 ${fmt(total)} を返済残額から減算しますか？`))return;
 for(const r of targets){
  const l=r.l;l.paidMonths=Array.isArray(l.paidMonths)?l.paidMonths:[];
  if(l.paidMonths.includes(ym))continue;
  l.remaining=Math.max(0,num(l.remaining)-r.expected);l.paidMonths.push(ym);if(l.remaining===0&&!l.finishMonth)l.finishMonth=ym;
 }
 persist();renderLenders();syncDebts();verifyTransfers();toast('確認済み返済を一括反映しました');
}
function clearTransferCheck(){transferCheck={rows:[],results:[],month:$('verifyMonth').value||ymNow(),fileName:''};$('transferCsv').value='';renderTransferCheck();}

function saveRecord(){
 const date=$('recordDate').value;if(!date)return;
 const t=totals(),rec={date,assets:clone(state.assets),debts:clone(state.debts),asset:t.a,debt:t.d,net:t.n};
 const i=state.records.findIndex(r=>r.date===date);
 if(i>=0)state.records[i]=rec;else state.records.push(rec);
 state.records.sort((a,b)=>a.date.localeCompare(b.date));persist();renderAll();toast('残高を記録しました');
}
function renderHistory(){
 const asc=[...state.records].sort((a,b)=>a.date.localeCompare(b.date));
 const prevByDate=new Map();
 asc.forEach((r,i)=>{
   const prev=i>0?asc[i-1]:null;
   prevByDate.set(r.date,prev?num(r.net)-num(prev.net):null);
 });
 const rows=[...asc].reverse();
 $('historyBody').innerHTML=rows.length?rows.map(r=>{
   const idx=state.records.findIndex(x=>x.date===r.date);
   const diff=prevByDate.get(r.date);
   const diffHtml=diff===null?'—':`<span class="history-diff ${diff>=0?'pos':'neg'}">${diff>=0?'+':''}${fmt(diff)}</span>`;
   return `<tr><td class="history-date longpress-ready" data-i="${idx}" data-date="${esc(r.date)}">${r.date}<span class="history-mobile-diff">前回比 ${diff===null?'—':`${diff>=0?'+':''}${fmt(diff)}`}</span></td><td>${fmt(r.asset)}</td><td>${fmt(r.debt)}</td><td>${fmt(r.net)}</td><td>${diffHtml}</td><td><button class="small edit" data-i="${idx}">編集</button></td></tr>`
 }).join(''):`<tr><td colspan="6" class="muted" style="text-align:center;padding:18px">まだ記録がありません</td></tr>`;
 $('historyBody').querySelectorAll('.edit').forEach(b=>b.onclick=()=>openEdit(Number(b.dataset.i)));
 bindHistoryLongPress();
}
function bindHistoryLongPress(){
 $('historyBody').querySelectorAll('.history-date').forEach(cell=>{
   let timer=null, fired=false;
   const cancel=()=>{if(timer){clearTimeout(timer);timer=null}};
   const start=e=>{
     fired=false;cancel();
     timer=setTimeout(()=>{
       fired=true;
       const idx=Number(cell.dataset.i);
       const rec=state.records[idx];
       if(!rec)return;
       if(confirm(`${rec.date} の記録を削除しますか？`)){
         state.records.splice(idx,1);
         persist();
         renderAll();
         toast('記録を削除しました');
       }
     },650);
   };
   cell.addEventListener('pointerdown',start);
   cell.addEventListener('pointerup',cancel);
   cell.addEventListener('pointercancel',cancel);
   cell.addEventListener('pointerleave',cancel);
   cell.addEventListener('contextmenu',e=>{e.preventDefault()});
 });
}

/* ---- Server sync ---- */
const SERVER_URL_KEY='asset_tracker_server_url';
const SERVER_TOKEN_KEY='asset_tracker_server_token';
const DEFAULT_SERVER_URL='https://assets-management.45kikurage.workers.dev';

function getServerConfig(){
 return {
   url:(localStorage.getItem(SERVER_URL_KEY)||DEFAULT_SERVER_URL).replace(/\/+$/,''),
   token:localStorage.getItem(SERVER_TOKEN_KEY)||''
 };
}
function configureServer(){
 const current=getServerConfig();
 const token=prompt('初回設定：サーバー接続キーを入力してください',current.token||'');
 if(token===null)return null;
 if(!token.trim()){
   alert('サーバー接続キーを入力してください');
   return null;
 }
 localStorage.setItem(SERVER_URL_KEY,DEFAULT_SERVER_URL);
 localStorage.setItem(SERVER_TOKEN_KEY,token.trim());
 return {url:DEFAULT_SERVER_URL,token:token.trim()};
}
async function serverRequest(method,body){
 let cfg=getServerConfig();
 if(!cfg.token){cfg=configureServer();if(!cfg)throw new Error('サーバー設定をキャンセルしました')}
 const res=await fetch(cfg.url+'/api/state',{
   method,
   headers:{'Content-Type':'application/json','Authorization':'Bearer '+cfg.token},
   body:body?JSON.stringify(body):undefined,
   cache:'no-store'
 });
 if(res.status===401)throw new Error('サーバー接続キーが一致しません。データ送信ボタンを長押しして再設定してください');
 if(!res.ok)throw new Error('サーバー通信エラー '+res.status);
 return await res.json();
}
async function saveStateToServer(){
 const btn=$('serverSaveFab');
 if(!confirm('現在の資産管理データをサーバーへ送信しますか？'))return;
 try{
   btn.classList.add('saving');btn.classList.remove('saved');btn.textContent='送信中…';
   const payload={
     state:clone(state),
     monthGoal:Number(localStorage.getItem('asset_tracker_month_goal')||0),
     clientSavedAt:new Date().toISOString()
   };
   await serverRequest('PUT',payload);
   btn.classList.remove('saving');btn.classList.add('saved');btn.textContent='送信済';
   toast('サーバーへデータを送信しました');
   setTimeout(()=>{btn.classList.remove('saved');btn.textContent='送信'},1800);
 }catch(e){
   btn.classList.remove('saving');btn.textContent='送信';
   alert(e.message||'データ送信に失敗しました');
 }
}
async function loadStateFromServer(options={}){
 const {manual=false,silent=false}=options;
 const cfg=getServerConfig();
 if(!cfg.token){
   if(!manual)return;
 }
 if(manual && !confirm('サーバーに保存されているデータをこの端末へ反映しますか？\n現在の端末データはサーバーデータで上書きされます。'))return;
 const btn=$('serverLoadFab');
 try{
   if(btn && manual){btn.classList.add('saving');btn.classList.remove('saved');btn.textContent='取得中…'}
   const data=await serverRequest('GET');
   if(!data||!data.exists||!data.payload||!data.payload.state){
     if(manual)alert('サーバーに保存データがまだありません');
     return;
   }
   state=normalize(data.payload.state);
   if(Number.isFinite(Number(data.payload.monthGoal))){
     localStorage.setItem('asset_tracker_month_goal',String(Number(data.payload.monthGoal)));
   }
   persist();
   syncCalendarMonth();
   renderAll();
   renderTransferCheck();
   if(btn && manual){
     btn.classList.remove('saving');btn.classList.add('saved');btn.textContent='取得済';
     setTimeout(()=>{btn.classList.remove('saved');btn.textContent='取得'},1800);
   }
   if(!silent)toast('サーバーのデータを読み込みました');
 }catch(e){
   if(btn){btn.classList.remove('saving');btn.textContent='取得'}
   if(manual)alert(e.message||'データ取得に失敗しました');
   else console.warn('Server load skipped:',e);
 }
}

let editingIndex=-1;
function openEdit(i){
 editingIndex=i;const r=state.records[i];
 $('editDate').value=r.date;
 $('editAssets').innerHTML='<h4>資産</h4>'+r.assets.map(x=>rowHtml(x,true)).join('');
 $('editDebts').innerHTML='<h4>借入</h4>'+r.debts.map(x=>rowHtml(x)).join('');
 $('editAssets').querySelectorAll('.del,.name').forEach(el=>el.disabled=true);
 $('editDebts').querySelectorAll('.del,.name').forEach(el=>el.disabled=true);
 $('editModal').classList.add('open');
}
function saveEditFn(){
 const r=state.records[editingIndex];
 r.date=$('editDate').value;
 const assetValues=[...$('editAssets').querySelectorAll('.amt')].map(el=>calculateAmount(el.value));
 const debtValues=[...$('editDebts').querySelectorAll('.amt')].map(el=>num(el.value));
 if(assetValues.some(result=>!result.ok))return alert('計算式を確認してください');
 r.assets.forEach((x,i)=>x.value=assetValues[i].value);
 r.debts.forEach((x,i)=>x.value=debtValues[i]);
 r.asset=r.assets.reduce((s,x)=>s+num(x.value),0);r.debt=r.debts.reduce((s,x)=>s+num(x.value),0);r.net=r.asset-r.debt;
 state.records.sort((a,b)=>a.date.localeCompare(b.date));persist();$('editModal').classList.remove('open');renderAll();
}

function monthlyDeltaData(){
 const now=new Date();
 const y=now.getFullYear(),m=now.getMonth()+1;
 const ym=`${y}-${String(m).padStart(2,'0')}`;
 const start=`${ym}-01`;
 const recs=[...state.records].sort((a,b)=>a.date.localeCompare(b.date));
 const prior=recs.filter(r=>r.date<start);
 const baseRec=prior.length?prior[prior.length-1]:null;
 const base=baseRec?Number(baseRec.net)||0:null;
 const monthRecs=recs.filter(r=>r.date>=start&&r.date.slice(0,7)===ym);
 const points=[];
 if(base!==null)points.push({date:start,delta:0,synthetic:true});
 if(base!==null){
  for(const r of monthRecs)points.push({date:r.date,delta:(Number(r.net)||0)-base,synthetic:false});
 }
 return {ym,start,base,baseRec,points,monthRecs};
}
function drawMonthlyDeltaChart(){
 const c=$('monthDeltaChart');if(!c)return;
 const ctx=c.getContext('2d'),dpr=devicePixelRatio||1,rect=c.getBoundingClientRect();if(rect.width<10)return;
 c.width=Math.max(1,Math.round(rect.width*dpr));c.height=Math.max(1,Math.round(rect.height*dpr));ctx.setTransform(dpr,0,0,dpr,0,0);ctx.clearRect(0,0,rect.width,rect.height);
 const d=monthlyDeltaData();
 const monthNum=Number(d.ym.slice(5,7));
 $('monthChartPeriod').textContent=`${monthNum}月1日 → 本日`;
 if(d.base===null){
  $('monthBaseText').textContent='前月末基準：基準となる過去記録がありません';
  $('monthDeltaNow').textContent='—';$('monthDeltaNow').className='month-delta-now';
  ctx.fillStyle='rgba(255,255,255,.72)';ctx.font='14px sans-serif';ctx.fillText('前月末以前の記録を1件保存すると表示できます',18,34);return;
 }
 $('monthBaseText').textContent=`前月末基準：${fmt(d.base)}（${d.baseRec.date}の最新記録）`;
 const last=d.points.length?d.points[d.points.length-1]:{delta:0};
 const current=last.delta||0;
 $('monthDeltaNow').textContent=`${current>0?'+':current<0?'−':'±'}${Math.abs(Math.round(current)).toLocaleString('ja-JP')}円`;
 $('monthDeltaNow').className='month-delta-now '+(current>0?'plus':current<0?'minus':'');
 const points=d.points;
 const pad={l:66,r:14,t:18,b:34},w=rect.width-pad.l-pad.r,h=rect.height-pad.t-pad.b;
 if(points.length<2){
  ctx.strokeStyle='rgba(255,255,255,.18)';ctx.beginPath();ctx.moveTo(pad.l,pad.t+h/2);ctx.lineTo(pad.l+w,pad.t+h/2);ctx.stroke();
  ctx.fillStyle='rgba(255,255,255,.72)';ctx.font='13px sans-serif';ctx.fillText('今月の残高を記録すると増減が表示されます',18,34);return;
 }
 let vals=points.map(p=>p.delta);let min=Math.min(0,...vals),max=Math.max(0,...vals);let span=max-min;if(span===0)span=1;
 const margin=Math.max(span*.14,1000);min-=margin;max+=margin;span=max-min;
 ctx.font='11px sans-serif';ctx.textBaseline='middle';
 for(let i=0;i<5;i++){
  const v=max-span*i/4,y=pad.t+h*i/4;
  ctx.strokeStyle='rgba(255,255,255,.12)';ctx.lineWidth=1;ctx.beginPath();ctx.moveTo(pad.l,y);ctx.lineTo(pad.l+w,y);ctx.stroke();
  ctx.fillStyle='rgba(255,255,255,.68)';const txt=(v>=0?'+':'−')+Math.abs(Math.round(v)).toLocaleString('ja-JP');ctx.fillText(txt,4,y);
 }
 const zeroY=pad.t+h*(max-0)/span;ctx.strokeStyle='rgba(246,199,25,.58)';ctx.lineWidth=1.5;ctx.setLineDash([5,4]);ctx.beginPath();ctx.moveTo(pad.l,zeroY);ctx.lineTo(pad.l+w,zeroY);ctx.stroke();ctx.setLineDash([]);
 const dayOf=date=>Number(date.slice(8,10));
 const daysInMonth=new Date(Number(d.ym.slice(0,4)),Number(d.ym.slice(5,7)),0).getDate();
 const xOf=p=>pad.l+w*(dayOf(p.date)-1)/Math.max(1,daysInMonth-1);
 const yOf=p=>pad.t+h*(max-p.delta)/span;
 const grad=ctx.createLinearGradient(0,pad.t,0,pad.t+h);grad.addColorStop(0,'rgba(246,199,25,.22)');grad.addColorStop(1,'rgba(246,199,25,0)');
 ctx.beginPath();points.forEach((p,i)=>{const x=xOf(p),y=yOf(p);i?ctx.lineTo(x,y):ctx.moveTo(x,y)});ctx.lineTo(xOf(points.at(-1)),zeroY);ctx.lineTo(xOf(points[0]),zeroY);ctx.closePath();ctx.fillStyle=grad;ctx.fill();
 ctx.strokeStyle='#f6c719';ctx.lineWidth=3;ctx.lineJoin='round';ctx.lineCap='round';ctx.beginPath();points.forEach((p,i)=>{const x=xOf(p),y=yOf(p);i?ctx.lineTo(x,y):ctx.moveTo(x,y)});ctx.stroke();
 points.slice(1).forEach(p=>{const x=xOf(p),y=yOf(p);ctx.beginPath();ctx.arc(x,y,4,0,Math.PI*2);ctx.fillStyle=p.delta>=0?'#35e5d2':'#ff8c95';ctx.fill();ctx.strokeStyle='rgba(255,255,255,.85)';ctx.lineWidth=1;ctx.stroke()});
 ctx.fillStyle='rgba(255,255,255,.72)';ctx.textBaseline='alphabetic';ctx.font='11px sans-serif';
 const ticks=[1,Math.min(10,daysInMonth),Math.min(20,daysInMonth),daysInMonth];
 [...new Set(ticks)].forEach(day=>{const x=pad.l+w*(day-1)/Math.max(1,daysInMonth-1);ctx.fillText(`${day}日`,Math.max(pad.l-2,Math.min(x-10,pad.l+w-22)),rect.height-9)});
}
let homeTrendDays=90;
let homeTrendMode='month';

function getCurrentMonthNetDelta(){
 const now=new Date();
 const y=now.getFullYear(),m=now.getMonth();
 const monthStart=new Date(y,m,1);
 const monthEnd=new Date(y,m+1,0);
 const prevEnd=new Date(y,m,0);
 const iso=d=>`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
 const startIso=iso(monthStart),endIso=iso(monthEnd),prevEndIso=iso(prevEnd);
 const recs=[...state.records].sort((a,b)=>a.date.localeCompare(b.date));

 // 基準1: 前月末。無ければ前月末から3日以内で最も月末に近い記録。
 let baseRec=null,baseType='none';
 for(let back=0;back<=3;back++){
   const d=new Date(prevEnd); d.setDate(d.getDate()-back);
   const key=iso(d);
   const found=[...recs].reverse().find(r=>r.date===key);
   if(found){baseRec=found;baseType=back===0?'prev-end':'prev-near';break;}
 }
 // 基準2: 上記が無ければ、当月の月初に最も近い（=当月最初の）記録。
 const monthRecs=recs.filter(r=>r.date>=startIso&&r.date<=endIso);
 if(!baseRec && monthRecs.length){baseRec=monthRecs[0];baseType='month-first';}
 const base=baseRec?(Number(baseRec.net)||0):null;
 const points=[];
 if(base!==null){
   // 前月データを基準にする場合は「先月末=0」の開始点を置く。
   // 当月最初の記録を基準にする場合は、その実日付を0とする。
   points.push({date:baseType==='month-first'?baseRec.date:prevEndIso,delta:0,synthetic:true});
   for(const r of monthRecs){
     if(baseType==='month-first' && r.date<baseRec.date)continue;
     points.push({date:r.date,delta:(Number(r.net)||0)-base,synthetic:false});
   }
 }
 // 同一日が0点と実測点で重なる場合、実測点を優先して1点にまとめる。
 const byDate=new Map(); for(const p of points)byDate.set(p.date,p);
 const merged=[...byDate.values()].sort((a,b)=>a.date.localeCompare(b.date));
 return {y,m,monthStart,monthEnd,prevEnd,startIso,endIso,prevEndIso,baseRec,baseType,base,monthRecs,points:merged};
}

function drawHomeMonthNetChart(){
 const split=document.querySelector('#page-home .home-split-charts');
 const assetPanel=document.querySelector('#page-home .home-asset-panel');
 const netPanel=document.querySelector('#page-home .home-net-panel');
 if(split)split.classList.add('month-mode');
 if(assetPanel)assetPanel.setAttribute('aria-hidden','true');
 if(netPanel)netPanel.removeAttribute('aria-hidden');
 const netLabel=$('homeNetLabel');if(netLabel)netLabel.textContent='純資産増減（先月末比）';
 const d=getCurrentMonthNetDelta();
 const latestPoint=d.points.length?d.points[d.points.length-1]:null;
 const latestDelta=latestPoint?Number(latestPoint.delta)||0:0;
 const nLatest=$('homeNetLatest');
 if(nLatest)nLatest.textContent=d.base===null?'—':`${latestDelta>0?'+':latestDelta<0?'−':'±'}¥${Math.abs(Math.round(latestDelta)).toLocaleString('ja-JP')}`;

 const c=$('homeNetChart');if(!c)return;
 const ctx=c.getContext('2d'),dpr=devicePixelRatio||1,rect=c.getBoundingClientRect();if(rect.width<10)return;
 c.width=Math.max(1,Math.round(rect.width*dpr));c.height=Math.max(1,Math.round(rect.height*dpr));ctx.setTransform(dpr,0,0,dpr,0,0);ctx.clearRect(0,0,rect.width,rect.height);
 const box=c.closest('.home-trend-box');if(box)box.classList.toggle('is-empty',d.base===null||d.points.length<2);
 if(d.base===null){
   ctx.fillStyle='rgba(255,255,255,.72)';ctx.font='12px sans-serif';
   ctx.fillText('基準データがありません',12,28);
   ctx.font='10px sans-serif';ctx.fillText('前月末付近または当月月初の記録が必要です',12,48);return;
 }
 const pts=d.points;
 if(pts.length<2){
   ctx.strokeStyle='rgba(255,255,255,.15)';ctx.beginPath();ctx.moveTo(52,rect.height/2);ctx.lineTo(rect.width-10,rect.height/2);ctx.stroke();
   ctx.fillStyle='rgba(255,255,255,.72)';ctx.font='11px sans-serif';ctx.fillText('当月の残高を記録すると増減が表示されます',12,28);return;
 }
 let vals=pts.map(p=>p.delta);let min=Math.min(0,...vals),max=Math.max(0,...vals);let span=max-min;if(span===0)span=1;
 const margin=Math.max(span*.14,1000);min-=margin;max+=margin;span=max-min;
 const pad={l:64,r:12,t:10,b:28},w=rect.width-pad.l-pad.r,h=rect.height-pad.t-pad.b;
 ctx.font='10px sans-serif';ctx.textBaseline='middle';
 for(let i=0;i<5;i++){
   const v=max-span*i/4,y=pad.t+h*i/4;
   ctx.strokeStyle='rgba(255,255,255,.11)';ctx.lineWidth=1;ctx.beginPath();ctx.moveTo(pad.l,y);ctx.lineTo(pad.l+w,y);ctx.stroke();
   ctx.fillStyle='rgba(255,255,255,.70)';const txt=(v>0?'+':v<0?'−':'±')+Math.abs(Math.round(v)).toLocaleString('ja-JP');ctx.fillText(txt,2,y);
 }
 const zeroY=pad.t+h*(max/span);
 ctx.strokeStyle='rgba(246,199,25,.52)';ctx.lineWidth=1.4;ctx.setLineDash([5,4]);ctx.beginPath();ctx.moveTo(pad.l,zeroY);ctx.lineTo(pad.l+w,zeroY);ctx.stroke();ctx.setLineDash([]);
 const totalMs=d.monthEnd-d.prevEnd;
 const xOf=p=>pad.l+w*Math.max(0,Math.min(1,(new Date(p.date+'T00:00:00')-d.prevEnd)/Math.max(1,totalMs)));
 const yOf=p=>pad.t+h*(max-p.delta)/span;
 ctx.strokeStyle='#22c8e5';ctx.lineWidth=3;ctx.lineJoin='round';ctx.lineCap='round';ctx.beginPath();pts.forEach((p,i)=>{const x=xOf(p),y=yOf(p);i?ctx.lineTo(x,y):ctx.moveTo(x,y)});ctx.stroke();
 pts.forEach((p,i)=>{if(i===0&&p.synthetic)return;const x=xOf(p),y=yOf(p);ctx.beginPath();ctx.arc(x,y,4,0,Math.PI*2);ctx.fillStyle=p.delta>=0?'#35e5d2':'#ff8c95';ctx.fill();ctx.strokeStyle='rgba(255,255,255,.85)';ctx.lineWidth=1;ctx.stroke()});
 ctx.fillStyle='rgba(255,255,255,.72)';ctx.textBaseline='alphabetic';ctx.font='10px sans-serif';
 ctx.fillText('先月末',Math.max(2,pad.l-20),rect.height-7);
 const endLabel=`${String(d.m+1).padStart(2,'0')}/${String(d.monthEnd.getDate()).padStart(2,'0')}`;
 ctx.fillText(endLabel,Math.max(pad.l,rect.width-42),rect.height-7);
 // 基準が当月最初の記録なら、採用日を小さく明示。
 if(d.baseType==='month-first'){
   ctx.fillStyle='rgba(255,255,255,.55)';ctx.font='9px sans-serif';ctx.fillText(`基準:${d.baseRec.date.slice(5).replace('-','/')}`,pad.l+4,12);
 }
}

function drawHomeTrendChart(){
 const split=document.querySelector('#page-home .home-split-charts');
 const assetPanel=document.querySelector('#page-home .home-asset-panel');
 const netPanel=document.querySelector('#page-home .home-net-panel');
 const netLabel=$('homeNetLabel');if(netLabel)netLabel.textContent='純資産額';
 if(homeTrendMode==='month'){drawHomeMonthNetChart();return;}
 if(split)split.classList.remove('month-mode');
 if(assetPanel)assetPanel.removeAttribute('aria-hidden');
 if(netPanel)netPanel.removeAttribute('aria-hidden');
 const all=[...state.records].sort((a,b)=>a.date.localeCompare(b.date));
 let arr=all;
 if(all.length){const end=new Date(all[all.length-1].date+'T00:00:00');const start=new Date(end);start.setDate(start.getDate()-homeTrendDays);arr=all.filter(r=>new Date(r.date+'T00:00:00')>=start)}
 const latest=arr.length?arr[arr.length-1]:null;
 const aLatest=$('homeAssetLatest'),nLatest=$('homeNetLatest');
 if(aLatest)aLatest.textContent=fmt(latest?Number(latest.asset)||0:0);
 if(nLatest)nLatest.textContent=fmt(latest?Number(latest.net)||0:0);
 function drawOne(id,key,color){
  const c=$(id);if(!c)return;
  const ctx=c.getContext('2d'),dpr=devicePixelRatio||1,rect=c.getBoundingClientRect();if(rect.width<10)return;
  c.width=Math.max(1,Math.round(rect.width*dpr));c.height=Math.max(1,Math.round(rect.height*dpr));ctx.setTransform(dpr,0,0,dpr,0,0);ctx.clearRect(0,0,rect.width,rect.height);
  const box=c.closest('.home-trend-box');if(box)box.classList.toggle('is-empty',arr.length<2);
  ctx.font='10px sans-serif';ctx.fillStyle='rgba(255,255,255,.70)';
  if(arr.length<2){ctx.fillText('2件以上記録すると表示されます',10,24);return}
  const vals=arr.map(r=>Number(r[key])||0);let min=Math.min(...vals),max=Math.max(...vals);if(min===max){const d=Math.max(1,Math.abs(min)*.05);min-=d;max+=d}const raw=max-min;min-=raw*.10;max+=raw*.10;
  const pad={l:58,r:10,t:8,b:25},w=rect.width-pad.l-pad.r,h=rect.height-pad.t-pad.b;
  for(let i=0;i<3;i++){const y=pad.t+h*i/2,v=max-(max-min)*i/2;ctx.strokeStyle='rgba(255,255,255,.11)';ctx.lineWidth=1;ctx.beginPath();ctx.moveTo(pad.l,y);ctx.lineTo(pad.l+w,y);ctx.stroke();ctx.fillStyle='rgba(255,255,255,.70)';ctx.fillText(Math.round(v).toLocaleString('ja-JP'),2,y+4)}
  const xOf=i=>pad.l+w*i/(arr.length-1),yOf=v=>pad.t+h*(max-v)/(max-min);
  ctx.strokeStyle=color;ctx.lineWidth=2.5;ctx.lineJoin='round';ctx.lineCap='round';ctx.beginPath();arr.forEach((r,i)=>{const x=xOf(i),y=yOf(Number(r[key])||0);i?ctx.lineTo(x,y):ctx.moveTo(x,y)});ctx.stroke();
  arr.forEach((r,i)=>{const x=xOf(i),y=yOf(Number(r[key])||0);ctx.beginPath();ctx.arc(x,y,3,0,Math.PI*2);ctx.fillStyle=color;ctx.fill();ctx.strokeStyle='rgba(255,255,255,.75)';ctx.lineWidth=.8;ctx.stroke()});
  ctx.fillStyle='rgba(255,255,255,.70)';ctx.font='9px sans-serif';[0,arr.length-1].forEach(i=>{const x=xOf(i),lab=arr[i].date.slice(5).replace('-','/');ctx.fillText(lab,Math.max(pad.l-2,Math.min(x-10,pad.l+w-24)),rect.height-6)});
 }
 drawOne('homeAssetChart','asset','#f6c719');
 drawOne('homeNetChart','net','#22c8e5');
}
function updateHomeTimestamp(){const el=$('homeUpdated');if(!el)return;const d=new Date();el.textContent=`更新：${d.getFullYear()}/${String(d.getMonth()+1).padStart(2,'0')}/${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`}

function drawChart(){
 const c=$('chart'),ctx=c.getContext('2d'),dpr=devicePixelRatio||1,rect=c.getBoundingClientRect();if(rect.width<10)return;
 c.width=Math.max(1,rect.width*dpr);c.height=Math.max(1,rect.height*dpr);ctx.scale(dpr,dpr);ctx.clearRect(0,0,rect.width,rect.height);
 let arr=[...state.records].sort((a,b)=>a.date.localeCompare(b.date));if($('range').value!=='all')arr=arr.slice(-Number($('range').value));
 const pad={l:58,r:14,t:16,b:34},w=rect.width-pad.l-pad.r,h=rect.height-pad.t-pad.b;ctx.strokeStyle='#e5e7eb';ctx.fillStyle='#6b7280';ctx.font='12px sans-serif';
 if(arr.length<2){ctx.fillText('2件以上記録するとグラフが表示されます',18,30);return}
 let vals=arr.map(x=>x.net),min=Math.min(...vals),max=Math.max(...vals);if(min===max){min-=1;max+=1}
 for(let i=0;i<5;i++){const y=pad.t+h*i/4;ctx.beginPath();ctx.moveTo(pad.l,y);ctx.lineTo(pad.l+w,y);ctx.stroke();const v=max-(max-min)*i/4;ctx.fillText(Math.round(v).toLocaleString('ja-JP'),4,y+4)}
 ctx.strokeStyle='#2563eb';ctx.lineWidth=3;ctx.beginPath();arr.forEach((r,i)=>{const x=pad.l+w*i/(arr.length-1),y=pad.t+h*(max-r.net)/(max-min);i?ctx.lineTo(x,y):ctx.moveTo(x,y)});ctx.stroke();
 ctx.fillStyle='#6b7280';[0,Math.floor((arr.length-1)/2),arr.length-1].forEach(i=>{const x=pad.l+w*i/(arr.length-1);ctx.fillText(arr[i].date.slice(5),Math.max(pad.l,x-16),rect.height-10)});
}
function updateGlobalActionbar(name){
 const bar=$('globalActionbar'), assetBtn=$('assetSaveRecord'), lenderBtn=$('syncDebts');
 // ホームは閲覧専用なので操作バー自体を表示しない。
 bar.classList.toggle('home-hidden',name==='home');
 // その他のタブでは取得・送信を共通表示し、左端だけタブ別操作に切り替える。
 assetBtn.classList.toggle('visible',name==='assets');
 lenderBtn.classList.toggle('visible',name==='lenders');
}
function showTab(name){
 state.activeTab=name;persist();
 document.querySelectorAll('.tabpage').forEach(p=>p.classList.toggle('active',p.id===`page-${name}`));
 document.querySelectorAll('.navbtn').forEach(b=>b.classList.toggle('active',b.dataset.tab===name));
 updateGlobalActionbar(name);
 window.scrollTo({top:0,behavior:'auto'});if(name==='home')setTimeout(()=>{drawHomeTrendChart();updateHomeTimestamp()},0);if(name==='trend')setTimeout(drawChart,0);if(name==='transfer')setTimeout(renderTransferCheck,0);
}
function csvEscape(v){v=String(v??'');return /[",\n]/.test(v)?'"'+v.replace(/"/g,'""')+'"':v}
function exportCsvFn(){
 const head=['date','asset_total','debt_total','net_asset','assets_json','debts_json'];const lines=[head.join(',')];
 state.records.forEach(r=>lines.push([r.date,r.asset,r.debt,r.net,JSON.stringify(r.assets),JSON.stringify(r.debts)].map(csvEscape).join(',')));
 const blob=new Blob(['\ufeff'+lines.join('\n')],{type:'text/csv;charset=utf-8'}),a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download='asset_records.csv';a.click();setTimeout(()=>URL.revokeObjectURL(a.href),1000);
}
function parseCsv(text){
 const rows=[];let row=[],cur='',q=false;for(let i=0;i<text.length;i++){const ch=text[i];if(q){if(ch==='"'&&text[i+1]==='"'){cur+='"';i++}else if(ch==='"')q=false;else cur+=ch}else{if(ch==='"')q=true;else if(ch===','){row.push(cur);cur=''}else if(ch==='\n'){row.push(cur);rows.push(row);row=[];cur=''}else if(ch!=='\r')cur+=ch}}row.push(cur);if(row.some(x=>x!==''))rows.push(row);return rows;
}
async function importCsvFn(file){
 const rows=parseCsv(await file.text());const head=rows.shift();if(!head||head[0]!=='date')return alert('対応CSVではありません');
 const recs=[];for(const r of rows){try{recs.push({date:r[0],asset:num(r[1]),debt:num(r[2]),net:Number(r[3])||0,assets:JSON.parse(r[4]||'[]'),debts:JSON.parse(r[5]||'[]')})}catch(e){}}
 if(!recs.length)return alert('読み込める記録がありません');
 state.records=recs.sort((a,b)=>a.date.localeCompare(b.date));const last=state.records.at(-1);if(last){state.assets=clone(last.assets);state.debts=clone(last.debts)}persist();renderAll();toast('CSVを読み込みました');
}
function renderAll(){renderHomeGoal();renderInputs();renderLenders();renderHistory();updateTotals();drawMonthlyDeltaChart();drawHomeTrendChart();updateHomeTimestamp();if(state.activeTab==='trend')drawChart()}
$('addAsset').onclick=()=>{state.assets.push({id:uid(),name:'新しい資産',value:0});persist();renderInputs()};
$('addDebt').onclick=()=>{state.debts.push({id:uid(),name:'新しい借入',value:0,source:'manual'});persist();renderInputs()};
$('addLender').onclick=()=>{if(state.lenders.length>=MAX_LENDERS){toast('借入先は13件までです');return}state.lenders.push({id:uid(),name:'',account:'',totalRepayment:0,monthlyPayment:0,remaining:0,finishMonth:'',repayMonth:ymNow(),paidMonths:[],schedule:{}});persist();renderLenders()};
$('syncDebts').onclick=syncDebts;$('saveRecord').onclick=saveRecord;$('serverSaveFab').onclick=saveStateToServer;$('serverLoadFab').onclick=()=>loadStateFromServer({manual:true});
$('assetSaveRecord').onclick=()=>{
 const d=$('assetRecordDate').value||today();
 $('recordDate').value=d;
 saveRecord();
};$('transferCsv').onchange=e=>e.target.files[0]&&loadTransferCsv(e.target.files[0]);$('verifyMonth').onchange=e=>{transferCheck.month=e.target.value||ymNow();if(transferCheck.rows.length)verifyTransfers();else renderTransferCheck()};$('applyVerifiedPayments').onclick=applyVerifiedPayments;$('clearTransferCsv').onclick=clearTransferCheck;$('range').onchange=drawChart;
function renderHomeGoal(){
 const now=new Date(), month=now.getMonth()+1; const a=totals().a;
 const goal=Number(localStorage.getItem('asset_tracker_month_goal')||0);
 const gm=document.getElementById('goalMonthText'); if(gm)gm.textContent=month+'月';
 const ga=document.getElementById('goalAmountText'), gr=document.getElementById('goalRemainText'), gp=document.getElementById('goalPct'), gf=document.getElementById('goalFill');
 if(!ga)return; if(goal>0){const pct=Math.max(0,Math.min(100,a/goal*100));ga.textContent=fmt(goal);gr.textContent=fmt(Math.max(0,goal-a));gp.textContent=pct.toFixed(1)+'%';gf.style.width=pct+'%'}else{ga.textContent='未設定';gr.textContent='—';gp.textContent='—';gf.style.width='0%'}
}
document.querySelectorAll('[data-goto]').forEach(b=>b.addEventListener('click',()=>{const t=b.dataset.goto;document.querySelector('.navbtn[data-tab="'+t+'"]').click()}));
document.getElementById('editGoal')?.addEventListener('click',()=>{const cur=localStorage.getItem('asset_tracker_month_goal')||'';const v=prompt('今月の資産目標額を入力してください',cur);if(v===null)return;const n=Number(String(v).replace(/[^0-9.-]/g,''))||0;localStorage.setItem('asset_tracker_month_goal',String(n));renderHomeGoal()});
document.getElementById('homeRefresh')?.addEventListener('click',()=>{renderAll();toast('最新の表示に更新しました')});
document.querySelector('.goal-card')?.addEventListener('click',()=>document.getElementById('editGoal')?.click());
document.querySelectorAll('#homeRangeBtns button').forEach(b=>b.addEventListener('click',()=>{homeTrendMode=b.dataset.mode==='month'?'month':'range';if(homeTrendMode==='range')homeTrendDays=Number(b.dataset.days)||90;document.querySelectorAll('#homeRangeBtns button').forEach(x=>x.classList.toggle('active',x===b));drawHomeTrendChart()}));
window.addEventListener('resize',()=>{if(state.activeTab==='trend')drawChart();drawMonthlyDeltaChart();if(state.activeTab==='home')drawHomeTrendChart()});$('exportCsv').onclick=exportCsvFn;$('importCsv').onchange=e=>e.target.files[0]&&importCsvFn(e.target.files[0]);$('closeEdit').onclick=()=>$('editModal').classList.remove('open');$('saveEdit').onclick=saveEditFn;$('deleteEdit').onclick=()=>{if(confirm('この記録を削除しますか？')){state.records.splice(editingIndex,1);persist();$('editModal').classList.remove('open');renderAll()}};
document.querySelectorAll('.navbtn').forEach(b=>b.onclick=()=>showTab(b.dataset.tab));

let serverFabTimer=null;
$('serverSaveFab').addEventListener('pointerdown',()=>{
 serverFabTimer=setTimeout(()=>{
   serverFabTimer=null;
   if(confirm('サーバー接続キーを再設定しますか？'))configureServer();
 },900);
});
['pointerup','pointercancel','pointerleave'].forEach(ev=>$('serverSaveFab').addEventListener(ev,()=>{if(serverFabTimer){clearTimeout(serverFabTimer);serverFabTimer=null}}));

$('recordDate').value=today();$('assetRecordDate').value=today();$('todayLabel').textContent=today();$('verifyMonth').value=transferCheck.month||ymNow();renderAll();renderTransferCheck();state.activeTab='home';
homeTrendMode='month';
document.querySelectorAll('#homeRangeBtns button').forEach(b=>b.classList.toggle('active',b.dataset.mode==='month'));
showTab('home');
// 接続キー設定済みの端末だけ、起動時にサーバーの最新データを静かに取得します。
setTimeout(()=>loadStateFromServer({silent:true}),250);

function refreshForNewMonth(){
 if(syncCalendarMonth()){
  $('todayLabel').textContent=today();
  renderLenders();
  renderTransferCheck();
  toast(`${Number(ymNow().slice(5,7))}月の返済予定に自動更新しました`);
 }
}
document.addEventListener('visibilitychange',()=>{if(!document.hidden)refreshForNewMonth()});
window.addEventListener('focus',refreshForNewMonth);
setInterval(refreshForNewMonth,60*60*1000);

if('serviceWorker' in navigator && location.protocol!=='file:'){
 let reloadingForUpdate=false;
 navigator.serviceWorker.addEventListener('controllerchange',()=>{
  if(reloadingForUpdate)return;
  reloadingForUpdate=true;
  location.reload();
 });
 window.addEventListener('load',async()=>{
  try{
   const registration=await navigator.serviceWorker.register('./sw.js?v=20260901i',{updateViaCache:'none'});
   await registration.update();
  }catch(e){console.warn('Service Worker update skipped:',e)}
 });
}

})();
