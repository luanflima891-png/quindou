# Quindou Android 1.1.0

- Login/cadastro reais por e-mail e senha no projeto Supabase Quindou. Conta online; sessão AES-GCM protegida pelo Android Keystore. Sem senha salva no aparelho.
- Biblioteca vazia, sem obras ou estatísticas demonstrativas. A interface Android é própria em `android/ui/`; a demonstração web pública permanece separada.
- O importador COPIA o PDF para `files/library/<auth-user-id>/`, gera a capa a partir da primeira página, evita duplicação por SHA-256 e valida abertura pelo PdfRenderer. Limite 300 MB. Não depende de URI ou do arquivo original após importação.
- Metadados gravados atomicamente: última página, maior avanço, conclusão explícita, favoritos e tempo ativo. Checkpoints a cada 15 s, troca de página e pausa. Só uma página por vez em memória; seleção de texto e PDF com senha ainda não suportados.
- PDF e progresso ficam no celular por conta; não há upload ou sincronização entre aparelhos. Sair mantém os arquivos; desinstalar/limpar dados remove-os. Conta online não é backup dos PDFs.
- Auth usa somente chave publicável. Não há tabelas públicas para documentos. Logout impede acesso à biblioteca até novo login. Uma sessão previamente autenticada permite leitura offline; ao expirar, tenta renovar quando houver rede.
- Confirmação por e-mail permanece ligada no Supabase. O SMTP padrão do Supabase restringe destinatários e possui limites: configurar SMTP próprio e URL de confirmação antes de abrir cadastros ao público geral. Nunca contornar confirmação por criação administrativa de usuários.
- 4 testes Robolectric verificam cópia independente, restauração de progresso, isolamento entre contas, deduplicação e rejeição de PDF inválido. Workflow executa testes, assembleDebug, lint, apksigner e aapt antes de publicar.
- Assinatura de depuração é temporária. A instalação de 1.1.0 sobre 1.0.0 exige remover a anterior. Antes de distribuição definitiva, configurar chave estável privada. Não reinstalar futuras builds com assinaturas diferentes sem exportar os documentos.
