const paths = {
 library:'M3 4h4v16H3z M10 4h4v16h-4z M17 4l4 1-2 15-4-1z',
 book:'M12 5C8 2 3 4 3 4v15s5-2 9 1c4-3 9-1 9-1V4s-5-2-9 1v15',
 heart:'M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8z',
 check:'M5 12l4 4L19 6',spark:'M12 2l3 7 7 3-7 3-3 7-3-7-7-3 7-3z',
 download:'M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5',plus:'M12 4v16M4 12h16',
 arrow:'M4 12h16m-6-6 6 6-6 6',chart:'M4 20V10m8 10V4m8 16v-7',
 grid:'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
 list:'M8 5h13M8 12h13M8 19h13M3 5h.01M3 12h.01M3 19h.01',
 search:'M21 21l-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0',close:'M5 5l14 14M19 5 5 19'
};
const icon = name => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="${paths[name] || paths.book}"/></svg>`;
document.querySelectorAll('[data-icon]').forEach(el => { el.innerHTML = icon(el.dataset.icon); });
const escapeHTML = value => String(value).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const books = [
 {id:'restart',title:'A arte de recomeçar',author:'Clara Ventura',category:'Desenvolvimento',pages:224,progress:42,bg:'#c26040',fg:'#fff0d6',accent:'#ecb15d',style:0},
 {id:'silence',title:'O lado de dentro do silêncio',author:'Tomás Amaral',category:'Ficção',pages:286,progress:18,bg:'#34494c',fg:'#eee5c8',accent:'#93a78c',style:1},
 {id:'ideas',title:'Ideias fora da caixa',author:'Nina Azevedo',category:'Criatividade',pages:192,progress:0,bg:'#d4bf66',fg:'#343e36',accent:'#b95e41',style:2},
 {id:'time',title:'O tempo é seu',author:'André Fontes',category:'Desenvolvimento',pages:208,progress:67,bg:'#e5dcca',fg:'#4d5b4b',accent:'#a8b39b',style:3},
 {id:'horizon',title:'Depois do horizonte',author:'Helena Reis',category:'Ficção',pages:312,progress:0,bg:'#4c6684',fg:'#f2ead5',accent:'#d59b77',style:4},
 {id:'essential',title:'Menos, mas melhor',author:'Pedro Salles',category:'Negócios',pages:176,progress:100,bg:'#e5a279',fg:'#3c4339',accent:'#c76d4b',style:5},
 {id:'create',title:'O impulso de criar',author:'Lia Monteiro',category:'Criatividade',pages:248,progress:100,bg:'#888c69',fg:'#faf0d6',accent:'#c6cb9e',style:3},
 {id:'tomorrow',title:'Um negócio para amanhã',author:'Rafael Moura',category:'Negócios',pages:264,progress:0,bg:'#4a454e',fg:'#f2d7ae',accent:'#b48868',style:2}
];
let favorites = new Set(['restart','silence']);
try { const saved=JSON.parse(localStorage.getItem('quindou-demo-favorites')); if(Array.isArray(saved)) favorites=new Set(saved); } catch {}
let view='all', filter='all';
const bookGrid=document.querySelector('#books');
const status=document.querySelector('#status');
const normalize = s => s.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
const cover = book => `<div class="cover-stage"><div class="book-cover cover-style-${book.style}" style="--cover-bg:${book.bg};--cover-fg:${book.fg};--accent:${book.accent}"><span class="edition">QUINDOU / ${book.local?'MEUS ARQUIVOS':'EDIÇÃO DEMO'}</span><div class="cover-title">${escapeHTML(book.title)}</div><div class="cover-shape"></div><span class="cover-author">${escapeHTML(book.author.toUpperCase())}</span></div></div>`;
function render() {
 const search=normalize(document.querySelector('#search').value.trim());
 let visible=books.filter(b => (view==='all'||view==='favorites'&&favorites.has(b.id)||view==='reading'&&b.progress>0&&b.progress<100||view==='finished'&&b.progress===100) && (filter==='all'||b.category===filter) && normalize(b.title+' '+b.author).includes(search));
 const sort=document.querySelector('#sort').value;
 if(sort==='title') visible.sort((a,b)=>a.title.localeCompare(b.title,'pt-BR'));
 if(sort==='progress') visible.sort((a,b)=>b.progress-a.progress);
 bookGrid.innerHTML=visible.map(b=>`<article class="book-card"><button class="cover-button" data-open="${b.id}" aria-label="${b.local?'Abrir PDF':'Ver livro demonstrativo'}: ${escapeHTML(b.title)}">${cover(b)}</button><button class="favorite" data-favorite="${b.id}" aria-label="Favoritar ${escapeHTML(b.title)}" aria-pressed="${favorites.has(b.id)}">${icon('heart')}</button><div class="book-info"><div class="category-line"><span>${escapeHTML(b.category)}</span><span class="pdf-tag">${b.local?'PDF LOCAL':'DEMO'}</span></div><h3 class="book-title">${escapeHTML(b.title)}</h3><p class="book-author">${escapeHTML(b.author)}</p><div class="progress-label"><span>${b.progress===100?'Leitura concluída':b.progress?'Em leitura':b.local?'Pronto para abrir':'Ainda não começou'}</span><strong>${b.progress?b.progress+'%':'—'}</strong></div><div class="progress-track"><div style="width:${b.progress}%"></div></div><div class="book-meta">${b.local?'Disponível nesta sessão':b.pages+' páginas · coleção demonstrativa'}</div></div></article>`).join('');
 document.querySelector('#book-count').textContent=visible.length+' '+(visible.length===1?'livro':'livros');
 document.querySelector('#nav-count').textContent=books.length;
 document.querySelector('#fav-count').textContent=books.filter(b=>favorites.has(b.id)).length;
 document.querySelector('#empty').hidden=visible.length>0;
}
bookGrid.addEventListener('click', event => {
 const favorite=event.target.closest('[data-favorite]');
 if(favorite){const id=favorite.dataset.favorite; favorites.has(id)?favorites.delete(id):favorites.add(id); try{localStorage.setItem('quindou-demo-favorites',JSON.stringify([...favorites]));}catch{} render();return;}
 const open=event.target.closest('[data-open]'); if(open) openBook(open.dataset.open);
});
function openBook(id){
 const book=books.find(b=>b.id===id); if(!book)return;
 if(book.local){openPDF(book);return;}
 document.querySelector('#dialog-content').innerHTML=`<div class="detail-layout"><div class="detail-cover">${cover(book)}</div><div><span class="detail-badge">LIVRO DEMONSTRATIVO</span><h2>${escapeHTML(book.title)}</h2><p>${escapeHTML(book.author)}<br>${book.category} · ${book.pages} páginas</p><p>Este título e seu progresso são ilustrativos, para você experimentar a biblioteca do Quindou.</p></div></div><div class="excerpt"><h3>Um gostinho da experiência de leitura</h3><p>Há dias em que a melhor direção é para dentro. Entre uma tarefa e outra, existe um pequeno espaço que pode ser só seu. Abra uma página, respire fundo e deixe uma ideia nova entrar.</p><small>Trecho original criado para esta demonstração. Para ler um documento real, use “Adicionar PDF”.</small></div>`;
 document.querySelector('#book-dialog').showModal();
}
document.querySelector('#continue').addEventListener('click',()=>openBook('restart'));
document.querySelectorAll('[data-view]').forEach(button=>button.addEventListener('click',()=>{
 view=button.dataset.view; document.querySelectorAll('[data-view]').forEach(b=>b.classList.toggle('active',b===button));
 const titles={all:'Minha biblioteca',reading:'Em leitura',favorites:'Favoritos',finished:'Concluídos'};
 document.querySelector('#library-title').firstChild.textContent=titles[view]+' ';
 render();
}));
document.querySelectorAll('[data-filter]').forEach(button=>button.addEventListener('click',()=>{filter=button.dataset.filter;document.querySelectorAll('[data-filter]').forEach(b=>b.classList.toggle('active',b===button));render();}));
document.querySelector('#search').addEventListener('input',render);
document.querySelector('#sort').addEventListener('change',render);
for(const mode of ['grid','list']) document.querySelector('#'+mode+'-view').addEventListener('click',()=>{
 bookGrid.classList.toggle('list-layout',mode==='list');
 for(const m of ['grid','list']){const el=document.querySelector('#'+m+'-view');el.classList.toggle('selected',m===mode);el.setAttribute('aria-pressed',String(m===mode));}
});
const input=document.querySelector('#pdf-file');
const reader=document.querySelector('#reader');
const preview=document.querySelector('#pdf-preview');
function openPDF(book){document.querySelector('#filename').textContent=book.title;document.querySelector('#open-pdf').href=book.url;preview.src=book.url;reader.showModal();}
input.addEventListener('change',async()=>{
 const file=input.files[0];if(!file)return;
 try{
 const signature=new Uint8Array(await file.slice(0,5).arrayBuffer());
 if(String.fromCharCode(...signature)!=='%PDF-'){status.textContent='Escolha um arquivo PDF válido.';return;}
 const book={id:'local-'+Date.now(),title:file.name.replace(/\.pdf$/i,''),author:'Meu documento',category:'Meus PDFs',progress:0,bg:'#52654d',fg:'#faf0d6',accent:'#91a183',style:1,local:true,url:URL.createObjectURL(file)};
 books.unshift(book);view='all';filter='all';document.querySelector('#search').value='';document.querySelectorAll('[data-view]').forEach(b=>b.classList.toggle('active',b.dataset.view==='all'));document.querySelectorAll('[data-filter]').forEach(b=>b.classList.toggle('active',b.dataset.filter==='all'));document.querySelector('#library-title').firstChild.textContent='Minha biblioteca ';render();status.textContent='PDF adicionado nesta sessão. O arquivo não é enviado; selecione-o novamente se recarregar a página.';openPDF(book);
 }catch{status.textContent='Não foi possível abrir esse arquivo. Tente selecionar o PDF novamente.';}finally{input.value='';}
});
document.querySelector('#close').addEventListener('click',()=>reader.close());
reader.addEventListener('close',()=>preview.removeAttribute('src'));
for(const dialog of document.querySelectorAll('dialog'))dialog.addEventListener('click',e=>{if(e.target===dialog){const r=dialog.getBoundingClientRect();if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom)dialog.close();}});
window.addEventListener('beforeunload',()=>books.filter(b=>b.local).forEach(b=>URL.revokeObjectURL(b.url)));
const install=document.querySelector('#install');let installPrompt;
window.addEventListener('beforeinstallprompt',event=>{event.preventDefault();installPrompt=event;install.hidden=false;});
install.addEventListener('click',async()=>{if(!installPrompt)return;await installPrompt.prompt();await installPrompt.userChoice;installPrompt=undefined;install.hidden=true;});
window.addEventListener('appinstalled',()=>{install.hidden=true;});
if('serviceWorker' in navigator)navigator.serviceWorker.register('./sw.js').catch(()=>{status.textContent='O modo offline não está disponível nesta sessão.';});
render();
