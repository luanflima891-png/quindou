let currentUser=null, books=[], mode='login', filter='all', latest=null, busy=false;
const $=id=>document.getElementById(id);
const say=(message,error=false)=>{$('status').textContent=message||'';$('status').classList.toggle('error',error);};
function loading(value){busy=value;document.querySelectorAll('button').forEach(b=>b.disabled=value);}
function request(action,data={}){if(busy&&action!=='state')return;loading(true);say(action==='import'?'Copiando o PDF para sua biblioteca…':action==='signup'?'Criando sua conta…':'');if(!window.QuindouNative){loading(false);say('Esta interface deve ser aberta no aplicativo Android.',true);return;}window.QuindouNative.request(JSON.stringify({action,...data}));}
window.onNative=value=>{
 loading(false);if(value.error){say(value.error,true);return;}
 if(Object.hasOwn(value,'user')){currentUser=value.user;books=value.books||[];$('auth-screen').hidden=!!currentUser;$('library-screen').hidden=!currentUser;$('logout').hidden=!currentUser;if(currentUser)render();}
 if(value.message){say(value.message);if(!currentUser&&mode==='signup'){mode='login';updateMode();}}
 else say('');
 $('password').value='';$('confirm').value='';
};
// Object.hasOwn is not available in older Android WebView versions.
if(!Object.hasOwn)Object.hasOwn=(o,k)=>Object.prototype.hasOwnProperty.call(o,k);
function updateMode(){const signup=mode==='signup';$('name-label').hidden=!signup;$('name').required=signup;$('confirm-label').hidden=!signup;$('confirm').required=signup;$('password').minLength=signup?8:1;$('password').autocomplete=signup?'new-password':'current-password';$('tab-login').classList.toggle('selected',!signup);$('tab-signup').classList.toggle('selected',signup);$('auth-submit').textContent=signup?'Criar minha conta →':'Entrar na minha biblioteca →';}
$('tab-login').onclick=()=>{mode='login';updateMode();say('');};$('tab-signup').onclick=()=>{mode='signup';updateMode();say('');};
$('auth-form').onsubmit=e=>{e.preventDefault();const email=$('email').value.trim(), password=$('password').value;if(mode==='signup'&&password!==$('confirm').value){say('As senhas precisam ser iguais.',true);return;}request(mode,{email,password,name:$('name').value.trim()});};
$('resend').onclick=()=>{if(!$('email').value||!$('email').checkValidity()){say('Preencha seu e-mail primeiro.',true);return;}request('resend',{email:$('email').value.trim()});};
$('logout').onclick=()=>{if(confirm('Sair da conta? Seus livros continuarão guardados neste aparelho.'))request('logout');};
$('import').onclick=()=>request('import');
const progress=b=>b.completed?100:Math.min(99,Math.floor((b.furthest||0)/b.pages*100));
const normalize=value=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
function render(){
 $('greeting').textContent='SEU ESPAÇO, '+currentUser.name.toUpperCase();$('total').textContent=books.length;$('completed').textContent=books.filter(b=>b.completed).length;$('minutes').textContent=Math.floor(books.reduce((sum,b)=>sum+(b.readMillis||0),0)/60000);
 latest=[...books].filter(b=>b.updated&&!b.completed).sort((a,b)=>b.updated-a.updated)[0];$('continue').hidden=!latest;if(latest){$('continue-title').textContent=latest.title;$('continue-page').textContent='Página '+(latest.page+1)+' de '+latest.pages+' · '+progress(latest)+'%';}
 const query=normalize($('search').value.trim());let visible=books.filter(b=>normalize(b.title).includes(query)&&(filter==='all'||filter==='favorites'&&b.favorite||filter==='reading'&&b.updated&&!b.completed||filter==='finished'&&b.completed));
 const sort=$('sort').value;visible.sort(sort==='title'?(a,b)=>a.title.localeCompare(b.title,'pt-BR'):sort==='progress'?(a,b)=>progress(b)-progress(a):(a,b)=>b.added-a.added);
 $('books').replaceChildren();$('empty').hidden=visible.length>0;$('empty').querySelector('h3').textContent=books.length?'Nenhum livro neste filtro.':'Sua biblioteca começa com você.';
 for(const b of visible){const article=document.createElement('article');article.className='book';const open=document.createElement('button');open.className='open-book';open.setAttribute('aria-label','Ler '+b.title);open.onclick=()=>request('open',{id:b.id});const cover=document.createElement('div');cover.className='cover';const img=document.createElement('img');img.src='https://appassets.androidplatform.net/covers/'+encodeURIComponent(b.id)+'.png';img.alt='Primeira página de '+b.title;cover.append(img);const title=document.createElement('h3');title.textContent=b.title;open.append(cover,title);article.append(open);
 const fav=document.createElement('button');fav.className='favorite'+(b.favorite?' selected':'');fav.textContent=b.favorite?'♥':'♡';fav.setAttribute('aria-label','Favoritar '+b.title);fav.setAttribute('aria-pressed',String(b.favorite));fav.onclick=()=>request('favorite',{id:b.id});article.append(fav);
 const meta=document.createElement('p');meta.className='book-meta';meta.textContent=b.pages+' páginas · '+(b.size/1024/1024).toFixed(1)+' MB';article.append(meta);const line=document.createElement('div');line.className='progress-line';const bar=document.createElement('progress');bar.max=100;bar.value=progress(b);bar.setAttribute('aria-label','Progresso de '+b.title);line.append(bar);article.append(line);const info=document.createElement('div');info.className='progress-info';const state=document.createElement('span');state.textContent=b.completed?'Concluído':b.updated?'Página '+(b.page+1)+' de '+b.pages:'Ainda não começou';const percent=document.createElement('span');percent.textContent=progress(b)+'%';info.append(state,percent);article.append(info);$('books').append(article);}
}
$('continue').onclick=()=>{if(latest)request('open',{id:latest.id});};
for(const button of document.querySelectorAll('[data-filter]'))button.onclick=()=>{filter=button.dataset.filter;document.querySelectorAll('[data-filter]').forEach(b=>b.classList.toggle('selected',b===button));render();};
$('search').oninput=()=>{if(currentUser)render();};$('sort').onchange=()=>{if(currentUser)render();};
window.refreshLibrary=()=>{if(!busy)request('state');};updateMode();request('state');
