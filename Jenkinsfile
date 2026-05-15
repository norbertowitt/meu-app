pipeline {
    agent any

    environment {
        SERVIDOR_DOCKER = "192.168.0.100"
        SERVIDOR_K8S = "192.168.0.110"
        USUARIO_SSH = "norbertoneto"
        NOME_IMAGEM = "meu-app"
        REGISTRY = "192.168.0.100:5000"
        GRADLE_USER_HOME = "${WORKSPACE}\\.gradle"
    }

    stages {

        stage('Checkout') {
            steps {

                echo "📥 Iniciando checkout do repositório..."

                checkout scm

                echo "🧹 Limpando cache Gradle antigo..."

                bat(script: 'if exist .gradle rmdir /s /q .gradle')

                echo "✅ Checkout realizado com sucesso."
            }
        }

        stage('DEBUG - Branch Info') {
            steps {
                script {

                    echo "🔍 Verificando informações da branch..."

                    bat(script: 'git branch -a')

                    def branchGit = bat(
                        script: '@git rev-parse --abbrev-ref HEAD',
                        returnStdout: true
                    ).trim()

                    echo "=============================="
                    echo "BRANCH (Git): ${branchGit}"
                    echo "BRANCH_NAME (Jenkins): ${env.BRANCH_NAME}"
                    echo "GIT_BRANCH (Jenkins): ${env.GIT_BRANCH}"
                    echo "=============================="

                    echo "✅ Informações da branch obtidas com sucesso."
                }
            }
        }

        stage('Build') {
            steps {

                echo "🔨 Iniciando build da aplicação..."

                bat(script: 'gradlew.bat clean bootJar --no-daemon')

                echo "✅ Build concluído com sucesso."
            }
        }

        stage('Test') {
            steps {

                echo "🧪 Executando testes automatizados..."

                bat(script: 'gradlew.bat test --no-daemon')

                echo "✅ Testes executados com sucesso."
            }
        }

        stage('Version + Deploy') {

            when {
                expression {
                    return env.GIT_BRANCH?.contains('main')
                }
            }

            steps {
                script {

                    echo "🚀 Entrou na etapa de Version + Deploy"

                    echo "📥 Buscando tags do repositório..."

                    bat(script: 'git fetch --tags')

                    echo "✅ Tags obtidas com sucesso."

                    echo "📄 Obtendo mensagem do último commit..."

                    def mensagemCommit = bat(
                        script: '@git log -1 --pretty=%%B',
                        returnStdout: true
                    ).trim()

                    echo "✅ Mensagem do commit obtida:"
                    echo "${mensagemCommit}"

                    echo "🔍 Verificando se o commit possui release..."

                    def versaoRelease = null

                    if (mensagemCommit.contains('release/')) {

                        def partes = mensagemCommit.split('release/')

                        if (partes.length > 1) {

                            def candidato = partes[1]
                                .split('[\\s\\r\\n]')[0]
                                .trim()

                            if (candidato ==~ /\\d+\\.\\d+\\.\\d+/) {
                                versaoRelease = candidato
                            }
                        }
                    }

                    def possuiRelease = versaoRelease != null

                    if (possuiRelease) {
                        echo "✅ Release encontrada no commit: ${versaoRelease}"
                    } else {
                        echo "ℹ️ Nenhuma release encontrada no commit."
                    }

                    echo "🏷️ Obtendo lista de tags existentes..."

                    def listaTagsRaw = bat(
                        script: '@git tag',
                        returnStdout: true
                    ).trim()

                    def listaTags = listaTagsRaw ? listaTagsRaw.split("\\r?\\n") : []

                    listaTags = listaTags.findAll {
                        it ==~ /\\d+\\.\\d+\\.\\d+/
                    }

                    echo "✅ Tags encontradas:"
                    echo "${listaTags}"

                    def ultimaVersao = listaTags ? listaTags.max() : null

                    def novaVersao

                    echo "🧠 Calculando próxima versão..."

                    if (possuiRelease) {

                        echo "📦 Commit contém release explícita."
                        echo "📦 Versão encontrada no commit: ${versaoRelease}"

                        if (!ultimaVersao) {

                            novaVersao = versaoRelease

                        } else {

                            def r = versaoRelease.tokenize('.').collect {
                                it.toInteger()
                            }

                            def u = ultimaVersao.tokenize('.').collect {
                                it.toInteger()
                            }

                            if (
                                r[0] > u[0] ||
                                (r[0] == u[0] && r[1] > u[1])
                            ) {

                                novaVersao = versaoRelease

                            } else {

                                novaVersao = "${u[0]}.${u[1]}.${u[2] + 1}"
                            }
                        }

                    } else {

                        echo "📦 Nenhuma release explícita encontrada."

                        if (!ultimaVersao) {

                            novaVersao = '1.0.0'

                        } else {

                            def u = ultimaVersao.tokenize('.').collect {
                                it.toInteger()
                            }

                            novaVersao = "${u[0]}.${u[1]}.${u[2] + 1}"
                        }
                    }

                    if (listaTags.contains(novaVersao)) {

                        echo "⚠️ Tag já existe. Incrementando patch..."

                        def u = novaVersao.tokenize('.').collect {
                            it.toInteger()
                        }

                        novaVersao = "${u[0]}.${u[1]}.${u[2] + 1}"
                    }

                    def tagImagem = novaVersao

                    echo "----------------------------------------"
                    echo "✅ Versão final definida: ${novaVersao}"
                    echo "🐳 Imagem Docker: ${REGISTRY}/${NOME_IMAGEM}:${tagImagem}"
                    echo "----------------------------------------"

                    echo "🔐 Iniciando autenticação GitHub..."

                    withCredentials([
                        usernamePassword(
                            credentialsId: 'github-token',
                            usernameVariable: 'GIT_USER',
                            passwordVariable: 'GIT_TOKEN'
                        )
                    ]) {

                        echo "⚙️ Configurando usuário Git..."

                        bat(script: 'git config user.email "jenkins@local"')

                        bat(script: 'git config user.name "Jenkins"')

                        echo "✅ Usuário Git configurado."

                        echo "🔗 Configurando remote do Git..."

                        bat(script: 'git remote set-url origin https://%GIT_USER%:%GIT_TOKEN%@github.com/norbertowitt/meu-app.git')

                        echo "✅ Remote configurado."

                        echo "🏷️ Criando tag ${novaVersao}..."

                        bat(script: "git tag ${novaVersao}")

                        echo "✅ Tag criada com sucesso."

                        echo "📤 Enviando tag para o GitHub..."

                        bat(script: "git push origin ${novaVersao}")

                        echo "✅ Tag enviada com sucesso."
                    }

                    echo "📦 Procurando arquivo JAR Spring Boot..."

                    def caminhoJar = powershell(
                        script: '''
                        Get-ChildItem build/libs/*.jar |
                        Where-Object {
                            $_.Name -notlike "*-plain.jar"
                        } |
                        Select-Object -First 1 -ExpandProperty FullName
                        ''',
                        returnStdout: true
                    ).trim()

                    echo "✅ JAR encontrado:"
                    echo "${caminhoJar}"

                    echo "📏 Obtendo tamanho do arquivo JAR..."

                    def tamanhoJar = powershell(
                        script: """
                        ((Get-Item '${caminhoJar}').Length / 1MB).ToString('0.00')
                        """,
                        returnStdout: true
                    ).trim()

                    echo "✅ Tamanho final do JAR: ${tamanhoJar} MB"

                    echo "📂 Listando arquivos da pasta build/libs..."

                    bat(script: 'dir build\\libs')

                    echo "🧪 Testando SSH..."

                    bat(script: "ssh -v -o StrictHostKeyChecking=no ${USUARIO_SSH}@${SERVIDOR_DOCKER} exit")

                    echo "📤 Enviando JAR para servidor Docker..."

                    bat(script: "scp -v -o BatchMode=yes -o StrictHostKeyChecking=no \"${caminhoJar}\" ${USUARIO_SSH}@${SERVIDOR_DOCKER}:/home/${USUARIO_SSH}/app.jar")

                    echo "✅ JAR enviado com sucesso."

                    echo "📤 Enviando Dockerfile para servidor Docker..."

                    bat(script: "scp -v -o BatchMode=yes -o StrictHostKeyChecking=no Dockerfile ${USUARIO_SSH}@${SERVIDOR_DOCKER}:/home/${USUARIO_SSH}/Dockerfile")

                    echo "✅ Dockerfile enviado com sucesso."

                    echo "🐳 Iniciando build e push da imagem Docker..."

                    bat(script: """
                    ssh -o StrictHostKeyChecking=no ${USUARIO_SSH}@${SERVIDOR_DOCKER} ^
                    "cd /home/${USUARIO_SSH} && ^
                    docker build -t ${REGISTRY}/${NOME_IMAGEM}:${tagImagem} -t ${REGISTRY}/${NOME_IMAGEM}:latest . && ^
                    docker push ${REGISTRY}/${NOME_IMAGEM}:${tagImagem} && ^
                    docker push ${REGISTRY}/${NOME_IMAGEM}:latest"
                    """)

                    echo "✅ Build e push Docker concluídos com sucesso."

                    echo "☸️ Iniciando sincronização no ArgoCD..."

                    bat(script: "ssh -o StrictHostKeyChecking=no ${USUARIO_SSH}@${SERVIDOR_K8S} \"argocd app sync meu-app\"")

                    echo "✅ Deploy sincronizado com sucesso no Kubernetes."

                    echo "🎉 Pipeline finalizado com sucesso."
                }
            }
        }
    }

    post {

        always {

            echo "📑 Publicando resultados dos testes..."

            junit '**/build/test-results/test/*.xml'

            echo "✅ Publicação dos testes concluída."
        }

        success {
            echo "🎉 PIPELINE EXECUTADO COM SUCESSO."
        }

        failure {
            echo "❌ PIPELINE FINALIZADO COM ERRO."
        }
    }
}