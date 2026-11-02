# Quindou Android 1.0.0

Aplicativo Android nativo com interface web incorporada por WebViewAssetLoader e leitor PDF nativo por PdfRenderer. A biblioteca abre offline desde a instalação. Não solicita permissão de internet nem acesso amplo ao armazenamento; os documentos são escolhidos pelo seletor do Android.

No APK, **Adicionar PDF** abre o seletor e encaminha diretamente para o leitor nativo, com troca de páginas e zoom. Documentos não são adicionados permanentemente à biblioteca nesta versão. As oito obras, estatísticas e progressos da biblioteca continuam demonstrativos. Não há suporte a senha, seleção de texto, busca ou anotações no PDF.

O workflow `.github/workflows/android-apk.yml` copia a interface atual para `app/src/main/assets`, compila com Java 17/Gradle 8.9, executa lint e valida assinatura/identidade do APK. O APK de teste tem applicationId `br.com.quindou.app`, versão 1.0.0, minSdk 24, targetSdk 35. A versão é distribuída em uma prévia pública do GitHub Releases.

A assinatura de depuração serve para testes diretos, não para publicação na Play Store. Chaves de depuração dos runners são temporárias; builds futuros com outra assinatura podem exigir desinstalação. Antes de distribuir produção, configurar chave estável privada e versionCode crescente. Não guardar chaves no repositório público.
