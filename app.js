const input = document.querySelector('#pdf-file');
const reader = document.querySelector('#reader');
const preview = document.querySelector('#pdf-preview');
const status = document.querySelector('#status');
const install = document.querySelector('#install');
let objectURL;
let installPrompt;

function closePDF() {
  preview.removeAttribute('src');
  document.querySelector('#open-pdf').removeAttribute('href');
  if (objectURL) URL.revokeObjectURL(objectURL);
  objectURL = undefined;
  reader.hidden = true;
  input.value = '';
}

input.addEventListener('change', async () => {
  const file = input.files[0];
  if (!file) return;
  const signature = new Uint8Array(await file.slice(0, 5).arrayBuffer());
  if (String.fromCharCode(...signature) !== '%PDF-') {
    status.textContent = 'Escolha um arquivo PDF válido.';
    input.value = '';
    return;
  }
  closePDF();
  objectURL = URL.createObjectURL(file);
  document.querySelector('#filename').textContent = file.name;
  document.querySelector('#open-pdf').href = objectURL;
  preview.src = objectURL;
  reader.hidden = false;
  status.textContent = 'PDF selecionado. A prévia usa o visualizador do seu navegador.';
  reader.scrollIntoView({ behavior: 'smooth', block: 'start' });
});
document.querySelector('#close').addEventListener('click', () => {
  closePDF();
  status.textContent = 'Escolha outro PDF quando quiser.';
  input.focus();
});
window.addEventListener('beforeunload', closePDF);
window.addEventListener('beforeinstallprompt', event => {
  event.preventDefault();
  installPrompt = event;
  install.hidden = false;
});
install.addEventListener('click', async () => {
  if (!installPrompt) return;
  await installPrompt.prompt();
  await installPrompt.userChoice;
  installPrompt = undefined;
  install.hidden = true;
});
window.addEventListener('appinstalled', () => { install.hidden = true; });
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('./sw.js').catch(() => {
    status.textContent = 'A página abriu, mas o modo offline não está disponível nesta sessão.';
  });
}
