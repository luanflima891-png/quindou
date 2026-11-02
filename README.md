# Quindou

Base estática PWA para um leitor de PDF, sem dependências ou backend. Compatível com GitHub Pages em `/quindou/` graças a caminhos relativos.

## O que está pronto

- Interface responsiva em português.
- Manifesto com ícones PNG, instalação em navegadores compatíveis e service worker para o shell offline após a primeira visita online.
- Seleção local de PDF com verificação de assinatura e prévia pelo visualizador nativo do navegador. Não há upload de documentos.

**Esta é uma base de projeto.** O leitor completo ainda precisa ser integrado. A prévia nativa pode não aparecer em alguns celulares; há um link para abrir o PDF separadamente. Arquivos e progresso não são persistidos; escolha novamente o PDF ao reabrir.

## Executar localmente

Requer Python 3:

```sh
python3 -m http.server 8080
```

Abra http://localhost:8080. Service workers requerem HTTPS ou localhost; abrir `index.html` diretamente não habilita o modo offline.

## Publicação gratuita no GitHub Pages

No repositório público `quindou`, configure **Settings → Pages → Deploy from a branch → main → / (root) → Save**. A publicação é feita automaticamente após commits em `main`, sem compilação e sem serviços pagos. URL esperada: https://luanflima891-png.github.io/quindou/ (disponível somente após ativação e publicação bem-sucedida).

## Próximas etapas do leitor

1. Integrar PDF.js com versão fixa, arquivos locais e respectiva licença.
2. Implementar navegação de páginas, zoom, busca e acessibilidade.
3. Guardar PDFs e progresso em IndexedDB com opção de exclusão e tratamento de limites de armazenamento.
4. Testar instalação, leitura e reabertura offline no Android e iOS.

Ao mudar o shell, incremente a versão `CACHE` em `sw.js`. Uma nova versão do service worker aguarda as abas antigas fecharem antes de assumir. Não coloque chaves, dados privados ou PDFs pessoais neste repositório público.
