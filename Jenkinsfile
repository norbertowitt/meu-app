pipeline {
    agent any

    // 🌎 Variáveis de ambiente
    environment {
        SERVIDOR_DOCKER = "192.168.0.100"
        SERVIDOR_K8S = "192.168.0.110"
        NOME_IMAGEM = "meu-app"
        REGISTRY = "192.168.0.100:5000"
    }

    stages {

        // 📥 Checkout do código
        stage('Checkout') {
            steps {

                echo "📥 Iniciando checkout do repositório..."

                checkout scm

                echo "✅ Checkout realizado com sucesso."
            }
        }

        // 🔍 Debug de branch
        stage('DEBUG - Branch Info') {
            steps {
                script {

                    echo "🔍 Verificando informações da branch..."

                    bat(script: 'git branch -a')

                    def branchGit = bat(
                        script: 'git rev-parse --abbrev-ref HEAD',
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

        // 🔨 Build
        stage('Build') {
            steps {

                echo "🔨 Iniciando build da aplicação..."

                bat(script: 'gradlew.bat clean build')

                echo "✅ Build concluído com sucesso."
            }
        }

        // 🧪 Testes
        stage('Test') {
            steps {

                echo "🧪 Executando testes automatizados..."

                bat(script: 'gradlew.bat test')

                echo "✅ Testes executados com sucesso."
            }
        }

        // 🚀 Versionamento e Deploy
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
                        script: 'git log -1 --pretty=%%B',
                        returnStdout: true
                    ).trim()

                    echo "✅ Mensagem do commit obtida:"
                    echo "${mensagemCommit}"

                    def encontrouRelease = mensagemCommit =~ /release\/(\d+\.\d+\.\d+)/

                    echo "🏷️ Obtendo lista de tags existentes..."

                    def listaTags = bat(
                        script: 'git tag',
                        returnStdout: true
                    ).trim()

                    listaTags = listaTags ? listaTags.split("\n") : []

                    listaTags = listaTags.findAll {
                        it ==~ /\d+\.\d+\.\d+/
                    }

                    listaTags.sort { a, b ->

                        def va = a.tokenize('.').collect {
                            it.toInteger()
                        }

                        def vb = b.tokenize('.').collect {
                            it.toInteger()
                        }

                        va[0] <=> vb[0] ?:
                        va[1] <=> vb[1] ?:
                        va[2] <=> vb[2]
                    }

                    echo "✅ Tags encontradas:"
                    echo "${listaTags}"

                    def ultimaVersao = listaTags ? listaTags.last() : null
                    def novaVersao

                    echo "🧠 Calculando próxima versão..."

                    if (encontrouRelease) {

                        echo "📦 Commit contém release explícita."

                        def versaoRelease = encontrouRelease[0][1]

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

                        bat(
                            script: 'git remote set-url origin https://%GIT_USER%:%GIT_TOKEN%@github.com/norbertowitt/meu-app.git'
                        )

                        echo "✅ Remote configurado."

                        echo "🏷️ Criando tag ${novaVersao}..."

                        bat(script: "git tag ${novaVersao}")

                        echo "✅ Tag criada com sucesso."

                        echo "📤 Enviando tag para o GitHub..."

                        bat(script: "git push origin ${novaVersao}")

                        echo "✅ Tag enviada com sucesso."
                    }

                    echo "📦 Procurando arquivo JAR gerado..."

                    def caminhoJar = powershell(
                        script: 'Get-ChildItem build/libs/*.jar | Select-Object -First 1 -ExpandProperty FullName',
                        returnStdout: true
                    ).trim()

                    echo "✅ JAR encontrado:"
                    echo "${caminhoJar}"

                    echo "📤 Enviando JAR para servidor Docker..."

                    bat(
                        script: "scp ${caminhoJar} user@${SERVIDOR_DOCKER}:/home/user/app.jar"
                    )

                    echo "✅ JAR enviado com sucesso."

                    echo "🐳 Iniciando build e push da imagem Docker..."

                    bat(
                        script:
                            "ssh user@${SERVIDOR_DOCKER} \"docker build -t ${REGISTRY}/${NOME_IMAGEM}:${tagImagem} -t ${REGISTRY}/${NOME_IMAGEM}:latest /home/user && docker push ${REGISTRY}/${NOME_IMAGEM}:${tagImagem} && docker push ${REGISTRY}/${NOME_IMAGEM}:latest\""
                    )

                    echo "✅ Build e push Docker concluídos com sucesso."

                    echo "☸️ Iniciando sincronização no ArgoCD..."

                    bat(
                        script:
                            "ssh user@${SERVIDOR_K8S} \"argocd app sync meu-app\""
                    )

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